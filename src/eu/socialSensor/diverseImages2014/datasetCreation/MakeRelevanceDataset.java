package eu.socialSensor.diverseImages2014.datasetCreation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.lang3.ArrayUtils;

import eu.socialSensor.diverseImages2014.MEDI2014Collection;
import eu.socialSensor.diverseImages2014.MEDI2014Image;
import eu.socialSensor.diverseImages2014.MEDI2014ImageBase;
import eu.socialSensor.diverseImages2014.MEDI2014Location;
import eu.socialSensor.diverseImages2014.utils.Normalizations;

/**
 * Create ARFF files for use with Weka machine learning toolbox.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class MakeRelevanceDataset {

	public static final int locationIdsStart = 1;
	public static final int locationIdsEnd = 153;

	/**
	 * total length of all features, calculated during header writing
	 */
	private int totalLength = 0;

	private String[] featureTypes;

	private MEDI2014Collection dataStore;

	public MakeRelevanceDataset(MEDI2014Collection dataStore, String[] featureTypes) {
		this.dataStore = dataStore;
		this.featureTypes = featureTypes;
	}

	/*
	 * Writes header of ARFF file (specifying fieldnames, data types etc) and then appends actual data.
	 */
	public void writeFile(String filename, String[] individualNorms, String normalizeLevel1,
			String normalizeFinal, boolean sparse) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("datasets/relevance/" + filename)));
		// writing the header of the arff
		out.write(generateArffHeader());
		// writing the actual data
		ArrayList<MEDI2014Location> locs = dataStore.getLocationList();
		// int locIndex = 0;
		for (MEDI2014Location loc : locs) {
			System.out.println(loc.getLocationName());
			ArrayList<MEDI2014ImageBase> ims = loc.getImageList();
			for (MEDI2014ImageBase im : ims) {
				System.out.println("Location: " + loc.getQueryId() + "-" + loc.getLocationName() + " Image: "
						+ im.getId());
				// the wikiIndex parameter is not used in this case
				out.write(imageToARFF(im, loc, individualNorms, normalizeLevel1, normalizeFinal, sparse, 0));
			}
			ims = loc.getImageListWiki();
			int wikiIndex = 0;
			for (MEDI2014ImageBase im : ims) {
				try {
					out.write(imageToARFF(im, loc, individualNorms, normalizeLevel1, normalizeFinal, sparse,
							wikiIndex));
				} catch (Exception e) {
					System.err.println("Image: " + im.getImageFilename());
					e.printStackTrace();
					System.exit(1);
				}
				wikiIndex++;
			}
			// locIndex++;
		}
		out.close();
	}

	public String generateArffHeader() throws Exception {
		// generate the header of the arff file
		StringBuilder sb = new StringBuilder();
		sb.append("% Created by MakeARFFs.java\n");
		sb.append("@RELATION matches\n\n");
		sb.append("@ATTRIBUTE imageId\tNUMERIC\n");
		// sb.append("@ATTRIBUTE locationId\tNUMERIC\n");
		sb.append("@ATTRIBUTE locationId\t{");
		for (int i = 0; i < dataStore.getLocationList().size() - 1; i++) {
			sb.append(dataStore.getLocationList().get(i).getQueryId() + ",");
		}
		// for (int i = locationIdsStart; i <= locationIdsEnd; i++) { // use all location ids in header
		// sb.append(i + ",");
		// }
		sb.append(dataStore.getLocationList().get(dataStore.getLocationList().size() - 1).getQueryId() + "}");
		sb.append("\n");
		sb.append("@ATTRIBUTE flickrRank\tNUMERIC\n");

		// sb.append("@ATTRIBUTE latitude\tNUMERIC\n");
		// sb.append("@ATTRIBUTE longitude\tNUMERIC\n");

		// used to get the length of each feature type
		ArrayList<HashMap<Long, double[]>> features = dataStore.getLocationList().get(0).getFeatures();
		int counter = 0;
		for (String featureType : featureTypes) {
			// get the length of this feature type
			HashMap<Long, double[]> feature = features.get(counter);
			int featureLength = feature.values().iterator().next().length;
			totalLength += featureLength;
			for (int i = 0; i < featureLength; i++) {
				sb.append("@ATTRIBUTE " + featureType + "_" + i + "\tNUMERIC\n");
			}
			counter++;
		}

		sb.append("@ATTRIBUTE relevance \t{0,1}\n\n");
		sb.append("@DATA\n");
		return sb.toString();
	}

	/*
	 * Formats all visual attributes of one image to a ARFF-formatted string.
	 */
	private String imageToARFF(MEDI2014ImageBase im, MEDI2014Location loc, String[] individualNorms,
			String level1Norm, String finalNorm, boolean sparse, int wikiIndex) throws Exception {
		StringBuilder sb = new StringBuilder();

		int flickrRank = 0;
		if (im.getId() == -1) {// wikipedia image
			flickrRank = 0; // flickr rank is set to 0
		} else {
			flickrRank = ((MEDI2014Image) im).getRank();
		}

		int numDefaultFeatures = 3;

		if (!sparse) {
			sb.append(String.format("%d,%d,%d,", im.getId(), loc.getQueryId(), flickrRank));
			// arffLine += String.format("%5.5f,%5.5f,", im.getLatitude(), im.getLongitude());
		} else {
			sb.append(String.format("{0 %d,1 %d,2 %d,", im.getId(), loc.getQueryId(), flickrRank)); // first
																									// row is
																									// class
																									// label
		}

		ArrayList<Double> allFeaturesToNormalize = new ArrayList<Double>();
		ArrayList<Double> allOtherFeatures = new ArrayList<Double>();

		int featureIndex = 0;
		for (String featureType : featureTypes) {
			double[] feature;
			if (im.getId() == -1) { // a wiki image
				feature = loc.getFeaturesWiki().get(featureIndex).get(wikiIndex);
			} else {
				feature = loc.getFeatureVectors(im.getId())[featureIndex];
			}

			for (int i = 0; i < feature.length; i++) {
				if (Boolean.parseBoolean(individualNorms[featureIndex])) {
					allFeaturesToNormalize.add(feature[i]);
				} else {
					allOtherFeatures.add(feature[i]);
				}
			}
			featureIndex++;
		}

		double[] allfeaturesToNormalizeArray = new double[allFeaturesToNormalize.size()];
		for (int i = 0; i < allFeaturesToNormalize.size(); i++) {
			allfeaturesToNormalizeArray[i] = allFeaturesToNormalize.get(i);
		}
		// apply the appropriate normalization on the concatenated feature vector
		allfeaturesToNormalizeArray = Normalizations.normalize(allfeaturesToNormalizeArray, level1Norm);

		double[] allOtherFeaturesArray = new double[allOtherFeatures.size()];
		for (int i = 0; i < allOtherFeatures.size(); i++) {
			allOtherFeaturesArray[i] = allOtherFeatures.get(i);
		}

		double[] allfeaturesArray = ArrayUtils.addAll(allOtherFeaturesArray, allfeaturesToNormalizeArray);
		// apply the appropriate normalization on the final feature vector
		allfeaturesArray = Normalizations.normalize(allfeaturesArray, finalNorm);

		if (allfeaturesArray.length != totalLength) {
			throw new Exception("Something went wrong with image: " + im.getId() + " from location: "
					+ loc.getLocationName() + "\nExpected feature length was :" + totalLength + " but "
					+ allfeaturesArray.length + " was found!\nVector: " + Arrays.toString(allfeaturesArray));

		}

		if (!sparse) {
			for (int i = 0; i < allfeaturesArray.length; i++) {
				sb.append(allfeaturesArray[i] + ",");
			}

			// finally append relevance info!
			sb.append(loc.getRelevance(im) + "\n");
			/*
			 * long id; //Flickr id of photo (unique) float latitude; int license; //Creative Commons license
			 * type float longitude; int nbComments; //number of comments int rank; //rank in list retrieved
			 * from Flickr String tags; String title; String url_b; String username; int views; //number of
			 * times the photo has been displayed on Flickr //Fields containing given descriptors
			 * MEDIVisualAttributes visAttr; //Other fields String location; //name of landmark/location etc.
			 * int relevance; //ground truth relevance (1=relevant; 0=irrelevant; -1=unknown) int gtClusterId;
			 * //diversity: ground truth cluster number or -1 if irrelevant (or of unknown relevance) String
			 * imageFilename; //JPEG
			 */
		} else {
			for (int i = 0; i < allfeaturesArray.length; i++) {
				if (allfeaturesArray[i] != 0) {
					sb.append((i + numDefaultFeatures) + " " + allfeaturesArray[i] + ",");
				}
			}
			// finally append relevance info!
			sb.append((allfeaturesArray.length + numDefaultFeatures) + " " + loc.getRelevance(im) + "}\n");
		}
		return sb.toString();
	}

	/**
	 * @param args
	 *            [0] collection path<br>
	 *            [1] comma separated feature types e.g. "HOG,SURF,CM" <br>
	 *            [2] comma separated individual feature normalizations e.g. "l2,no,l2" <br>
	 *            [3] the type of normalization to apply on the concatenation of the features that are
	 *            individually normalized, e.g. "l2" or "no" <br>
	 *            [4] the type of normalization to apply on the overall vector, e.g. "l2" or "no" <br>
	 *            [5] whether to include Wikipedia image vectors <br>
	 *            [6] whether to write the dataset in sparse format<br>
	 *            [7] the filename extension (typically "dev" or "test")
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String collectionPath = args[0];
		String[] featureTypes = args[1].split(",");
		String[] individualNorms = args[2].split(",");
		if (featureTypes.length != individualNorms.length) {
			throw new Exception("Features types and normalizations have different sizes!");
		}
		String level1Norm = args[3];
		String finalNorm = args[4];
		boolean includeWiki = Boolean.parseBoolean(args[5]);
		boolean writeSparse = Boolean.parseBoolean(args[6]);
		String datasetExtension = args[7];

		MEDI2014Collection dataStoreTrain = new MEDI2014Collection(collectionPath);
		dataStoreTrain.loadAll(includeWiki, featureTypes, individualNorms);

		MakeRelevanceDataset marf = new MakeRelevanceDataset(dataStoreTrain, featureTypes);

		String arffFileName = Arrays.toString(featureTypes) + "-" + Arrays.toString(individualNorms) + "-"
				+ level1Norm + "-" + finalNorm;
		arffFileName += "-" + datasetExtension + ".arff";
		marf.writeFile(arffFileName, individualNorms, level1Norm, finalNorm, writeSparse);

	}
}
