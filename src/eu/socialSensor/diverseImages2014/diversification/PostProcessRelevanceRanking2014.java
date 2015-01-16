package eu.socialSensor.diverseImages2014.diversification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import weka.core.Utils;
import eu.socialSensor.diverseImages2014.MEDI2014Collection;
import eu.socialSensor.diverseImages2014.MEDI2014Location;
import eu.socialSensor.diverseImages2014.datasetCreation.PairWiseDistanceComputation;
import eu.socialSensor.diverseImages2014.eval.MEDI2014Evaluation;
import eu.socialSensor.diverseImages2014.eval.Medi2014LocationEval;
import eu.socialSensor.diverseImages2014.utils.MediImageComparator2014;

/**
 * The main of this class takes as input a submission file where images for each location are ranked by their
 * relevance to that location (according to some criterion) and re-ranks these images so that diversity is
 * imposed.
 * 
 * This class works for both devset and testset!
 * 
 * @author Eleftherios Spyromitros-Xioufis
 */
public class PostProcessRelevanceRanking2014 {

	public static boolean printPerLocationResults = false;

	public static enum diversityAggregationMethods {
		AVG, // diversity = average dissimilarity to the selected images
		MIN // diversity = dissimilarity to the most similar of the selected images
	}

	/**
	 * Each time that the ReDiv re-ranking method is called, this field is updated with all pair-wise
	 * distances of a specific location.
	 */
	private ArrayList<ArrayList<HashMap<HashSet<Long>, Double>>> pairWiseDistances;

	/** because the official measures of Diverse Images 2014 are calculated at this cut-off */
	// public static final int topK = Evaluation2014.officialCutoff2014;
	public static final int topK = 20;

	/**
	 * 
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String collectionPath = args[0];
		String[] features = args[1].split(",");
		String[] norms = args[2].split(",");
		String[] dfuncs = args[3].split(",");
		String submissionFileWithScores = args[4];
		String postProcessingString = args[5];
		String postProcessingMethod = postProcessingString.split(" ")[0];

		// This collection object is needed so that the requested feature vectors are available during
		// diversity
		// calculation. Only the requested feature vectors are loaded.
		MEDI2014Collection dataStore = new MEDI2014Collection(collectionPath);
		boolean loadWiki = false; // features for the Wikipedia images are not be needed
		// we do not actually need to load the features because we have pre-computed the distances!
		dataStore.loadAll(loadWiki, new String[0], new String[0]);

		// This TreeMap objects contains duplicate information with the previous object but is still loaded to
		// allow
		// re-use of 2013's evaluation code that expects a Map of MediLocation2014 objects to perform the
		// evaluation.
		// Alternatively, the official scoring tool could be used for evaluation.
		TreeMap<Integer, Medi2014LocationEval> locationsForEval = MEDI2014Evaluation.loadGroundTruth(
				collectionPath, true);

		HashMap<Integer, ArrayList<MediImageComparator2014>> allRelevanceScores = parseSubmissionFile(submissionFileWithScores);

		String submissionFilePP = "";
		String suffix = "_" + postProcessingMethod;
		if (postProcessingMethod.equals("skip")) {
			int numToSkip = Integer.parseInt(postProcessingString.split(" ")[1]);
			suffix += "_" + numToSkip;
			submissionFilePP = submissionFileWithScores + suffix + ".txt";
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(submissionFilePP)));
			for (MEDI2014Location location : dataStore.getLocationList()) {
				int locationId = location.getQueryId();
				ArrayList<MediImageComparator2014> relevanceOrderedImages = allRelevanceScores
						.get(locationId);
				ArrayList<MediImageComparator2014> rAndDOrderedImages = PostProcessRelevanceRanking2014
						.postProcessSkip(relevanceOrderedImages, numToSkip);

				for (int i = 0; i < rAndDOrderedImages.size(); i++) {
					// out.write(location.getId() + " 0 " + images.get(i).getImageId() + " " + i + " "
					// + images.get(i).getCombinedScore() + " rel_run\n");
					out.write(location.getQueryId() + " 0 " + rAndDOrderedImages.get(i).getImageId() + " "
							+ i + " " + ((double) 1 / (i + 1)) + " rel_run\n");
				}
				out.flush();
			}
			out.close();

			double[] results = MEDI2014Evaluation.evaluate(submissionFilePP, locationsForEval, false,
					MEDI2014Evaluation.officialCutoff2014);
			double PatX = results[0];
			double CRatX = results[1];
			double F1atX = results[2];
			System.out.print(numToSkip + " " + PatX + " " + CRatX + " " + F1atX + "\n");
		} else if (postProcessingMethod.equals("random")) { // report average results of n random runs!
			int numToConsider = Integer.parseInt(postProcessingString.split(" ")[1]);
			int numRuns = Integer.parseInt(postProcessingString.split(" ")[2]);
			suffix += numToConsider;
			submissionFilePP = submissionFileWithScores + suffix + ".txt";
			double avgPatX = 0;
			double avgCRatX = 0;
			double avgF1atX = 0;

			for (int k = 0; k < numRuns; k++) {
				BufferedWriter out = new BufferedWriter(new FileWriter(new File(submissionFilePP)));
				for (MEDI2014Location location : dataStore.getLocationList()) {
					int locationId = location.getQueryId();
					ArrayList<MediImageComparator2014> relevanceOrderedImages = allRelevanceScores
							.get(locationId);
					ArrayList<MediImageComparator2014> rAndDOrderedImages = PostProcessRelevanceRanking2014
							.postProcessRandom(relevanceOrderedImages, numToConsider, k);
					for (int i = 0; i < rAndDOrderedImages.size(); i++) {
						out.write(locationId + " 0 " + rAndDOrderedImages.get(i).getImageId() + " " + i + " "
								+ ((double) 1 / (i + 1)) + " rel_run\n");
					}
					out.flush();
				}
				out.close();
				double[] results = MEDI2014Evaluation.evaluate(submissionFilePP, locationsForEval, false,
						MEDI2014Evaluation.officialCutoff2014);
				avgPatX += results[0];
				avgCRatX += results[1];
				avgF1atX += results[2];
			}
			System.out.print(numToConsider + " " + avgPatX / numRuns + " " + avgCRatX / numRuns + " "
					+ avgF1atX / numRuns + "\n");

		} else if (postProcessingMethod.equals("oracle")) {
			int numToConsider = Integer.parseInt(postProcessingString.split(" ")[1]);
			suffix += numToConsider;
			submissionFilePP = submissionFileWithScores + suffix + ".txt";
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(submissionFilePP)));
			for (MEDI2014Location location : dataStore.getLocationList()) {
				int locationId = location.getQueryId();
				ArrayList<MediImageComparator2014> relevanceOrderedImages = allRelevanceScores
						.get(locationId);
				ArrayList<MediImageComparator2014> rAndDOrderedImages = PostProcessRelevanceRanking2014
						.postProcessOracle(relevanceOrderedImages, numToConsider, location);

				for (int i = 0; i < rAndDOrderedImages.size(); i++) {
					// out.write(location.getId() + " 0 " + images.get(i).getImageId() + " " + i + " "
					// + images.get(i).getCombinedScore() + " rel_run\n");
					out.write(location.getQueryId() + " 0 " + rAndDOrderedImages.get(i).getImageId() + " "
							+ i + " " + ((double) 1 / (i + 1)) + " rel_run\n");
				}
				out.flush();
			}
			out.close();

			double[] results = MEDI2014Evaluation.evaluate(submissionFilePP, locationsForEval, false,
					MEDI2014Evaluation.officialCutoff2014);
			double PatX = results[0];
			double CRatX = results[1];
			double F1atX = results[2];
			System.out.print(numToConsider + " " + PatX + " " + CRatX + " " + F1atX + "\n");
		} else if (postProcessingMethod.equals("relonly")) {
			submissionFilePP = submissionFileWithScores + suffix + ".txt";
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(submissionFilePP)));
			for (MEDI2014Location location : dataStore.getLocationList()) {
				int locationId = location.getQueryId();
				ArrayList<MediImageComparator2014> relevanceOrderedImages = allRelevanceScores
						.get(locationId);
				ArrayList<MediImageComparator2014> rAndDOrderedImages = PostProcessRelevanceRanking2014
						.postProcessRemoveIrrelevant(relevanceOrderedImages, location);

				for (int i = 0; i < rAndDOrderedImages.size(); i++) {
					// out.write(location.getId() + " 0 " + images.get(i).getImageId() + " " + i + " "
					// + images.get(i).getCombinedScore() + " rel_run\n");
					out.write(location.getQueryId() + " 0 " + rAndDOrderedImages.get(i).getImageId() + " "
							+ i + " " + ((double) 1 / (i + 1)) + " rel_run\n");
				}
				out.flush();
			}
			out.close();

			double[] results = MEDI2014Evaluation.evaluate(submissionFilePP, locationsForEval, false,
					MEDI2014Evaluation.officialCutoff2014);
			double PatX = results[0];
			double CRatX = results[1];
			double F1atX = results[2];
			System.out.print("- " + PatX + " " + CRatX + " " + F1atX + "\n");
		} else if (postProcessingMethod.startsWith("rediv")) {

			String diversityAggregationMethodString = postProcessingString.split(" ")[2];
			diversityAggregationMethods diversityMethod;
			if (diversityAggregationMethodString.equals("min")) {
				diversityMethod = PostProcessRelevanceRanking2014.diversityAggregationMethods.MIN;
			} else if (diversityAggregationMethodString.equals("avg")) {
				diversityMethod = PostProcessRelevanceRanking2014.diversityAggregationMethods.AVG;
			} else {
				throw new Exception("Wrong diversity aggregation method!");
			}

			int topKSets = 1;
			if (postProcessingMethod.equals("rediva")) {
				topKSets = Integer.parseInt(postProcessingString.split(" ")[4]);
			}

			String[] partialListString = args[6].split(" ");
			String[] weightString = args[7].split(" ");
			int partialListStart = Integer.parseInt(partialListString[0]);
			int partialListStep = Integer.parseInt(partialListString[1]);
			int partialListNumSteps = Integer.parseInt(partialListString[2]);
			double weightStart = Double.parseDouble(weightString[0]);
			double weightStep = Double.parseDouble(weightString[1]);
			int weightNumSteps = Integer.parseInt(weightString[2]);

			suffix += "_" + diversityMethod + "_" + "_" + args[1] + "_" + partialListStart;
			submissionFilePP = submissionFileWithScores + suffix + ".txt";

			PostProcessRelevanceRanking2014 pp = new PostProcessRelevanceRanking2014(diversityMethod,
					dataStore, features, norms, dfuncs);

			BufferedWriter outResults = new BufferedWriter(new FileWriter(new File(submissionFilePP.replace(
					".txt", "-stats.txt"))));

			double bestF1atX = 0;
			int bestK = 0;
			double bestW = 0;

			for (int k = 0; k < partialListNumSteps; k++) {
				int numToConsider = partialListStart + k * partialListStep;
				double bestWThisK = 0;
				double bestF1atXThisK = 0;
				for (int j = 0; j < weightNumSteps; j++) {
					double weight = weightStart + j * weightStep;
					// rounding to 2 decimal places
					weight = new BigDecimal(weight).setScale(2, RoundingMode.HALF_UP).doubleValue();

					BufferedWriter out = new BufferedWriter(new FileWriter(new File(submissionFilePP)));
					int locationIndexInCollection = 0;
					for (MEDI2014Location location : dataStore.getLocationList()) {
						int locationId = location.getQueryId();
						ArrayList<MediImageComparator2014> relevanceOrderedImages = allRelevanceScores
								.get(locationId);
						ArrayList<Long> reRankedImages = pp.postProcessRD(relevanceOrderedImages, weight,
								numToConsider);

						for (int i = 0; i < reRankedImages.size(); i++) {
							out.write(location.getQueryId() + " 0 " + reRankedImages.get(i) + " " + i + " "
									+ ((double) 1 / (i + 1)) + " rel_run\n");
						}
						out.flush();
						locationIndexInCollection++;
					}
					out.close();

					System.out.print(numToConsider + " " + weight + " ");
					double[] results = MEDI2014Evaluation.evaluate(submissionFilePP, locationsForEval,
							printPerLocationResults, MEDI2014Evaluation.officialCutoff2014);
					double PatX = results[0];
					double CRatX = results[1];
					double F1atX = results[2];

					if (F1atX > bestF1atXThisK) {
						bestF1atXThisK = F1atX;
						bestWThisK = weight;
					}

					outResults.write(numToConsider + " " + weight + " " + PatX + " " + CRatX + " " + F1atX
							+ "\n");
					outResults.flush();
				}
				System.out.println("Best F1@20: " + bestF1atXThisK + " with params: k=" + numToConsider
						+ ",w=" + bestWThisK);
				if (bestF1atXThisK > bestF1atX) {
					bestF1atX = bestF1atXThisK;
					bestK = numToConsider;
					bestW = bestWThisK;
				}
			}
			outResults.close();
			System.out.println("Best F1@20: " + bestF1atX + " with params: k=" + bestK + ",w=" + bestW);

		} else {
			throw new Exception("Wrong post-processing method!");
		}

	}

	/**
	 * Parses a submission file and returns a HashMap with location ids as keys and an ArrayList of
	 * MediComparator2014 objects as values. The MediImageComparator2014 objects are ordered by descending
	 * relevance score in the ArrayList.
	 * 
	 * @param submissionFileName
	 * @return
	 */
	public static HashMap<Integer, ArrayList<MediImageComparator2014>> parseSubmissionFile(
			String submissionFileName) throws IOException {
		HashMap<Integer, ArrayList<MediImageComparator2014>> allRelevanceScores = new HashMap<Integer, ArrayList<MediImageComparator2014>>();
		BufferedReader in = new BufferedReader(new FileReader(new File(submissionFileName)));
		ArrayList<MediImageComparator2014> locationRelevanceScores = new ArrayList<MediImageComparator2014>();
		int locationId = -1;
		String line;
		int imageRankingCounter = 0;
		while ((line = in.readLine()) != null) {
			int thisLocationId = Integer.parseInt(line.split(" ")[0]);
			if (thisLocationId != locationId) { // first or new location
				if (locationId != -1) { // if not the 1st location
					// add ArrayList of previous location in the HashMap
					allRelevanceScores.put(locationId, locationRelevanceScores);
					locationRelevanceScores = new ArrayList<MediImageComparator2014>(); // empty the ArrayList
																						// of images
					imageRankingCounter = 0;
				}
				locationId = thisLocationId;// update the locationId
			}
			imageRankingCounter++;
			String imagId = line.split(" ")[2];
			double relevanceScore = Double.parseDouble(line.split(" ")[4]);
			MediImageComparator2014 image = new MediImageComparator2014(imagId, locationId, relevanceScore);
			locationRelevanceScores.add(image);
		}
		// add last location in the HashMap
		allRelevanceScores.put(locationId, locationRelevanceScores);

		in.close();
		return allRelevanceScores;
	}

	/**
	 * Post-processes a given list of relevance-ordered images and removes irrelevant images using the
	 * location's ground truth.
	 * 
	 * @param images
	 *            The relevance-ordered image list.
	 * @param loc
	 *            A MEDI2014Location object with ground truth for the location of these images.
	 * @return An image list with irrelevant images removed.
	 */
	public static ArrayList<MediImageComparator2014> postProcessRemoveIrrelevant(
			ArrayList<MediImageComparator2014> images, MEDI2014Location loc) {
		// get the relevant images of this location
		HashMap<String, Integer> relImages = loc.getRelevantImages();

		ArrayList<MediImageComparator2014> topRelevantImages = new ArrayList<MediImageComparator2014>(
				relImages.size());
		for (int i = 0; i < images.size(); i++) {
			String imageId = images.get(i).getImageId();
			if (relImages.containsKey(imageId + ".jpg")) {
				topRelevantImages.add(images.get(i));
			}
		}
		return topRelevantImages;
	}

	/**
	 * Performs an oracle post-processing of a given list of relevance-ordered images using the location's
	 * ground truth.
	 * 
	 * @param images
	 *            The relevance-ordered image list.
	 * @param numImagesToConsider
	 *            How many of the most relevant images to consider when performing the post-processing.
	 * @param loc
	 *            A MEDI2014Location object with ground truth for the location of these images.
	 * @return A perfectly diversified image list.
	 */
	public static ArrayList<MediImageComparator2014> postProcessOracle(
			ArrayList<MediImageComparator2014> images, int numImagesToConsider, MEDI2014Location loc) {
		// create a copy of the given ArrayList that contains only the top numMostRelevantToConsider images
		int size = Math.min(images.size(), numImagesToConsider);
		ArrayList<MediImageComparator2014> topImages = new ArrayList<MediImageComparator2014>(size);
		for (int i = 0; i < size; i++) {
			topImages.add(images.get(i));
		}

		// get one image from each different cluster!
		HashMap<String, Integer> relImageClusters = loc.getRelevantImages();
		HashMap<Integer, ArrayList<Integer>> distinctClusters = new HashMap<Integer, ArrayList<Integer>>();
		for (int i = 0; i < size; i++) {
			String imageId = topImages.get(i).getImageId() + ".jpg";
			Integer clusterId = relImageClusters.get(imageId);
			if (clusterId == null) {
				clusterId = -1;
			}
			ArrayList<Integer> existing = distinctClusters.get(clusterId);
			if (existing == null) {
				existing = new ArrayList<Integer>();
			}
			existing.add(i);
			distinctClusters.put(clusterId, existing);
		}

		// iterate through distinct clusters and select one image form each cluster except the -1!
		ArrayList<MediImageComparator2014> oracleImages = new ArrayList<MediImageComparator2014>();
		int i = 0;
		while (oracleImages.size() < MEDI2014Evaluation.officialCutoff2014) { // while we haven't selected 20
																				// images yet
			// iterate through the clusters and select the i-th image from each (if an i-th image exists!);
			boolean allImagesVisited = true; // flag that signs that all relevant images have been selected
			for (Entry<Integer, ArrayList<Integer>> cluster : distinctClusters.entrySet()) {
				int clusterId = cluster.getKey();
				if (clusterId == -1) { // skip the irrelevant images cluster
					continue;
				} else {
					ArrayList<Integer> clusterImages = cluster.getValue();
					if (clusterImages.size() > i) {// check if this cluster contains an i-th image
						allImagesVisited = false; // at least one i-th image was found
						// get the i-the image (actually the image's index in the topImages list!)
						int imageIndexInTopK = clusterImages.get(i);
						oracleImages.add(topImages.get(imageIndexInTopK));
					}
				}
			}
			if (allImagesVisited) { // no more relevant images to add
				break;
			}
			i++;
		}

		// if the oracleImagesList is still under-populated, ad images from the irrelevant cluster
		int remainingImages = MEDI2014Evaluation.officialCutoff2014 - oracleImages.size();
		if (remainingImages > 0) {
			ArrayList<Integer> irrelevant = distinctClusters.get(-1);
			for (int j = 0; j < remainingImages; j++) {
				oracleImages.add(topImages.get(irrelevant.get(j)));
			}
		}

		return oracleImages;
	}

	/**
	 * Performs random re-ranking of the topk most relevant images. Serves as a baseline.
	 * 
	 * @param images
	 *            The relevance-ordered image list.
	 * @param numImagesToConsider
	 *            How many of the most relevant images to consider when performing the post-processing.
	 * @return A randomly diversified image list.
	 */
	public static ArrayList<MediImageComparator2014> postProcessRandom(
			ArrayList<MediImageComparator2014> images, int numImagesToConsider, int seed) {
		// create a copy of the given ArrayList that contains only the top numMostRelevantToConsider images
		int size = Math.min(images.size(), numImagesToConsider);
		ArrayList<MediImageComparator2014> topImages = new ArrayList<MediImageComparator2014>(size);
		for (int i = 0; i < size; i++) {
			topImages.add(images.get(i));
		}

		// do a random permutation of the top images
		Collections.shuffle(topImages, new Random(seed));

		// add the first topK images of the shuffled topImages list into the selectedSoFar list
		ArrayList<MediImageComparator2014> selectedSoFar = new ArrayList<MediImageComparator2014>();
		for (int i = 0; i < topK; i++) {
			selectedSoFar.add(topImages.get(i));
		}

		return selectedSoFar;
	}

	/**
	 * 
	 * @param images
	 * @return
	 */
	public static ArrayList<MediImageComparator2014> postProcessSkip(
			ArrayList<MediImageComparator2014> images, int numImagesToSkip) {
		for (int i = 0; i < numImagesToSkip; i++) {
			images.remove(0);
		}
		return images;
	}

	private MEDI2014Collection collection;

	private String[] features;
	// private String[] norms;
	// private String[] dFuncs;

	/**
	 * This is used for creating artificial diversity scores!
	 */
	Random rand;

	private diversityAggregationMethods diversityAggregationMethod;

	public PostProcessRelevanceRanking2014(diversityAggregationMethods diversityMethod,
			MEDI2014Collection collection, String[] features, String[] norms, String[] dFuncs)
			throws Exception {
		this.diversityAggregationMethod = diversityMethod;
		this.collection = collection;
		this.features = features;
		this.rand = new Random(1);

		int numLocations = collection.getLocationList().size();
		pairWiseDistances = new ArrayList<ArrayList<HashMap<HashSet<Long>, Double>>>(numLocations);

		for (int i = 0; i < numLocations; i++) {
			// load the required distances (and optionally models) for this location
			// read all pairwise distances for this location
			int locationId = collection.getLocationList().get(i).getQueryId();
			System.out.println("Loading distances (and optionally models) for location " + locationId);
			pairWiseDistances.add(PairWiseDistanceComputation.readPairwiseDistances(collection.getRootDir(),
					features, norms, dFuncs, locationId));
		}
	}

	private double aggregateDivesityScores(ArrayList<Double> diversityScores) {
		double distance = 0;
		if (diversityAggregationMethod == diversityAggregationMethods.AVG) {
			double avgDiversity = 0;
			for (double diversityScore : diversityScores) {
				avgDiversity += diversityScore;
			}
			avgDiversity /= diversityScores.size();
			distance = avgDiversity;
		} else if (diversityAggregationMethod == diversityAggregationMethods.MIN) {
			double minDiversity = Double.MAX_VALUE;
			for (double diversityScore : diversityScores) {
				if (diversityScore < minDiversity) {
					minDiversity = diversityScore;
				}
			}
			distance = minDiversity;
		}
		return distance;
	}

	private double calculateCombinedScore(double relevanceScore, double diversityScore, double weight) {
		return weight * relevanceScore + (1 - weight) * diversityScore;
	}

	/**
	 * In this method, the diversity score of the candidate image is calculated by aggregating the distances
	 * of this image from the images already selected. The larger the distance the aggregated distance, the
	 * higher the diversity.
	 * 
	 * @param locationIndex
	 * @param candidateId
	 * @param selectedSoFarIds
	 * @return
	 * @throws Exception
	 */
	private double computeDiversity(int locationIndex, long candidateId, HashSet<Long> selectedSoFarIds)
			throws Exception {
		ArrayList<Double> combinedDistances = new ArrayList<Double>();
		for (long selectedId : selectedSoFarIds) {
			if (selectedId == candidateId) {
				continue;
			}
			HashSet<Long> pair = new HashSet<Long>(2);
			pair.add(candidateId);
			pair.add(selectedId);
			double[] distances = new double[features.length];
			for (int i = 0; i < features.length; i++) {
				Double distance = pairWiseDistances.get(locationIndex).get(i).get(pair);
				if (distance == null) {
					throw new Exception("Distnace not found!");
				} else {
					distances[i] = distance;
				}
			}
			// currently, distances from different modalities are averaged
			double combinedDistance = Utils.mean(distances);
			combinedDistances.add(combinedDistance);
		}
		return aggregateDivesityScores(combinedDistances);
	}

	/**
	 * This is the actual implementation of the RD post processing algorithm. The method takes two arguments:
	 * <ul>
	 * <li>An ordered list (ArrayList) of MediImageComparator2014 objects, i.e. image ids with relevance
	 * scores</li>
	 * <li>A weight in the [0-1] range that corresponds to the a parameter of the RD method, i.e. the
	 * importance of relevance in the final RD score</li>
	 * </ul>
	 *
	 * @param images
	 * @param weight
	 * @return
	 * @throws Exception
	 */
	public ArrayList<Long> postProcessRD(ArrayList<MediImageComparator2014> images, double weight,
			int numMostRelevantToConsider) throws Exception {
		if (numMostRelevantToConsider < topK) {
			throw new Exception("The number of most relevant images to be considered should be larger than "
					+ topK);
		}
		if (images.size() < topK) {
			throw new Exception("The size of the relevance ordered image list should be larger than " + topK);
		}

		// create a copy of the given ArrayList that contains only the top numMostRelevantToConsider images
		int size = Math.min(images.size(), numMostRelevantToConsider);
		ArrayList<MediImageComparator2014> topImages = new ArrayList<MediImageComparator2014>(size);
		for (int i = 0; i < size; i++) {
			topImages.add(images.get(i));
		}

		// create a new list that will hold the R&D re-ranked images
		ArrayList<Long> rerankedSoFar = new ArrayList<Long>();

		// remove the most relevant image from the unselected images (topImages) list and add it as first to
		// the
		// rerankedSoFar list after updating its R&D (combined) score
		MediImageComparator2014 selectedFirst = topImages.remove(0);
		// the R&D score of the 1st image is equal to its R score multiplied by w (the diversity score is
		// still 0)
		selectedFirst.setCombinedScore(weight * selectedFirst.getRelevanceScore());
		rerankedSoFar.add(Long.parseLong(selectedFirst.getImageId()));

		// for topK - 1 steps
		for (int j = 1; j < topK; j++) {
			// calculate the R&D scores of all unselected images (topImages list)
			// TO DO: modify the code to store the k top scoring images so that back-tracking is possible
			int maxIndex = -1;
			double maxCombinedScore = -Double.MAX_VALUE; // minus is very important
			// in current implementation, images are always visited as they are order in topImages list,
			// i.e. descending relevance order
			for (int i = 0; i < topImages.size(); i++) {
				MediImageComparator2014 candidateImage = topImages.get(i);

				double relevanceScore = candidateImage.getRelevanceScore();
				double diversityScore = -1000;
				// double diversityScore = computeDiversity(candidateImage, rerankedSoFar, attributes,
				// norms[0]);
				double combinedScore = calculateCombinedScore(relevanceScore, diversityScore, weight);

				if (combinedScore > maxCombinedScore) {
					maxCombinedScore = combinedScore;
					maxIndex = i;
				}
			}

			// remove the highest scoring image from the unselected images (topImages) list and add it to the
			// rerankedSoFar list after updating its R&D (combined) score
			MediImageComparator2014 selected = topImages.remove(maxIndex);
			selected.setCombinedScore(maxCombinedScore);
			rerankedSoFar.add(Long.parseLong(selected.getImageId()));
		}

		return rerankedSoFar;
	}

}
