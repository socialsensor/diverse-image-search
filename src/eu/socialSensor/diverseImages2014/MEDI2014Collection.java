package eu.socialSensor.diverseImages2014;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Loads and stores an entire collection of images and metadata.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * 
 */
public class MEDI2014Collection {

	private String rootDir;

	public String getRootDir() {
		return rootDir;
	}

	private ArrayList<MEDI2014Location> locationList;

	public MEDI2014Collection(String newDir) {
		setRootDir(newDir);
		locationList = new ArrayList<MEDI2014Location>();
	}

	public ArrayList<MEDI2014Location> getLocationList() {
		return locationList;
	}

	public void addLocation(MEDI2014Location location) {
		locationList.add(location);
	}

	/**
	 * 
	 * Read all locations from master XML topics file
	 * 
	 * @param newDir
	 * @param topicsFile
	 * @throws Exception
	 */
	public void loadAll(boolean loadWiki, String[] featureTypes, String[] normalizations) throws Exception {
		loadAll(Integer.MAX_VALUE, loadWiki, featureTypes, normalizations);
	}

	/**
	 * Read some (or all) locations from master XML topics file. Limit number of locations to read for faster
	 * run-time during development.
	 * 
	 * This method basically loads the following info for each location (in the given order):
	 * <ol>
	 * <li>Info from the master xml file (queryId, locationName, lat-long, wikiUrl)</li>
	 * <li>Image (textual) metadata from each location's xml file (date_taken, description, lat-long, licence,
	 * nbComments, rank, tags, title, username, views, imageFilename). Selected visual attributes are also
	 * loaded for each image.</li>
	 * <li>Selected visual attributes for the Wikipedia images of the location.</li>
	 * <li>Ground truth info for each location, if this is a development collection.</li>
	 * </ol>
	 * 
	 * @param newDir
	 * @param topicsFile
	 * @param maxToRead
	 * @throws Exception
	 */
	public void loadAll(int maxToRead, boolean loadWiki, String[] featureTypes, String[] normalizations)
			throws Exception {
		// the original topics of devset and testset collections were both rename to topics.xml for simplicity
		String topicsFile = "topics.xml";
		int numRead = 0;

		// reading the master xml file for the collection
		File fXmlFile = new File(rootDir + topicsFile);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName("topic");
		System.out.println("Reading " + nList.getLength() + " location(s) from "
				+ fXmlFile.getCanonicalPath());

		for (int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				// create location object
				MEDI2014Location loc = new MEDI2014Location(eElement, rootDir, featureTypes, normalizations);
				// System.out.println("Loading flickr images..");
				loc.loadImagesFromXML(); // load each image's metadata
				loc.loadImageFeatures();
				// System.out.println("Loading wiki images..");
				if (loadWiki) {
					loc.loadWikiImages();
					loc.loadImageFeaturesWiki(); // wikiImages are basically their feature vectors!!!
				}
				// after the end of the contest, we have gt for the test set as well!!!
				// if (collectionType == collectionTypes.dev) { // only devset collection has ground truth
				loc.loadGT();
				// }
				System.out.println((i + 1) + ") " + loc.toString());
				locationList.add(loc);
				numRead++;
				if (numRead >= maxToRead) {
					System.out.println("Stopped after " + numRead + " locations.");
					break; // import just a subset of locations, for faster debugging etc.
				}
			}
		}
		System.out.println("Loaded " + locationList.size() + " location(s).");
	}

	private void setRootDir(String newDir) {
		if (!newDir.endsWith(File.separator)) {
			newDir = newDir + File.separator;
		}
		this.rootDir = newDir;
	}

}
