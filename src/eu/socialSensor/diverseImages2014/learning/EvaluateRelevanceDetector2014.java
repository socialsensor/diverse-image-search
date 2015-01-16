package eu.socialSensor.diverseImages2014.learning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.LogitBoost;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.REPTree;
import weka.classifiers.trees.RandomForest;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddValues;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;
import weka.filters.unsupervised.instance.Resample;

import com.aliasi.util.BoundedPriorityQueue;

import eu.socialSensor.diverseImages2014.MEDI2014Collection;
import eu.socialSensor.diverseImages2014.utils.MediImageComparator2014;

/**
 * 
 * @author Eleftherios Spyromitros-Xioufis
 */
public class EvaluateRelevanceDetector2014 {

	public static final int imageIdAttributeIndex = 0;

	// attribute indices start from 1 when using Strings as arguments in Instances
	public static final String imageIdAttributeIndexString = "1";
	public static final String locationIdAttributeIndexString = "2";
	public static final String flickrRankAttributeIndexString = "3";

	// meaning of each value of the target variable
	public static final int relevanceIndex = 1;
	public static final int irrelevanceIndex = 0;

	public static final int maxNumImagesInSubmissionFile = 300;

	public static final boolean debug = false;

	/**
	 * Performs parameter selection via cross-validation on the devset, retrains the model on the full devset
	 * using the selected parameters and evaluates it on the test set.
	 * 
	 * @param args
	 *            [0] full path to the dataset and the dataset's filestem
	 * @param args
	 *            [1] how many of the remaining validation locations to use for the prediction of each
	 *            location during cross-validation, e.g. 5
	 * @param args
	 *            [2] how much weight to give to the Wikipedia images of each location, e.g. 100
	 * @param args
	 *            [3] whether to use ONLY Wikipedia images as positive (relevant) examples for each location
	 * @param args
	 *            [4] the classifier to use, e.g. liblinear, rf
	 * @param args
	 *            [5] full path to the submission file that will be created
	 * @param args
	 *            [6] whether to create new features using predictions
	 * @param args
	 *            [7] full path to the collection (if args[6] is true), e.g. C:/Medi2014/
	 * @param args
	 *            [8] name of the features to be created (if args[6] is true)
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String datasetFileStem = args[0];
		int numTrainLocations = Integer.parseInt(args[1]);
		int wikiWeight = Integer.parseInt(args[2]);
		boolean onlyWikiAsPositive = Boolean.parseBoolean(args[3]);
		String classifierSelection = args[4].split(" ")[0];
		double parameterStart = Double.parseDouble(args[4].split(" ")[1]);
		double parameterStep = Double.parseDouble(args[4].split(" ")[2]);
		int parameterNumSteps = Integer.parseInt(args[4].split(" ")[3]);
		String submissionFileStem = args[5];
		boolean createNewFeatures = Boolean.parseBoolean(args[6]);
		String collectionPath = "";
		String featureName = "";
		MEDI2014Collection dataStoreTrain = null;
		MEDI2014Collection dataStoreTest = null;

		if (createNewFeatures) { // load the development collection
			collectionPath = args[7];
			featureName = args[8];
			// also load the train and test collections in order to be able to retrieve the location name from
			// the location id
			dataStoreTrain = new MEDI2014Collection(collectionPath + "devset/");
			dataStoreTrain.loadAll(false, new String[0], new String[0]);
			dataStoreTest = new MEDI2014Collection(collectionPath + "testset/");
			dataStoreTest.loadAll(false, new String[0], new String[0]);
		}

		System.out.println("Loading the validation dataset..");
		DataSource source = new DataSource(datasetFileStem + "-dev.arff");
		Instances validationDataset = source.getDataSet();
		validationDataset.setClassIndex(validationDataset.numAttributes() - 1);
		System.out.println("Loading completed!\n\n");

		int numValidationLocations = validationDataset.attribute(
				Integer.valueOf(locationIdAttributeIndexString) - 1).numValues();

		// initializing measures
		double[][] AuROC = new double[parameterNumSteps][];
		double[][] AuPRC = new double[parameterNumSteps][];
		double[][] Pat10 = new double[parameterNumSteps][];
		double[][] Pat20 = new double[parameterNumSteps][];
		double[][] Pat50 = new double[parameterNumSteps][];
		double[][] Pat100 = new double[parameterNumSteps][];
		double[][] FNRP = new double[parameterNumSteps][];

		double bestAuROC = 0;
		int bestParameterStep = 0;
		double bestParameter = 0;

		int noImprovementCounter = 0;
		for (int stepIndex = 0; stepIndex < parameterNumSteps; stepIndex++) {
			// initializing measures for this set parameter value
			AuROC[stepIndex] = new double[numValidationLocations];
			AuPRC[stepIndex] = new double[numValidationLocations];
			Pat10[stepIndex] = new double[numValidationLocations];
			Pat20[stepIndex] = new double[numValidationLocations];
			Pat50[stepIndex] = new double[numValidationLocations];
			Pat100[stepIndex] = new double[numValidationLocations];
			FNRP[stepIndex] = new double[numValidationLocations];

			double parameter = Math.pow(10, parameterStart + stepIndex * parameterStep);

			System.out.println("Cross-validation using: " + classifierSelection + " with parameter: "
					+ parameter);
			String submissionFileName = submissionFileStem + "_" + classifierSelection + "_" + parameter
					+ "-dev.txt";
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(submissionFileName)));
			for (int locationIndex = 0; locationIndex < numValidationLocations; locationIndex++) {
				long start = System.currentTimeMillis();
				String locationId = String.valueOf(validationDataset.attribute(
						Integer.valueOf(locationIdAttributeIndexString) - 1).value(locationIndex));
				System.out.print("Evaluating classifier on location: " + locationId);
				// System.out.println("Doing the split..");
				Instances[] sets = EvaluateRelevanceDetector2014.createTrainTestSplitForLocationValidation(
						locationId, validationDataset, numTrainLocations, wikiWeight, onlyWikiAsPositive);

				Instances xFoldTrainDataOriginal = sets[0];
				Instances xFoldTestDataOriginal = sets[1];

				// remove the extra attributes!
				Remove rm = new Remove();
				// image id and location id attributes should be ignored!
				rm.setAttributeIndices(imageIdAttributeIndexString + "," + locationIdAttributeIndexString
						+ "," + flickrRankAttributeIndexString);
				rm.setInputFormat(xFoldTrainDataOriginal);
				Instances xFoldTrainData = Filter.useFilter(xFoldTrainDataOriginal, rm);
				// the original training data can be deleted
				xFoldTrainDataOriginal.delete();
				Instances xFoldTestData = Filter.useFilter(xFoldTestDataOriginal, rm);
				// the original test data are still needed

				// build the model on xFoldTrainData
				long modelStart = System.currentTimeMillis();
				Classifier classifier = selectClassifier(classifierSelection, parameter);
				classifier.buildClassifier(xFoldTrainData);
				long modelEnd = System.currentTimeMillis();

				// evaluate the model on xFoldTestData
				Evaluation eval = new Evaluation(xFoldTrainData);
				eval.evaluateModel(classifier, xFoldTestData);
				AuROC[stepIndex][locationIndex] += eval.areaUnderROC(relevanceIndex);
				AuPRC[stepIndex][locationIndex] += eval.areaUnderPRC(relevanceIndex);

				// This bounded priority queue will hold maxNumImagesInSubmissionFile images in decreasing
				// relevance score order
				BoundedPriorityQueue<MediImageComparator2014> bpq = new BoundedPriorityQueue<MediImageComparator2014>(
						new MediImageComparator2014(), xFoldTestData.numInstances());

				BufferedWriter outFeatures = null;
				if (createNewFeatures) {
					String locationName = dataStoreTrain.getLocationList().get(locationIndex)
							.getLocationName();
					int queryId = dataStoreTrain.getLocationList().get(locationIndex).getQueryId();
					if (queryId != Integer.parseInt(locationId)) {
						throw new Exception("Incorrect location!");
					}
					String newFeaturesFileLocation = collectionPath + "devset/" + "descvis/img/"
							+ locationName + " " + featureName + ".csv";
					outFeatures = new BufferedWriter(new FileWriter(new File(newFeaturesFileLocation)));
				}

				for (int j = 0; j < xFoldTestData.numInstances(); j++) {
					String imageId = String.valueOf(new Double(xFoldTestDataOriginal.instance(j).value(
							Integer.valueOf(imageIdAttributeIndexString) - 1)).longValue());
					double relevanceScore = classifier.distributionForInstance(xFoldTestData.instance(j))[relevanceIndex];
					int flickrRank = (int) xFoldTestDataOriginal.instance(j).value(
							Integer.valueOf(flickrRankAttributeIndexString) - 1);
					MediImageComparator2014 srs = new MediImageComparator2014(imageId, locationIndex,
							flickrRank, j, relevanceScore);
					if (!bpq.offer(srs)) { // System.out.println("Offering: " + srs.toString() + " " + j);
						throw new Exception("Queue insertion failed!");
					}
					if (createNewFeatures) {
						outFeatures.write(imageId + "," + relevanceScore + "\n");
					}
				}

				if (createNewFeatures) {
					outFeatures.close();
				}

				// Generate a submission file where images are ordered based on relevance score.
				// At the same time calculate the P@X and FNRP measures
				int limit = Math.min(bpq.size(), maxNumImagesInSubmissionFile);
				boolean firstNotRelevantFound = false; // -1 means firstNotRelevant not found

				for (int j = 0; j < limit; j++) {
					MediImageComparator2014 srs = bpq.poll();
					int classValueIndex = (int) xFoldTestData.instance(srs.getImageIndexInTestSet())
							.classValue();
					if (classValueIndex == relevanceIndex) {
						if (j < 100) {
							Pat100[stepIndex][locationIndex] += 1;
						}
						if (j < 50) {
							Pat50[stepIndex][locationIndex] += 1;
						}
						if (j < 20) {
							Pat20[stepIndex][locationIndex] += 1;
						}
						if (j < 10) {
							Pat10[stepIndex][locationIndex] += 1;
						}
					}
					if (classValueIndex == irrelevanceIndex && !firstNotRelevantFound) {
						firstNotRelevantFound = true;
						FNRP[stepIndex][locationIndex] = j + 1;
					}
					out.write(locationId + " 0 " + srs.getImageId() + " " + j + " " + srs.getRelevanceScore()
							+ " rel_run\n");
				}
				out.flush();

				Pat10[stepIndex][locationIndex] /= 10.0;
				Pat20[stepIndex][locationIndex] /= 20.0;
				Pat50[stepIndex][locationIndex] /= 50.0;
				Pat100[stepIndex][locationIndex] /= 100.0;

				// System.out.print("Measures: ");
				// System.out.print(AuROC[stepIndex][locationIndex] + ",");
				// System.out.print(AuPRC[stepIndex][locationIndex] + ",");
				// System.out.print(Pat10[stepIndex][locationIndex] + ",");
				// System.out.print(Pat20[stepIndex][locationIndex] + ",");
				// System.out.print(Pat50[stepIndex][locationIndex] + ",");
				// System.out.print(Pat100[stepIndex][locationIndex] + ",");
				// System.out.println(FNRP[stepIndex][locationIndex]);

				long end = System.currentTimeMillis();

				// System.out.println("Model building time in ms: " + (modelEnd - modelStart));
				// System.out.println("Total time in ms: " + (end - start) + "\n");

			}
			out.close();

			System.out.print("Average Measures: ");
			System.out.print(Utils.mean(AuROC[stepIndex]) + " ");
			System.out.print(Utils.mean(AuPRC[stepIndex]) + " ");
			System.out.print(Utils.mean(Pat10[stepIndex]) + " ");
			System.out.print(Utils.mean(Pat20[stepIndex]) + " ");
			System.out.print(Utils.mean(Pat50[stepIndex]) + " ");
			System.out.print(Utils.mean(Pat100[stepIndex]) + " ");
			System.out.println(Utils.mean(FNRP[stepIndex]));

			// selection is currently based on AuROC
			if (Utils.mean(AuROC[stepIndex]) > bestAuROC) {
				bestAuROC = Utils.mean(AuROC[stepIndex]);
				bestParameterStep = stepIndex;
				bestParameter = parameter;

				if (noImprovementCounter > 0) {// reset no improvement counter
					noImprovementCounter = 0;
				}
			} else {
				noImprovementCounter++; // increase no improvement counter
				System.out.println("No improvement counter = " + noImprovementCounter);
				if (noImprovementCounter > 2) { // break if there are 3 consecutive no improvements
					break;
				}
			}
		}

		System.out.println("Best Measures: ");
		System.out.print(Utils.mean(AuROC[bestParameterStep]) + " ");
		System.out.print(Utils.mean(AuPRC[bestParameterStep]) + " ");
		System.out.print(Utils.mean(Pat10[bestParameterStep]) + " ");
		System.out.print(Utils.mean(Pat20[bestParameterStep]) + " ");
		System.out.print(Utils.mean(Pat50[bestParameterStep]) + " ");
		System.out.print(Utils.mean(Pat100[bestParameterStep]) + " ");
		System.out.println(Utils.mean(FNRP[bestParameterStep]));
		System.out.println("With parameter: " + bestParameter);

		// now rebuild on full training set and evaluate on test set!
		// also add code for creating meta features on test set!
		// see the MakePredictionsWithRelevanceDetector class..

		System.out.println("Loading the test dataset..");
		source = new DataSource(datasetFileStem + "-test.arff");
		Instances testDataset = source.getDataSet();
		testDataset.setClassIndex(testDataset.numAttributes() - 1);
		System.out.println("Loading completed!\n");

		int numTestLocations = testDataset.attribute(Integer.valueOf(locationIdAttributeIndexString) - 1)
				.numValues();

		System.out.println("Making predictions on test set using: " + classifierSelection
				+ " with parameter: " + bestParameter);
		String submissionFileNameTest = submissionFileStem + "_" + classifierSelection + "_" + bestParameter
				+ "-test.txt";
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(submissionFileNameTest)));

		// initializing measures for this set parameter value
		double[] AuROCtest = new double[numTestLocations];
		double[] AuPRCtest = new double[numTestLocations];
		double[] Pat10test = new double[numTestLocations];
		double[] Pat20test = new double[numTestLocations];
		double[] Pat50test = new double[numTestLocations];
		double[] Pat100test = new double[numTestLocations];
		double[] FNRPtest = new double[numTestLocations];

		for (int locationIndex = 0; locationIndex < numTestLocations; locationIndex++) {
			long start = System.currentTimeMillis();
			String locationId = String.valueOf(testDataset.attribute(
					Integer.valueOf(locationIdAttributeIndexString) - 1).value(locationIndex));
			System.out.println("Making predictions for location id " + locationId);
			Instances[] sets = EvaluateRelevanceDetector2014.createTrainTestSplitForLocationTest(locationId,
					validationDataset, testDataset, wikiWeight, onlyWikiAsPositive);

			Instances xFoldTrainData = sets[0];
			Instances xFoldTestData = sets[1];

			// build a model on trainingData
			// System.out.println("Building the model..");
			long modelStart = System.currentTimeMillis();
			Classifier model = selectClassifier(classifierSelection, bestParameter);
			Remove rm = new Remove();
			// image id, location id and flickr rank attributes should be ignored!
			rm.setAttributeIndices(imageIdAttributeIndexString + "," + locationIdAttributeIndexString + ","
					+ flickrRankAttributeIndexString);
			FilteredClassifier fc = new FilteredClassifier();
			fc.setFilter(rm);
			fc.setClassifier(model);
			fc.buildClassifier(xFoldTrainData);
			long modelEnd = System.currentTimeMillis();
			// System.out.println("Building completed!");
			// System.out.println("Model building time in ms: " + (modelEnd - modelStart));

			// System.out.println("Evaluating the model..");
			Evaluation eval = new Evaluation(xFoldTrainData);
			eval.evaluateModel(fc, xFoldTestData);
			AuROCtest[locationIndex] = eval.areaUnderROC(relevanceIndex);
			AuPRCtest[locationIndex] = eval.areaUnderPRC(relevanceIndex);

			// This bounded priority queue will hold maxNumImagesInSubmissionFile images in decreasing
			// relevance score order
			BoundedPriorityQueue<MediImageComparator2014> bpq = new BoundedPriorityQueue<MediImageComparator2014>(
					new MediImageComparator2014(), xFoldTestData.numInstances());

			BufferedWriter outFeatures = null;
			if (createNewFeatures) {
				String locationName = dataStoreTest.getLocationList().get(locationIndex).getLocationName();
				int queryId = dataStoreTest.getLocationList().get(locationIndex).getQueryId();
				if (queryId != Integer.parseInt(locationId)) {
					throw new Exception("Incorrect location!");
				}
				String newFeaturesFileLocation = collectionPath + "testset/" + "descvis/img/" + locationName
						+ " " + featureName + ".csv";
				outFeatures = new BufferedWriter(new FileWriter(new File(newFeaturesFileLocation)));
			}

			for (int j = 0; j < xFoldTestData.numInstances(); j++) {
				String imageId = String.valueOf(new Double(xFoldTestData.instance(j).value(
						Integer.valueOf(imageIdAttributeIndexString) - 1)).longValue());
				double relevanceScore = fc.distributionForInstance(xFoldTestData.instance(j))[relevanceIndex];
				int flickrRank = (int) xFoldTestData.instance(j).value(
						Integer.valueOf(flickrRankAttributeIndexString) - 1);
				MediImageComparator2014 srs = new MediImageComparator2014(imageId, locationIndex, flickrRank,
						j, relevanceScore);
				if (!bpq.offer(srs)) { // System.out.println("Offering: " + srs.toString() + " " + j);
					throw new Exception("Queue insertion failed!");
				}
				if (createNewFeatures) {
					outFeatures.write(imageId + "," + relevanceScore + "\n");
				}
			}

			if (createNewFeatures) {
				outFeatures.close();
			}

			// Generate a submission file where images are order based on relevance score.
			// At the same time calculate the P@X and FNRP measures
			int limit = Math.min(bpq.size(), maxNumImagesInSubmissionFile);
			boolean firstNotRelevantFound = false; // -1 means firstNotRelevant not found
			for (int j = 0; j < limit; j++) {
				MediImageComparator2014 srs = bpq.poll();
				int classValueIndex = (int) xFoldTestData.instance(srs.getImageIndexInTestSet()).classValue();
				if (classValueIndex == relevanceIndex) {
					if (j < 100) {
						Pat100test[locationIndex] += 1;
					}
					if (j < 50) {
						Pat50test[locationIndex] += 1;
					}
					if (j < 20) {
						Pat20test[locationIndex] += 1;
					}
					if (j < 10) {
						Pat10test[locationIndex] += 1;
					}
				}
				if (classValueIndex == irrelevanceIndex && !firstNotRelevantFound) {
					firstNotRelevantFound = true;
					FNRPtest[locationIndex] = j + 1;
				}
				out.write(locationId + " 0 " + srs.getImageId() + " " + j + " " + srs.getRelevanceScore()
						+ " rel_run\n");
			}
			out.flush();

			Pat10test[locationIndex] /= 10.0;
			Pat20test[locationIndex] /= 20.0;
			Pat50test[locationIndex] /= 50.0;
			Pat100test[locationIndex] /= 100.0;

			long end = System.currentTimeMillis();
			// System.out.println("Total time in ms: " + (end - start) + "\n");
			System.gc();
		}
		out.close();

		System.out.println("Average Measures: ");
		System.out.print(Utils.mean(AuROCtest) + " ");
		System.out.print(Utils.mean(AuPRCtest) + " ");
		System.out.print(Utils.mean(Pat10test) + " ");
		System.out.print(Utils.mean(Pat20test) + " ");
		System.out.print(Utils.mean(Pat50test) + " ");
		System.out.print(Utils.mean(Pat100test) + " ");
		System.out.println(Utils.mean(FNRPtest));

	}

	/**
	 * Returns the position of the measure in the ArrayList or -1 if not found.
	 * 
	 * @param measures
	 * @param measure
	 * @return
	 */
	private static int findPosition(ArrayList<String> measures, String measure) {
		for (int i = 0; i < measures.size(); i++) {
			if (measure.equals(measures.get(i))) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Utility method to split the dataset according to location id. Useful for performing
	 * leave-one(location)-out cross-validation and for clustering only the instances of a particular
	 * location.<br>
	 * Notes:
	 * <ul>
	 * <li>Wikipedia images are always included in the full dataset. Their image ids are "-1".</li>
	 * </ul>
	 * 
	 * 
	 * @param locationId
	 *            The id of a location
	 * @param vadidationSet
	 *            The validation dataset containing instances of all validation locations
	 * @param numTrainLocations
	 *            The number of locations to be used as training data for this location
	 * @param wikiWeight
	 *            If wikiWeight > 0, the Wikipedia images for this location will be included as positive
	 *            examples for this location with the given weight (i.e. each wiki image will be repeated
	 *            wikiWeight times)
	 * @param useOnlyWikiAsPositive
	 *            If true, ONLY the Wikipedia images of this location will be used as positive (relevant)
	 *            examples for this location
	 * @return An array with two Instances objects. The 1st contains the TRAIN set for this location
	 *         (containing instances from other locations and possible the Wikipedia images of this location)
	 *         and the 2nd the TEST set for this location (all instances of this location, except for the
	 *         Wikipedia images)
	 * @throws Exception
	 */
	public static Instances[] createTrainTestSplitForLocationValidation(String locationId,
			Instances vadidationSet, int numTrainLocations, int wikiWeight, boolean useOnlyWikiAsPositive)
			throws Exception {
		// TO DO: check if this is 30 -> checked
		int numLocations = vadidationSet.attribute(Integer.valueOf(locationIdAttributeIndexString) - 1)
				.numValues();

		// 1. Apply the RemoveWithValues filter to split the dataset into a test set that contains only the
		// given location and a train set that contains all other locations!
		RemoveWithValues rwv = new RemoveWithValues();
		rwv.setAttributeIndex(locationIdAttributeIndexString);
		rwv.setNominalIndices(locationId);
		rwv.setInvertSelection(false);
		rwv.setInputFormat(vadidationSet);
		Instances locTrainData = Filter.useFilter(vadidationSet, rwv);
		rwv.setInvertSelection(true);
		Instances locTestData = Filter.useFilter(vadidationSet, rwv);
		// 2. Apply the RemoveWithValues filter again to keep only numTrainLocations from the train set!
		// 2a. Generate numTrainLocations location indices at random
		// initialize random generator using location id as seed
		Random rand = new Random(Integer.parseInt(locationId));
		HashSet<String> locationIds = new HashSet<String>();
		System.out.print(" using locations: ");
		while (locationIds.size() < numTrainLocations) {
			// nextInt generates a random int between 0 (inclusive) and numLocations (exclusive), therefore we
			// add 1
			int randomLocationId = rand.nextInt(numLocations) + 1;
			if (randomLocationId == Integer.parseInt(locationId)) {
				continue; // this location is already excluded from the train set, so select a different index
			}
			if (locationIds.add(String.valueOf(randomLocationId))) {
				System.out.print(randomLocationId + " ");
			}
		}
		System.out.println();
		// 2b. Do the filtering of these indices
		String nominalIndices = "";
		for (String index : locationIds) {
			nominalIndices += index + ",";
		}
		nominalIndices = nominalIndices.substring(0, nominalIndices.length() - 1);
		rwv = new RemoveWithValues();
		rwv.setAttributeIndex(locationIdAttributeIndexString);
		rwv.setNominalIndices(nominalIndices);
		rwv.setInvertSelection(true);
		rwv.setInputFormat(vadidationSet);
		locTrainData = Filter.useFilter(locTrainData, rwv);

		// 3. Now work with the Wikipedia images of the two sets
		// 3.a If useOnlyWikiAsPositive is true
		if (useOnlyWikiAsPositive) {
			// remove all positive (relevant) examples from the train set, except for the Wikipedia images of
			// this location. Negative (irrelevant) examples are kept
			// rwv = new RemoveWithValues();
			// rwv.setAttributeIndex(String.valueOf(locTrainData.numAttributes()));
			// rwv.setNominalIndices(String.valueOf(relevanceIndex + 1));
			// rwv.setInputFormat(locTrainData);
			// locTrainData = Filter.useFilter(locTrainData, rwv);
			// new version: set all examples from other locations as negative
			for (int i = 0; i < locTrainData.numInstances(); i++) {
				locTrainData.instance(i).setClassValue((double) irrelevanceIndex);
			}
			Resample res = new Resample();
			res.setSampleSizePercent(10.0);
			res.setNoReplacement(true);
			res.setInputFormat(locTrainData);
			locTrainData = Filter.useFilter(locTrainData, res);

		}
		// 3.b Remove Wikipedia images of this location from test set and add them in train set with
		// appropriate weights
		rwv = new RemoveWithValues();
		rwv.setAttributeIndex(imageIdAttributeIndexString);
		rwv.setSplitPoint(-0.5); // instances with smaller values will be selected
		rwv.setInvertSelection(true);
		rwv.setInputFormat(vadidationSet);
		// keep the images to add them in train set later
		Instances wikiImagesOfLocation = Filter.useFilter(locTestData, rwv);
		rwv.setInvertSelection(false);
		locTestData = Filter.useFilter(locTestData, rwv);

		for (int i = 0; i < wikiImagesOfLocation.numInstances(); i++) {
			for (int w = 0; w < wikiWeight; w++) { // add it in the training set wikiWeight times
				locTrainData.add(wikiImagesOfLocation.instance(i));
			}
		}

		// System.out.println("Instances in training set for location " + locationId + ": " +
		// locTrainData.numInstances());
		// System.out.println("Instances in test set for location " + locationId + ": " +
		// locTestData.numInstances());

		Instances sets[] = new Instances[2];
		sets[0] = locTrainData;
		sets[1] = locTestData;

		return sets;
	}

	/**
	 * Utility method to split the dataset according to location id. Useful for performing
	 * leave-one(location)-out cross-validation and for clustering only the instances of a particular
	 * location.<br>
	 * Notes:
	 * <ul>
	 * <li>Wikipedia images are always included in the full dataset. Their image ids are "-1".</li>
	 * </ul>
	 * 
	 * 
	 * @param locationId
	 *            The id of a location
	 * @param trainDataset
	 *            The training dataset
	 * @param testDataset
	 *            The test dataset
	 * @param numTrainLocations
	 *            The number of locations to be used as training data for this location
	 * @param wikiWeight
	 *            If wikiWeight > 0, the Wikipedia images for this location will be included as positive
	 *            examples for this location with the given weight (i.e. each wiki image will be repeated
	 *            wikiWeight times)
	 * @param useOnlyWikiAsPositive
	 *            If true, ONLY the Wikipedia images of this location will be used as positive (relevant)
	 *            examples for this location
	 * @return An array with two Instances objects. The 1st contains the TRAIN set for this location
	 *         (containing instances from other locations and possible the Wikipedia images of this location)
	 *         and the 2nd the TEST set for this location (all instances of this location, except for the
	 *         Wikipedia images)
	 * @throws Exception
	 */
	public static Instances[] createTrainTestSplitForLocationTest(String locationId, Instances trainDataset,
			Instances testDataset, int wikiWeight, boolean useOnlyWikiAsPositive) throws Exception {
		// {!!! Do not modify the original datasets !!!
		Instances finalTrainDataset = new Instances(trainDataset, 0);
		Instances finalTestDataset = new Instances(testDataset, 0);
		// }

		int locationIdAttributeIndexInt = Integer.parseInt(locationIdAttributeIndexString) - 1;
		int imageIdAttributeIndexInt = Integer.parseInt(imageIdAttributeIndexString) - 1;

		// { Make the dataset headers compatible
		String nominalLabels = "";
		int numValuesTrain = finalTrainDataset.attribute(locationIdAttributeIndexInt).numValues();
		for (int i = 0; i < numValuesTrain; i++) {
			nominalLabels += finalTrainDataset.attribute(locationIdAttributeIndexInt).value(i) + ",";
		}
		int numValuesTest = finalTestDataset.attribute(locationIdAttributeIndexInt).numValues();
		for (int i = 0; i < numValuesTest; i++) {
			nominalLabels += finalTestDataset.attribute(locationIdAttributeIndexInt).value(i) + ",";
		}
		AddValues av = new AddValues();
		av.setLabels(nominalLabels.substring(0, nominalLabels.length() - 1));
		av.setAttributeIndex(locationIdAttributeIndexString);
		av.setInputFormat(finalTrainDataset);
		finalTrainDataset = Filter.useFilter(finalTrainDataset, av);
		finalTestDataset = Filter.useFilter(finalTestDataset, av);
		// }

		// { Construct the final training and test sets
		for (int i = 0; i < testDataset.numInstances(); i++) {
			Instance candidate = new DenseInstance(testDataset.instance(i));
			candidate.setDataset(finalTestDataset);
			// modify the value of the location id attibute
			int canLocationIdValueIndex = (int) candidate.value(locationIdAttributeIndexInt);
			int correctLocIdValueIndex = canLocationIdValueIndex + numValuesTrain;
			candidate.setValue(locationIdAttributeIndexInt, correctLocIdValueIndex);

			String canLocationId = finalTestDataset.attribute(locationIdAttributeIndexInt).value(
					correctLocIdValueIndex);
			double canImageId = candidate.value(imageIdAttributeIndexInt); // tells us if this is a wiki image

			if (canLocationId.equals(locationId)) { // an image of the location to be predicted
				if (canImageId == -1) { // a wikipedia image of the location to be predicted
					// add it in final training set for the appropriate number of times
					if (candidate.classIsMissing()) {
						throw new Exception("Unexpected!");
					}
					for (int j = 0; j < wikiWeight; j++) {
						finalTrainDataset.add(candidate);
					}
				} else {// add it in final test set
					finalTestDataset.add(candidate);
					if (!candidate.classIsMissing()) {
						// this is no longer unexpected because we have the test gt as well!
						// throw new Exception("Unexpected!");
					}
				}
			} else {
				if (!useOnlyWikiAsPositive) {
					// adding wikipedia images of other test locations in training set as positive examples?
					if (canImageId == -1) {
						finalTrainDataset.add(candidate);
						if (candidate.classIsMissing()) {
							throw new Exception("Unexpected!");
						}
					}
				}
			}
		}

		if (!useOnlyWikiAsPositive) {
			for (int i = 0; i < trainDataset.numInstances(); i++) {
				// just copying all instances in the final training set!
				finalTrainDataset.add(new DenseInstance(trainDataset.instance(i)));
			}
		}
		// }

		Instances sets[] = new Instances[2];
		sets[0] = finalTrainDataset;
		sets[1] = finalTestDataset;

		return sets;
	}

	public static Classifier selectClassifier(String choice, double parameter) throws Exception {
		if (choice.equals("j48")) {
			J48 j48 = new J48();
			// j48.setBinarySplits(true);
			// j48.setReducedErrorPruning(true);
			// j48.setSubtreeRaising(false);
			// j48.setNumFolds(2);
			return j48;
		} else if (choice.equals("logistic")) {
			Logistic log = new Logistic();
			log.setRidge(parameter);
			return log;
		} else if (choice.equals("logitboost")) {
			LogitBoost lb = new LogitBoost();
			// lb.setNumIterations(100);
			// lb.setNumIterations((int) parameter);
			return lb;
		} else if (choice.equals("adaboost")) {
			AdaBoostM1 ada = new AdaBoostM1();
			return ada;
		} else if (choice.equals("rf")) {
			RandomForest rf = new RandomForest();
			// rf.setNumTrees(100);
			// rf.setNumTrees((int) parameter);
			rf.setNumExecutionSlots(3);
			return rf;
		} else if (choice.equals("ibk")) {
			IBk knn = new IBk();
			knn.setKNN(1);
			return knn;
		} else if (choice.equals("nb")) {
			NaiveBayes nb = new NaiveBayes();
			return nb;
		} else if (choice.equals("reptree")) {
			REPTree rep = new REPTree();
			// rep.setNumFolds(10);
			return rep;
		} else if (choice.equals("bagging")) {
			Bagging bag = new Bagging();
			bag.setNumIterations(10);
			bag.setNumExecutionSlots(3);
			return bag;
		} else if (choice.equals("bagging-j48")) {
			Bagging bag = new Bagging();
			bag.setClassifier(new J48());
			bag.setNumIterations(100);
			bag.setNumExecutionSlots(3);
			return bag;
		} else if (choice.equals("zeror")) {
			ZeroR zeror = new ZeroR();
			return zeror;
		} else if (choice.equals("stump")) {
			DecisionStump stump = new DecisionStump();
			return stump;
		} else if (choice.equals("dt")) {F
			DecisionTable dt = new DecisionTable();
			return dt;
		} else if (choice.equals("logistic")) {
			Logistic log = new Logistic();
			return log;
		} else if (choice.equals("smo")) {
			SMO smo = new SMO();
			return smo;
		} else {
			throw new Exception("Wrong selection");
		}
	}
}
