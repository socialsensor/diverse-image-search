package eu.socialSensor.diverseImages2014.eval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

/**
 * This class represents a query location and its ground truth.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 */
public class Medi2014LocationEval {

	/**
	 * Location type: keyword or keyword-gps location.<br>
	 * Only one location type (keyword) in 2014 locations.
	 */
	public static enum locationTypes {
		keyword, keywordGPS
	}

	/**
	 * The id of this location
	 */
	private int locationId;

	/**
	 * The name of this location
	 */
	private String locationName;

	/**
	 * The type of this location (keyword or keywordGPS)
	 */
	private locationTypes locationType;

	/**
	 * The Wikipedia page url for this location
	 */
	private String wikiUrl;

	/**
	 * The Wikipedia images for this location
	 */
	private String[] wikiImages;

	public String[] getWikiImages() {
		return wikiImages;
	}

	public String getWikiPage() {
		return wikiUrl;
	}

	public void setWikiUrl(String wikiUrl) {
		this.wikiUrl = wikiUrl;
	}

	private double latitude;

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	private double longitude;

	/**
	 * Relevant images for this location along with their corresponding cluster ids.
	 */
	private HashMap<String, Integer> relevantImages;

	/**
	 * Irrelevant images for this location (cluster id of all irrelevant images is set to -1).
	 */
	private HashMap<String, Integer> irrelevantImages;

	/**
	 * Don't know images for this location (cluster id of all don't know images is set to -1).
	 */
	private HashMap<String, Integer> dontKnowImages;

	/**
	 * The distinct clusters for this location (clusters ids and descriptions).
	 */
	private TreeMap<Integer, String> clusters;

	/**
	 * Map holding cluster ids and images of each cluster. Used in visualization.
	 */
	private TreeMap<Integer, ArrayList<String>> clusterMembers;

	/**
	 * The images of this locations in the order returned by flickr.
	 */
	private ArrayList<String> flickrOrdered;

	/**
	 * CR@X
	 */
	private double CRatX = 0;
	/**
	 * P@X
	 */
	private double PatX = 0;
	/**
	 * F1@X
	 */
	private double F1atX = 0;

	public Medi2014LocationEval(int id, String name) {
		this.locationId = id;
		this.locationName = name;
		relevantImages = new HashMap<String, Integer>();
		irrelevantImages = new HashMap<String, Integer>();
		dontKnowImages = new HashMap<String, Integer>();
		clusters = new TreeMap<Integer, String>();
		clusterMembers = new TreeMap<Integer, ArrayList<String>>();
		flickrOrdered = new ArrayList<String>();
	}

	/**
	 * Calculates the measures given an ordered list of images and a specific cut-off.
	 * 
	 * @param ids
	 *            Ids of the images
	 */
	public void caclulateMeasures(ArrayList<String> ids, int cutoff) {
		resetMeasures();
		HashSet<Integer> distinctClusters = new HashSet<Integer>();
		int limit = Math.min(cutoff, ids.size());
		for (int i = 0; i < limit; i++) {
			int clusterId = -1;
			if (relevantImages.containsKey(ids.get(i))) {
				clusterId = relevantImages.get(ids.get(i));
				PatX++;
				distinctClusters.add(clusterId);
			}
		}
		CRatX = distinctClusters.size();
		PatX /= (double) cutoff;
		CRatX /= (double) clusters.size();

		if (CRatX + PatX > 0) {
			F1atX = (2 * CRatX * PatX) / (CRatX + PatX);
		} else {
			F1atX = 0;
		}
	}

	public double getCRatX() {
		return CRatX;
	}

	public double getF1atX() {
		return F1atX;
	}

	public int getId() {
		return locationId;
	}

	/**
	 * Returns a random permutation of all images from this location.
	 * 
	 * @return
	 */
	public ArrayList<String> getRandomPermutation(int seed) {
		ArrayList<String> rankedList = new ArrayList<String>();
		for (Entry<String, Integer> entry : relevantImages.entrySet()) {
			rankedList.add(entry.getKey());
		}
		for (Entry<String, Integer> entry : irrelevantImages.entrySet()) {
			rankedList.add(entry.getKey());
		}
		Collections.shuffle(rankedList, new Random(seed));
		return rankedList;
	}

	/**
	 * Returns an ordered list of images ids which are as diverse and relevant as possible.
	 * 
	 * @return
	 */
	public ArrayList<String> getIdeal() {
		ArrayList<String> orderedList = new ArrayList<String>();
		// iteratively add one image from each cluster until all relevant images have been added
		int clusterImageIndex = 0;
		while (orderedList.size() < relevantImages.size()) {
			for (ArrayList<String> clusterImages : clusterMembers.values()) {
				if (clusterImages.size() > clusterImageIndex) {
					orderedList.add(clusterImages.get(clusterImageIndex));
				}
			}
			clusterImageIndex++;
		}
		// add all irrelevant images at the bottom of the list
		for (String imageId : irrelevantImages.keySet()) {
			orderedList.add(imageId);
		}

		return orderedList;
	}

	/**
	 * Gets the images in the order returned by flickr.
	 * 
	 * @return
	 */
	public ArrayList<String> getFlickrOrdered() {
		return flickrOrdered;
	}

	public HashMap<String, Integer> getIrrelevantImages() {
		return irrelevantImages;
	}

	public HashMap<String, Integer> getRelevantImages() {
		return relevantImages;
	}

	public HashMap<String, Integer> getDontKnowImages() {
		return dontKnowImages;
	}

	public String getName() {
		return locationName;
	}

	public int getNumClusters() {
		return clusters.size();
	}

	public int getNumIrrelevant() {
		return irrelevantImages.size();
	}

	public int getNumDontKnow() {
		return dontKnowImages.size();
	}

	public int getNumRelevant() {
		return relevantImages.size();
	}

	public double getPatX() {
		return PatX;
	}

	public locationTypes getType() {
		return locationType;
	}

	/**
	 * Loads info from the DclusterGT file.
	 * 
	 * @param fileName
	 * @throws Exception
	 */
	public void loadDclusterGT(String fileName) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));
		String line;
		while ((line = in.readLine()) != null) {
			int clusterId = Integer.parseInt(line.split(",")[0]);
			String clusterName = line.split(",")[1];
			clusters.put(clusterId, clusterName);
		}
		in.close();
	}

	/**
	 * Loads info from the DGT file.
	 * 
	 * @param fileName
	 * @throws Exception
	 */
	public void loadDGT(String fileName) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));
		String line;
		int prevClusterId = 1;
		ArrayList<String> images = new ArrayList<String>();
		while ((line = in.readLine()) != null) {
			String name = line.split(",")[0];
			int clusterId = Integer.parseInt(line.split(",")[1]);
			if (prevClusterId != clusterId) {// new cluster
				clusterMembers.put(prevClusterId, images);
				// update-initialize
				images = new ArrayList<String>();
				prevClusterId = clusterId;
			}
			if (!clusters.containsKey(clusterId)) {
				throw new Exception("Cluster id does not appear in corresponding dclusterGT.txt");
			}
			Integer existing = relevantImages.put(name, clusterId);
			if (existing != null) {
				System.out.println("Image " + name + " is assigned in multiple clusters: " + existing
						+ " and " + clusterId);
				System.out.println("The last assignment is kept.");
			}
			images.add(name);
		}

		clusterMembers.put(prevClusterId, images);

		in.close();
	}

	/**
	 * Loads info from the rGT file.
	 * 
	 * @param fileName
	 * @throws Exception
	 */
	public void loadRGT(String fileName) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));
		String line;
		while ((line = in.readLine()) != null) {
			String name = line.split(",")[0];
			String rel = line.split(",")[1];
			if (rel.equals("0")) {
				irrelevantImages.put(name, -1);
			}
			if (rel.equals("-1")) {
				dontKnowImages.put(name, -1);
			}
		}
		in.close();
	}

	/**
	 * Loads flickr order from the xml file.
	 * 
	 * @param fileName
	 * @throws Exception
	 */
	public void loadFlickrOrder(String fileName) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));
		String line;
		while ((line = in.readLine()) != null) {
			if (line.contains("id=")) {
				String id = line.split("id=\"")[1].split("\"")[0];
				flickrOrdered.add(id);
			}
		}
		in.close();
	}

	/**
	 * Loads Wikipedia images for this location
	 * 
	 * @param wikiDir
	 * @throws Exception
	 */
	public void loadWikiImages(String wikiDirPath) throws Exception {
		File wikiDir = new File(wikiDirPath);
		String[] wikiNames = wikiDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jpg") || name.endsWith(".jpeg");
			}
		});
		wikiImages = wikiNames;
	}

	private void resetMeasures() {
		PatX = 0;
		CRatX = 0;
		F1atX = 0;
	}

	/**
	 * Prints info about the location in CSV format.
	 * 
	 * @return
	 */
	public String toCsv() {
		String csvLine = locationId + "," + locationType + "," + locationName + "," + getNumRelevant() + ","
				+ getNumIrrelevant() + "," + getNumClusters();
		for (Map.Entry<Integer, String> entry : clusters.entrySet()) {
			String cName = entry.getValue();
			csvLine += "," + cName;
		}
		return csvLine;
	}

	/**
	 * Creates an html page that visualizes the ground truth for this location.
	 * 
	 * @throws Exception
	 */
	public void toHtml(String gtFolder) throws Exception {

		System.out.println("Creating gt html page for location: " + locationName);
		String html = "";
		html += "<html><body bgcolor=\"#E6E6FA\"><div>";
		html += "<div style=\"margin:15px; background:yellow;\"><h1><a href=\"" + wikiUrl + "\">"
				+ locationName + "</a>:";
		html += " # Relavant images: " + getNumRelevant();
		html += " | # Irrelavant images: " + getNumIrrelevant();
		html += " | # Clusters: " + clusters.size();
		html += "</h1></div>";

		// show the Wikipedia images for this location!
		html += "<div style=\"background:blue; margin-top:15px; padding:3px; margin-left:15px; float:left;\">";
		html += "<h2>Wikipedia Images:</h2>";
		for (String wikiImage : wikiImages) {
			html += "<a href=\"../imgWiki/" + locationName + "/" + wikiImage
					+ "\"><img height=\"150px\" width=\"150px\"  src=\"../imgWiki/" + locationName + "/"
					+ wikiImage + "\"></a>";
		}
		html += "</div>";

		int counterAll = 0;
		for (Map.Entry<Integer, ArrayList<String>> e : clusterMembers.entrySet()) {
			if (counterAll == 10) {
				counterAll = 0;
			}
			html += "<div style=\"background:green; margin-top:15px; padding:3px; margin-left:15px; float:left;\">";
			html += "<div style=\"height:20px; color:white;\"><div style=\"font-weight:900;\" align=\"center\">"
					+ clusters.get(e.getKey()) + " (" + e.getValue().size() + " images)" + "</div></div>";
			int counter = 0;
			for (String im : e.getValue()) {
				if (counter >= 3) {
					break;
				}
				// if (counter >= 3) {
				// if (counterAll < 8) {
				// break;
				// } else {
				// if (counterAll == 10) {
				// break;
				// }
				// }
				// }
				html += "<div align=\"center\" style=\"float:left; margin:2px;\"><a href=\"../img/"
						+ locationName + "/" + im + ".jpg"
						+ "\"><img height=\"150px\" width=\"150px\"  src=\"../img/" + locationName + "/" + im
						+ ".jpg" + "\"></a></div>";
				counter++;
				counterAll++;
			}
			html += "</div>";
		}

		// display irrelevant images

		html += "<div style=\"background:red; margin-top:15px; padding:3px; margin-left:15px; float:left;\">";
		html += "<div style=\"height:20px; color:white;\"><div style=\"font-weight:900;\" align=\"center\">Irrelevant"
				+ " (" + irrelevantImages.size() + " images)" + "</div></div>";
		for (String im : irrelevantImages.keySet()) {
			html += "<div style=\"float:left; margin:2px;\"><a href=\"../img/" + locationName + "/" + im
					+ ".jpg" + "\"><img width=\"150px\" height=\"150px\" src=\"../img/" + locationName + "/"
					+ im + ".jpg" + "\"></a></div>";
		}
		html += "</div>";

		html += "</div></body></html>";
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(gtFolder + "/htmlGT/" + locationName
				+ ".html")));
		out.write(html);
		out.close();
	}

	/**
	 * Print the CSV header.
	 */
	public static void printCsvHeader() {
		System.out.println("loc_id,loc_type,loc_name,num_rel,num_irrel,num_clusters,cluster_names");
	}
}
