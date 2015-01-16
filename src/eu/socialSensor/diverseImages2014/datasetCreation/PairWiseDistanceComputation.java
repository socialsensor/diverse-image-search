package eu.socialSensor.diverseImages2014.datasetCreation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import eu.socialSensor.diverseImages2014.MEDI2014Collection;
import eu.socialSensor.diverseImages2014.MEDI2014Location;
import eu.socialSensor.diverseImages2014.utils.Distances;

/**
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class PairWiseDistanceComputation {

	public static void main(String args[]) throws Exception {
		// parse input arguments
		String collectionPath = args[0];
		String[] features = args[1].split(",");
		String[] norms = args[2].split(",");
		String[] dfuncs = args[3].split(",");
		boolean random = Boolean.parseBoolean(args[4]);// if true, random distances are created
		Random rand = null;
		String rangesString = args[5];
		double[] ranges = new double[4]; // min and max range for probs of the two classes
		if (random) {
			rand = new Random(1); // /initialize the random number generator
			for (int i = 0; i < ranges.length; i++) {
				ranges[i] = Double.parseDouble(rangesString.split("_")[i]);
			}
		}

		// load the collection
		MEDI2014Collection collection = new MEDI2014Collection(collectionPath);
		boolean loadWiki = false; // features for the Wikipedia images will probably not be needed
		if (!random) {
			collection.loadAll(loadWiki, features, norms);
		} else {
			collection.loadAll(loadWiki, new String[0], new String[0]);
		}

		ArrayList<MEDI2014Location> locations = collection.getLocationList();
		// for each location
		for (MEDI2014Location location : locations) {
			// generate all distinct image pairs for this location
			HashSet<HashSet<Long>> pairs = generateAllDistinctPairs(location.getAllImageIds());
			// compute distance between each pair for each type of feature
			for (int i = 0; i < features.length; i++) {
				String featureType;
				if (!random) {
					featureType = features[i] + "-" + norms[i] + "-" + dfuncs[i];
				} else {
					featureType = "random" + "_" + rangesString + "-" + norms[i] + "-" + dfuncs[i];
				}
				// create a file where all pairwise distances for this location and this feature will be
				// written
				BufferedWriter out = new BufferedWriter(new FileWriter(new File(collectionPath
						+ "/distances/" + featureType + "_" + +location.getQueryId() + ".txt")));
				out.write(featureType + "\n");

				for (HashSet<Long> pair : pairs) {
					if (pair.size() != 2) {
						throw new Exception("Pair should have exactly 2 elements!");
					}
					double distance;

					long[] ids = new long[2];
					int index = 0;
					for (long id : pair) {
						ids[index] = id;
						index++;
					}

					index = 0;
					if (!random) {
						double[][] vecs = new double[2][];
						for (long id : pair) {
							vecs[index] = location.getFeatureVectors(id)[i];
							index++;
						}
						distance = Distances.computeDistance(vecs[0], vecs[1], dfuncs[i]);
					} else {
						// check if the images belong to the same cluster
						int clusterId1 = location.getClusterId(ids[0]);
						int clusterId2 = location.getClusterId(ids[1]);
						if (clusterId1 == clusterId2) {
							distance = ranges[0] + rand.nextDouble() * (ranges[1] - ranges[0]);
						} else {
							distance = ranges[2] + rand.nextDouble() * (ranges[3] - ranges[2]);
						}
					}
					out.write(ids[0] + "-" + ids[1] + "," + distance + "\n");
				}
				out.close();
			}
		}
	}

	/**
	 * Returns an ArrayList where each element corresponds to a HashMap that contains all pairwise distances
	 * for a specific feature for all pairs of the given location.
	 * 
	 * @param collectionPath
	 * @param features
	 * @param norms
	 * @param dfuncs
	 * @param locationId
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<HashMap<HashSet<Long>, Double>> readPairwiseDistances(String collectionPath,
			String[] features, String[] norms, String[] dfuncs, int locationId) throws Exception {
		ArrayList<HashMap<HashSet<Long>, Double>> pairWiseDistancesAllFeatures = new ArrayList<HashMap<HashSet<Long>, Double>>(
				features.length);
		String distancesFolder = collectionPath + "/distances/";
		for (int i = 0; i < features.length; i++) {
			HashMap<HashSet<Long>, Double> pairWiseDistancesThisFeature = new HashMap<HashSet<Long>, Double>(
					44850);
			String featureType = features[i] + "-" + norms[i] + "-" + dfuncs[i];
			BufferedReader in = new BufferedReader(new FileReader(new File(distancesFolder + featureType
					+ "_" + locationId + ".txt")));
			String line = in.readLine();// eat first line - check if the file is the correct one
			if (!featureType.equals(line)) {
				throw new Exception("Wrong feature type!");
			}
			while ((line = in.readLine()) != null) {
				String pairString = line.split(",")[0];
				long id1 = Long.parseLong(pairString.split("-")[0]);
				long id2 = Long.parseLong(pairString.split("-")[1]);
				HashSet<Long> pair = new HashSet<Long>(2);
				pair.add(id1);
				pair.add(id2);
				double distance = Double.parseDouble(line.split(",")[1]);
				pairWiseDistancesThisFeature.put(pair, distance);
			}
			pairWiseDistancesAllFeatures.add(pairWiseDistancesThisFeature);
			in.close();
		}
		return pairWiseDistancesAllFeatures;
	}

	public static HashSet<HashSet<Long>> generateAllDistinctPairs(long[] ids) {
		HashSet<HashSet<Long>> distinctPairs = new HashSet<HashSet<Long>>();
		for (int i = 0; i < ids.length; i++) {
			for (int j = i + 1; j < ids.length; j++) {
				HashSet<Long> pair = new HashSet<Long>();
				pair.add(ids[i]);
				pair.add(ids[j]);
				distinctPairs.add(pair);
			}
		}
		return distinctPairs;
	}
}
