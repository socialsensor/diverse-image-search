package eu.socialSensor.diverseImages2014;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class represents a query location and its ground truth.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 */
public class MEDI2014Location {

	/**
	 * The additional list of image found in the wikipedia page of this location.
	 */
	private ArrayList<MEDI2014ImageBase> additionalWikiImageList;

	/**
	 * Map holding cluster ids and images of each cluster. Used in visualization.
	 */
	private TreeMap<Integer, ArrayList<String>> clusterMembers;

	/**
	 * The distinct clusters for this location (clusters ids and descriptions).
	 */
	private TreeMap<Integer, String> clusters;

	/**
	 * Don't know images for this location (cluster id of all don't know images is set to -1).
	 */
	private HashMap<String, Integer> dontKnowImages;

	/**
	 * The fist ArrayList enumerates the feature types and the second enumerates the Wikipedia images.
	 */
	private ArrayList<ArrayList<double[]>> featuresWiki;

	/**
	 * The set of features that will be loaded for this location.
	 */
	private String[] featureTypes;

	/**
	 * Each element of the ArrayList is a HashMap that stores the fVecs of all images for a certain feature
	 * type.
	 */
	private ArrayList<HashMap<Long, double[]>> fVecs;

	private HashMap<Long, Integer> imageIdToArraylistPosition;

	/**
	 * The list of images for this location, ordered as they appear in the xml file of this location,
	 * essentially the Flickr order.
	 */
	private ArrayList<MEDI2014ImageBase> imageList;
	/**
	 * The list of wikipedia images given by the organizers for this location.
	 */
	private ArrayList<MEDI2014ImageBase> imageListWiki;

	/**
	 * Irrelevant images for this location (cluster id of all irrelevant images is set to -1).
	 */
	private HashMap<String, Integer> irrelevantImages;

	private double latitude;

	/**
	 * The name of this location
	 */
	private String locationName;

	private double longitude;

	/**
	 * The type of normalization to apply on each feature.
	 */
	private String[] norms;

	/**
	 * The query id of this location
	 */
	private int queryId;

	private HashMap<Long, Double> relevances;

	/**
	 * Relevant images for this location along with their corresponding cluster ids.
	 */
	private HashMap<String, Integer> relevantImages;

	private String rootDir;

	/**
	 * The text from the Wikipedia page for this location.
	 */
	private String wikiText;

	/**
	 * The Wikipedia page url for this location
	 */
	private String wikiUrl;

	public MEDI2014Location(Element eElement, String rootDir, String[] featureTypes, String[] normalizations)
			throws Exception {
		if (featureTypes.length != normalizations.length) {
			throw new Exception("Number of features should be equal to the number of normalizations!");
		}
		this.rootDir = rootDir;
		this.featureTypes = featureTypes; // these feature types will be loaded
		this.norms = normalizations;
		queryId = Integer.parseInt(eElement.getElementsByTagName("number").item(0).getTextContent());
		locationName = eElement.getElementsByTagName("title").item(0).getTextContent();
		latitude = Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent());
		longitude = Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent());
		wikiUrl = eElement.getElementsByTagName("wiki").item(0).getTextContent();

		importTextFromWIki();
		imageList = new ArrayList<MEDI2014ImageBase>();
		imageListWiki = new ArrayList<MEDI2014ImageBase>();
		relevantImages = new HashMap<String, Integer>();
		irrelevantImages = new HashMap<String, Integer>();
		dontKnowImages = new HashMap<String, Integer>();
		clusters = new TreeMap<Integer, String>();
		clusterMembers = new TreeMap<Integer, ArrayList<String>>();
		imageIdToArraylistPosition = new HashMap<Long, Integer>();
	}

	/**
	 * Correct some formatting in Wikipedia URL
	 */
	private String fixUrl(String wikiURL) {
		wikiURL = wikiURL.replaceAll("%25", "%");
		wikiURL = wikiURL.replaceAll("%28", "(");
		wikiURL = wikiURL.replaceAll("%29", ")");
		// fix issue with "Liberty Hall (Crawfordville, Georgia)
		wikiURL = wikiURL.replaceAll("Crawfordville._Georgia", "Crawfordville,_Georgia");

		return wikiURL;
	}

	public double getAvgComments() {
		double avgComments = 0;
		for (int i = 0; i < imageList.size(); i++) {
			avgComments += ((MEDI2014Image) imageList.get(i)).getNbComments();
		}
		return avgComments / imageList.size();
	}

	public double getAvgDistance() throws Exception {
		double avgDistance = 0;
		for (int i = 0; i < imageList.size(); i++) {
			avgDistance += ((MEDI2014Image) imageList.get(i)).distanceFromLocation(latitude, longitude);
		}
		return avgDistance / imageList.size();
	}

	public double getAvgViews() {
		double avgViews = 0;
		for (int i = 0; i < imageList.size(); i++) {
			avgViews += ((MEDI2014Image) imageList.get(i)).getViews();
		}
		return avgViews / imageList.size();
	}

	public TreeMap<Integer, ArrayList<String>> getClusterMembers() {
		return clusterMembers;
	}

	public HashMap<String, Integer> getDontKnowImages() {
		return dontKnowImages;
	}

	public ArrayList<HashMap<Long, double[]>> getFeatures() {
		return fVecs;
	}

	public ArrayList<ArrayList<double[]>> getFeaturesWiki() {
		return featuresWiki;
	}

	/**
	 * Returns the feature vectors of the image with the given id. Features are ordered as in the constructor.
	 * 
	 * @param id
	 * @return
	 */
	public double[][] getFeatureVectors(long id) {
		double fvecs[][] = new double[fVecs.size()][];
		for (int i = 0; i < fVecs.size(); i++) {
			fvecs[i] = fVecs.get(i).get(id);
		}
		return fvecs;
	}

	public MEDI2014ImageBase getImage(long imageId) {
		int pos = imageIdToArraylistPosition.get(imageId);
		return imageList.get(pos);
	}

	public HashMap<Long, Integer> getImageIdToArraylistPosition() {
		return imageIdToArraylistPosition;
	}

	public ArrayList<MEDI2014ImageBase> getImageList() {
		return imageList;
	}

	public ArrayList<MEDI2014ImageBase> getImageListWiki() {
		return imageListWiki;
	}

	public HashMap<String, Integer> getIrrelevantImages() {
		return irrelevantImages;
	}

	public double getLatitude() {
		return latitude;
	}

	public String getLocationName() {
		return locationName;
	}

	public double getLongitude() {
		return longitude;
	}

	public int getMaxComments() {
		int maxComments = 0;
		for (int i = 0; i < imageList.size(); i++) {
			int comments = ((MEDI2014Image) imageList.get(i)).getNbComments();
			if (comments > maxComments) {
				maxComments = comments;
			}
		}
		return maxComments;
	}

	public int getMaxViews() {
		int maxViews = 0;
		for (int i = 0; i < imageList.size(); i++) {
			int views = ((MEDI2014Image) imageList.get(i)).getViews();
			if (views > maxViews) {
				maxViews = views;
			}
		}
		return maxViews;
	}

	/**
	 * Open & parse HTTP stream from Wikipedia. Automatically follows "redirect" instructions from Wikipedia.
	 */
	private String getPageFromWiki(String wikiURL) {
		URL url;
		InputStream is = null;
		BufferedReader br;
		String line;
		String contents = "";
		wikiURL = fixUrl(wikiURL);

		try {
			// Properties systemProperties = System.getProperties();
			// systemProperties.setProperty("http.proxyHost", "proxy.rgu.ac.uk");
			// systemProperties.setProperty("http.proxyPort", "8080");
			System.out.println("Reading from wikiurl " + wikiURL);
			url = new URL(wikiURL);
			System.out.println("Reading from url " + url);
			is = url.openStream(); // throws an IOException
			br = new BufferedReader(new InputStreamReader(is));

			while ((line = br.readLine()) != null) {
				// System.out.println(line);
				contents += line;
			}
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException ioe) {
			}
		}

		// Follow redirects:
		String redirectPattern = "#redirect \\[\\[(\\S.+?)\\]\\]";
		Pattern p1 = Pattern.compile(redirectPattern, Pattern.CASE_INSENSITIVE);
		Matcher m1 = p1.matcher(contents);
		if (m1.find()) {
			String redirTo = m1.group(1);
			redirTo = redirTo.replaceAll(" ", "_");
			System.out.println("Redirecting to " + redirTo);
			wikiURL = "http://en.wikipedia.org/w/api.php?format=json&action=query&prop=revisions&rvprop=content&titles="
					+ redirTo;
			contents = getPageFromWiki(wikiURL); // call this method again with the new URL
		}

		return contents;
	}

	public int getQueryId() {
		return queryId;
	}

	/**
	 * Returns the relevance of this image as a String.
	 * 
	 * @param im
	 *            the image in question
	 * @return "?"/"1"/"0" depending on whether the relevance of the image is unknown/1/0
	 */
	public String getRelevance(MEDI2014ImageBase im) {
		if (im.getId() == -1) {
			return "1";
		}
		if (relevantImages.size() == 0) {// 0 relevant images = a testset location!
			return "?";
		}
		if (relevantImages.containsKey(im.getImageFilename())) {
			return "1";
		} else {
			return "0";
		}
	}

	public HashMap<String, Integer> getRelevantImages() {
		return relevantImages;
	}

	public int getClusterId(long imageId) {
		String imageName = imageId + ".jpg";
		if (relevantImages.containsKey(imageName)) {
			return relevantImages.get(imageName);
		} else {
			return -1;
		}
	}

	public String getRootDir() {
		return rootDir;
	}

	public String getWikiText() {
		return wikiText;
	}

	/**
	 * Accesses Wikipedia API to get a single page. Downloads then tidies up text (to remove some mark-up).
	 * Stores text in MEDILocation.wikiText.
	 * 
	 * @throws FileNotFoundException
	 */
	public void importTextFromWIki() throws Exception {
		// first check whether the text from Wikipedia has already been imported in written in a file
		// if yes, parse the text from the file
		// otherwise, download the text from Wikipedia and write it in the file
		String wikiFileName = rootDir + "txtwiki" + File.separator + locationName + ".txt";
		File f = new File(wikiFileName);
		if (f.exists() && !f.isDirectory()) { /* do something */
			BufferedReader in = new BufferedReader(new FileReader(new File(wikiFileName)));
			// String line = "";
			// while ((line = in.readLine()) != null) {
			// wikiText += line;
			// }
			// wikiText += line;
			wikiText = in.readLine();
			in.close();
			// wikiText = CreateTxtFiles.tidyWikiText(wikiText);

			// BufferedWriter out = new BufferedWriter(new FileWriter(new File(wikiFileName.replace(".txt",
			// "-tidy.txt"))));
			// out.write(wikiText);
			// out.close();
		} else {
			// Example: http://en.wikipedia.org/wiki/Colosseum
			// Some urls start with "http instead of http, " should be removed!
			if (wikiUrl.charAt(0) == '"') {
				wikiUrl = wikiUrl.substring(1, wikiUrl.length());
			}

			String wikiRoot = wikiUrl.substring(0, wikiUrl.indexOf(".org/") + 5); // e.g. en.wikipedia or
																					// de.wikipeda
																					// etc.
			String wikiTitle = wikiUrl.substring(wikiUrl.lastIndexOf("/") + 1); // title of target page
			// System.out.println(wikiTitle);

			String wikiAPIurl = wikiRoot
					+ "w/api.php?format=json&action=query&prop=revisions&rvprop=content&titles=" + wikiTitle; // form
																												// API
																												// call

			wikiText = getPageFromWiki(wikiAPIurl);

			BufferedWriter out = new BufferedWriter(new FileWriter(new File(wikiFileName)));
			out.write(wikiText);
			out.close();
		}
	}

	/**
	 * Returns the cluster id of the given image or -1 if the image is irrelevant
	 * 
	 * @param imageName
	 * @return
	 */
	public int isRelevant(String imageName) {
		int clusterId;
		relevantImages.get(imageName);
		if (relevantImages.containsKey(imageName)) {
			clusterId = relevantImages.get(imageName);
		} else {
			clusterId = -1;
		}
		return clusterId;
	}

	/**
	 * Loads info from the DclusterGT file.
	 * 
	 * @throws Exception
	 */
	private void loadDclusterGT() throws Exception {
		String gtFilename = rootDir + "gt" + File.separator + "dGT" + File.separator + locationName
				+ " dclusterGT.txt";
		BufferedReader in = new BufferedReader(new FileReader(gtFilename));
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
	 * @throws Exception
	 */
	private void loadDGT() throws Exception {
		String gtFilename = rootDir + "gt" + File.separator + "dGT" + File.separator + locationName
				+ " dGT.txt";
		BufferedReader in = new BufferedReader(new FileReader(gtFilename));
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
			relevantImages.put(name + ".jpg", clusterId);
			images.add(name);
		}

		clusterMembers.put(prevClusterId, images);

		in.close();
	}

	/**
	 * Methods that loads all the info contained in the ground truth files into corresponding structures. It
	 * calls a different method for each ground truth file!
	 */
	public void loadGT() throws Exception {
		loadDclusterGT();
		loadDGT();
		loadRGT();
	}

	public void loadImageFeatures() throws Exception {
		fVecs = new ArrayList<HashMap<Long, double[]>>();
		for (int i = 0; i < featureTypes.length; i++) {
			HashMap<Long, double[]> theseFeatures = MEDI2014Features.parseFile(rootDir, locationName,
					featureTypes[i], norms[i]);
			fVecs.add(theseFeatures);
		}
	}

	public void loadImageFeaturesWiki() throws Exception {
		featuresWiki = new ArrayList<ArrayList<double[]>>();
		for (int i = 0; i < featureTypes.length; i++) {
			ArrayList<double[]> theseFeaturesWiki = MEDI2014Features.parseFileWiki(rootDir, locationName,
					featureTypes[i], norms[i]);
			while (theseFeaturesWiki.size() < imageListWiki.size()) { // non enough features loaded
				theseFeaturesWiki.add(theseFeaturesWiki.get(0)); // add the first feature multiple times
			}
			featuresWiki.add(theseFeaturesWiki);
		}
	}

	// public void loadWikiPage() throws Exception {
	// imageListWiki.add(new MEDIImage2014Wiki(rootDir, locationName, "wiki"));
	// }

	/**
	 * Reads xml file listing all images from one location. Returns a list of newly created image objects.
	 */
	public void loadImagesFromXML() {
		try {
			File fXmlFile = new File(rootDir + "xml" + File.separator + locationName + ".xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			// optional, but recommended read this -
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("photo");
			System.out
					.println("Reading " + nList.getLength() + " photos from " + fXmlFile.getCanonicalPath());

			// SANITY CHECK: add all image files in the corresponding directory into a HashSet
			HashSet<String> imageFilesForLocation = new HashSet<String>();
			Path dir = Paths.get(rootDir + "img" + File.separator + locationName);
			DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{jpg,jpeg}");
			for (Path entry : stream) {
				imageFilesForLocation.add(entry.getFileName().toString());
			}

			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					MEDI2014Image im = new MEDI2014Image(eElement, rootDir, locationName);
					// SANITY CHECK: check if the image file of this xml element exists in the corresponding
					// directory
					if (!imageFilesForLocation.contains(im.getImageFilename())) {
						throw new Exception("Sanity check failed!");
					}
					imageList.add(im);
					imageIdToArraylistPosition.put(im.getId(), i);
					if (i % 100 == 0) {// debug
						// System.out.println("Loaded image " + i + " " + im.getImageFilename());
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads info from the rGT file.
	 * 
	 * @throws Exception
	 */
	private void loadRGT() throws Exception {
		String gtFilename = rootDir + "gt" + File.separator + "rGT" + File.separator + locationName
				+ " rGT.txt";
		BufferedReader in = new BufferedReader(new FileReader(gtFilename));
		String line;
		while ((line = in.readLine()) != null) {
			String name = line.split(",")[0];
			String rel = line.split(",")[1];
			if (rel.equals("0")) {
				irrelevantImages.put(name + ".jpg", -1);
			}
			if (rel.equals("-1")) {
				dontKnowImages.put(name + ".jpg", -1);
			}
		}
		in.close();
	}

	public void loadWikiImages() throws Exception {
		Path dir = Paths.get(rootDir + File.separator + "imgwiki" + File.separator + locationName
				+ File.separator);
		DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{jpeg,jpg,png}");
		for (Path entry : stream) {
			// remove the extension
			String imageFilename = entry.getFileName().toString().toLowerCase().split("\\.jp")[0];
			imageFilename = imageFilename.replaceAll("\\P{InBasic_Latin}", "");// remove non latin characters
			imageListWiki.add(new MEDI2014ImageWiki(rootDir, locationName));
		}
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public void setQueryId(int queryId) {
		this.queryId = queryId;
	}

	public void setRootDir(String rootDir) {
		this.rootDir = rootDir;
	}

	public void setWikiText(String wikiText) {
		this.wikiText = wikiText;
	}

	public long[] getAllImageIds() {
		long[] imageIDs = new long[imageList.size()];
		for (int i = 0; i < imageList.size(); i++) {
			imageIDs[i] = imageList.get(i).getId();
		}
		return imageIDs;
	}
}
