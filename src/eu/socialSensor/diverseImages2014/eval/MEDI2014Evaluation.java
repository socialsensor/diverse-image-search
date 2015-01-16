package eu.socialSensor.diverseImages2014.eval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.aliasi.util.BoundedPriorityQueue;

import eu.socialSensor.diverseImages2014.MEDI2014Collection;
import eu.socialSensor.diverseImages2014.Medi2014ConfigurationUtil;
import eu.socialSensor.diverseImages2014.utils.MediImageComparator2014;

/**
 * This class contains methods for performing evaluation, generating benchmark submission files and
 * visualizing the groung truth.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 */
public class MEDI2014Evaluation {

	public static final int officialCutoff2014 = 20;
	public static final int maxPerLocation = 300;

	/**
	 * This method contains example code to:<br>
	 * <ol>
	 * <li>Load the ground truth and print some statistics related to the locations.</li>
	 * <li>Generate benchmark submission files (random, flickr, ideal).</li>
	 * <li>Evaluate a submission file against the ground truth and print the evaluation measures.</li>
	 * <li>Generates an html visualization of the ground truth.</li>
	 * </ol>
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String collectionPath = Medi2014ConfigurationUtil.getTestset();
		String submissionsPath = "submission_files/relevance-ordered/";

		// 1. Loading ground truth for all locations and printing statistics
		boolean printCSVstats = true;
		boolean printPerLocationResults = false;
		TreeMap<Integer, Medi2014LocationEval> locationsGT = loadGroundTruth(collectionPath, printCSVstats);

		// 2. Generating benchmark submission files and evaluating them
		int numIterations = 1;

		String submissionType = "ideal";

		for (int i = 0; i < numIterations; i++) {
			int seed = i; // changing seed at each iteration if the generation method is randomizable
			String submissionFileDev = submissionsPath + "/submission_test_" + submissionType + ".txt";
			generateSubmissionFile(submissionFileDev, locationsGT, null, submissionType, seed);
			evaluate(submissionFileDev, locationsGT, printPerLocationResults, officialCutoff2014);
		}

		// generateHtmls(locationsDev, devPath);

	}

	/**
	 * This method takes as argument the full path to the MediaEval2014 root folder, loads the ground truth in
	 * an ArrayList and returns it. Statistics can be optionally printed.
	 * 
	 * @param mediaEval2014Collection
	 *            Full path to the folder where the devset is extracted.
	 * @param testset
	 *            Whether testset ground truth is to be loaded.
	 * @param printStatsInCSV
	 *            Whether to print stats in csv format.
	 * @return
	 * @throws Exception
	 */
	public static TreeMap<Integer, Medi2014LocationEval> loadGroundTruth(String mediaEval2014Collection,
			boolean printStatsInCSV) throws Exception {
		String topics = mediaEval2014Collection + "/topics.xml";

		double avgNumClusters = 0;
		double avgNumRelevant = 0;
		double avgNumIrrelevant = 0;
		double avgNumDontKnow = 0;

		if (printStatsInCSV) {
			Medi2014LocationEval.printCsvHeader();
		}

		ArrayList<Medi2014LocationEval> locs = new ArrayList<Medi2014LocationEval>();
		TreeMap<Integer, Medi2014LocationEval> locations = new TreeMap<Integer, Medi2014LocationEval>();
		String line;

		BufferedReader in = new BufferedReader(new FileReader(new File(topics)));
		while ((line = in.readLine()) != null) {
			if (line.contains("<number>")) {
				int qid = Integer.parseInt(line.split("<number>")[1].split("</")[0]);
				String locationName = in.readLine().split("<title>")[1].split("</")[0];
				Medi2014LocationEval loc = new Medi2014LocationEval(qid, locationName);
				loc.loadRGT(mediaEval2014Collection + "/gt/rGT/" + locationName + " rGT.txt");
				loc.loadDclusterGT(mediaEval2014Collection + "/gt/dGT/" + locationName + " dclusterGT.txt");
				loc.loadDGT(mediaEval2014Collection + "/gt/dGT/" + locationName + " dGT.txt");
				loc.loadFlickrOrder(mediaEval2014Collection + "/xml/" + locationName + ".xml");

				// in.readLine(); // skip latitude line
				// in.readLine(); // skip longitude line
				double latitude = Double.parseDouble(in.readLine().split("<latitude>")[1].split("</")[0]);
				double longitude = Double.parseDouble(in.readLine().split("<longitude>")[1].split("</")[0]);
				loc.setLatitude(latitude);
				loc.setLongitude(longitude);

				// load the Wikipedia page link for this location
				String wikipage = in.readLine().split("<wiki>")[1].split("</")[0];
				loc.setWikiUrl(wikipage);

				// load the Wikipedia images for this location
				loc.loadWikiImages(mediaEval2014Collection + "/imgWiki/" + locationName);

				// check if the folder for this location contains the same number of image files!
				File dir = new File(mediaEval2014Collection + "/img/" + locationName);
				String[] imageFiles = dir.list(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".jpg") || name.endsWith(".jpeg");
					}
				});
				if (imageFiles.length != (loc.getNumRelevant() + loc.getNumIrrelevant() + loc
						.getNumDontKnow())) {
					System.err.println("Problem at location " + locationName + "\nGT: "
							+ (loc.getNumRelevant() + loc.getNumIrrelevant()) + "\nFiles: "
							+ imageFiles.length);
				}

				if (printStatsInCSV) {
					System.out.println(loc.toCsv());
				}
				avgNumClusters += loc.getNumClusters();
				avgNumRelevant += loc.getNumRelevant();
				avgNumIrrelevant += loc.getNumIrrelevant();
				avgNumDontKnow += loc.getNumDontKnow();
				locations.put(qid, loc);
			}
		}
		in.close();

		System.out.println("Avg num relevant: " + avgNumRelevant / locations.size());
		System.out.println("Avg num irrelevant: " + avgNumIrrelevant / locations.size());
		System.out.println("Avg num dont know : " + avgNumDontKnow / locations.size());
		System.out.println("Avg num clusters: " + avgNumClusters / locations.size());

		return locations;
	}

	/**
	 * * This method can generate benchmark submission files.
	 * 
	 * @param filename
	 *            Full path to the submission file that will be generated.
	 * @param locations
	 *            The ground truth.
	 * @param type
	 *            Type of submission file to generate. Available types are: random, ideal, flickr
	 * @param maxPerLocation
	 *            Maximum number of results per location.
	 * @param seed
	 *            Seed for random number generation (only used for random type).
	 * @throws Exception
	 */
	public static void generateSubmissionFile(String filename,
			TreeMap<Integer, Medi2014LocationEval> locations, MEDI2014Collection collection, String type,
			int seed) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(filename)));
		int locationIndex = 0;
		for (Map.Entry<Integer, Medi2014LocationEval> entry : locations.entrySet()) {
			Medi2014LocationEval locationGT = entry.getValue();
			// System.out.println("Generating submission file for location: " + loc.getName());
			int qid = entry.getKey();
			ArrayList<String> results = null;
			BoundedPriorityQueue<MediImageComparator2014> bpq = null;
			if (type.equals("ideal")) {
				results = locationGT.getIdeal();
			} else if (type.equals("random")) {
				results = locationGT.getRandomPermutation(seed);
			} else if (type.equals("flickr")) {
				results = locationGT.getFlickrOrdered();
			} else {
				throw new Exception("Wrong submission file type given!");
			}

			int limit = 0;
			if (bpq == null) {
				limit = Math.min(results.size(), maxPerLocation);
			} else {
				limit = Math.min(bpq.size(), maxPerLocation);
			}
			for (int j = 0; j < limit; j++) {
				if (bpq == null) {
					out.write(qid + " 0 " + results.get(j) + " " + j + " " + (double) (limit - j) / (limit)
							+ " " + type + "_run\n");
				} else {
					MediImageComparator2014 image = bpq.poll();
					String imageId = image.getImageId();
					double relevanceScore = image.getRelevanceScore();

					out.write(qid + " 0 " + imageId + " " + j + " " + relevanceScore + " " + type + "_run\n");
				}
			}
			locationIndex++;
		}
		out.close();
	}

	/**
	 * This method takes a submission file and the loaded ground truth as inputs and prints the evaluation
	 * measures in the console.
	 * 
	 * @param submissionFile
	 *            Full path to a submission file in the MediaEval2013/2014 format.
	 * @param locations
	 *            A Map of MediLocation2014 objects (the ground truth).
	 * @param printPerLocationResults
	 *            If true, measures per location will be printed.
	 * @param cutoff
	 *            The cutoff at which measures will me calculated.
	 * @return
	 * @throws Exception
	 */
	public static double[] evaluate(String submissionFile, TreeMap<Integer, Medi2014LocationEval> locations,
			boolean printPerLocationResults, int cutoff) throws Exception {
		double mCRatX = 0;
		double mPatX = 0;
		double mF1atX = 0;

		int numQueriesSubmitted = 0;

		BufferedReader in = new BufferedReader(new FileReader(new File(submissionFile)));
		String line;
		int prevqid = 1;
		ArrayList<String> rankedList = new ArrayList<String>();
		while ((line = in.readLine()) != null) {
			int qid = Integer.parseInt(line.split(" ")[0]);
			String photoid = line.split(" ")[2];
			if (qid != prevqid && rankedList.size() > 0) { // new query
				// evaluate the previous query
				if (!locations.containsKey(prevqid)) {
					throw new Exception("This qid does not exist in the ground truth!");
				}
				Medi2014LocationEval loc = locations.get(prevqid);
				if (loc.getNumRelevant() > 0) {
					numQueriesSubmitted++;
					loc.caclulateMeasures(rankedList, cutoff);
					if (printPerLocationResults) {
						System.out.println(loc.getName() + " " + loc.getCRatX() + " " + loc.getPatX() + " "
								+ loc.getF1atX());
					}

					mCRatX += loc.getCRatX();
					mPatX += loc.getPatX();
					mF1atX += loc.getF1atX();
					// System.out.println("Cluster distribution: " +
					// loc.getClusterDistributionAtX().toString());
				} else {
					System.out.println("Skipping location with 0 relevant images: " + loc.getId());
				}
				// reinitialize
				rankedList = new ArrayList<String>();
			}
			// in any case
			rankedList.add(photoid);
			prevqid = qid;
		}
		in.close();

		// evaluate the last query
		if (!locations.containsKey(prevqid)) {
			throw new Exception("This qid does not exist in the ground truth!");
		}
		Medi2014LocationEval loc = locations.get(prevqid);
		if (loc.getNumRelevant() > 0) {
			numQueriesSubmitted++;

			loc.caclulateMeasures(rankedList, cutoff);

			if (printPerLocationResults) {
				System.out.println(loc.getName() + " " + loc.getCRatX() + " " + loc.getPatX() + " "
						+ loc.getF1atX());
			}

			mCRatX += loc.getCRatX();
			mPatX += loc.getPatX();
			mF1atX += loc.getF1atX();

		} else {
			System.out.println("Skipping location with 0 relevant images: " + loc.getId());
		}

		mPatX /= numQueriesSubmitted;
		mCRatX /= numQueriesSubmitted;
		mF1atX /= numQueriesSubmitted;

		System.out.println(mCRatX + " " + mPatX + " " + mF1atX);

		double[] results = { mPatX, mCRatX, mF1atX };
		return results;

	}

	/**
	 * This method can be used to generate an html page that visualizes the ground truth for each location and
	 * an index page that provides links to all locations.
	 * 
	 * @param locations
	 *            A Map containing MediLocation2014 objects (the ground truth).
	 * @param medievalPath
	 *            Full path to the folder of a Mediaeval dataset.
	 * @throws Exception
	 */
	public static void generateHtmls(TreeMap<Integer, Medi2014LocationEval> locations, String medievalPath)
			throws Exception {
		// Generate an index page for the locations
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(medievalPath + "/index-gt.html")));
		out.write("<html><body><h1>Keyword Locations</h1><ul>");

		// Create the folders where the individual pages will be written
		File file = new File(medievalPath + "/htmlGT/");
		if (!file.exists()) {
			if (file.mkdir()) {
				System.out.println("Directory is created!");
			} else {
				System.out.println("Failed to create directory!");
			}
		}

		for (Map.Entry<Integer, Medi2014LocationEval> entry : locations.entrySet()) {
			Medi2014LocationEval loc = entry.getValue();
			loc.toHtml(medievalPath);
			out.write(loc.getId() + " <a href=\"htmlGT/" + loc.getName() + ".html" + "\">" + loc.getName()
					+ "</a><br>");

		}

		out.write("</ul></body></html>");
		out.close();
	}
}
