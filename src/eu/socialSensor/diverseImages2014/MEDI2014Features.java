package eu.socialSensor.diverseImages2014;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import eu.socialSensor.diverseImages2014.utils.Normalizations;

/**
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class MEDI2014Features {

	/** name of this class used in reflection */
	public static String CLASS_NAME = "MEDI2014Features";

	protected double[] CM; // Global Colour Moments
	protected double[] CN; // Global Colour Naming Histogram
	protected double[] CSD; // Global Colour Structure Descriptor
	protected double[] GLRLM; // Global Statistics on Gray Level Run Length Matrix
	protected double[] HOG; // Global Histogram of Oriented Gradients
	protected double[] LBP; // Global Locally Binary Patterns
	protected double[] CM3x3; // Spatial pyramid representation of above
	protected double[] CN3x3; // Spatial pyramid representation of above
	protected double[] GLRLM3x3; // Spatial pyramid representation of above
	protected double[] LBP3x3; // Spatial pyramid representation of above
	protected double[] CNN;
	protected double[] VLAD;

	// textual
	protected double[] FRANK;
	protected double[] FRANKN;
	protected double[] VIEWS;
	protected double[] VIEWSN;
	protected double[] COMMENTS;
	protected double[] COMMENTSN;
	protected double[] DISTANCE;
	protected double[] DISTANCEN;
	protected double[] GEO;
	protected double[] BOW10K;

	/**
	 * Parse a CSV file of a named class of attributes (name must match a field of this object)
	 * 
	 * @throws Exception
	 * @throws
	 */
	public static HashMap<Long, double[]> parseFile(String rootDir, String locationName, String featureName,
			String normalization) throws Exception {
		HashMap<Long, double[]> allImagesFeatures = new HashMap<Long, double[]>();
		String filename = rootDir + "descvis" + File.separator + "img" + File.separator + locationName;
		if (!featureName.startsWith("_")) {// add a " " before the feature name
			filename += " " + featureName + ".csv";
		} else {
			filename += featureName + ".csv";
		}

		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = null;
		double[] attrVals = null;
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(",");
			long id = Long.parseLong(parts[0]);// the first element is always the image id
			attrVals = new double[parts.length - 1];
			for (int i = 1; i < parts.length; i++) {
				double d = Double.parseDouble(parts[i]);
				attrVals[i - 1] = d;
			}

			// apply the appropriate normalization if needed
			attrVals = Normalizations.normalize(attrVals, normalization);

			allImagesFeatures.put(id, attrVals);
		}
		reader.close();
		return allImagesFeatures;
	}

	public static ArrayList<double[]> parseFileWiki(String rootDir, String locationName, String featureName,
			String normalization) throws Exception {
		ArrayList<double[]> allImagesFeatures = new ArrayList<double[]>();
		String filename = rootDir + "descvis" + File.separator + "imgwiki" + File.separator + locationName;
		if (!featureName.startsWith("_")) {// add a " " in the feature name
			filename += " " + featureName + ".csv";
		} else {
			filename += featureName + ".csv";
		}

		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = null;
		double[] attrVals = null;
		while ((line = reader.readLine()) != null) {
			// if the wiki image name contains a comma, the 2nd element will not be a number!
			// count the number of leading not a number parts
			String[] parts = line.split(",");
			int partsToSkip = 0;
			if (parts[0].equals("wiki")) { // this is a Wikipedia image indicator
				partsToSkip = 1;
			} else { // count parts to skip
				for (int i = 0; i < parts.length; i++) {
					try {
						Double.parseDouble(parts[i]);
					} catch (NumberFormatException e) {
						partsToSkip++;
						continue;
					}
					break;
				}
			}
			// the partsToSkip first elements compose the image id
			// long id = Long.parseLong(parts[0]+parts[1]+...+parts[partsToSkip-1]);
			// truncate the parts array to keep only the numeric elements
			parts = Arrays.copyOfRange(parts, partsToSkip, parts.length);

			attrVals = new double[parts.length];
			for (int i = 0; i < parts.length; i++) {
				double d = Double.parseDouble(parts[i]);
				attrVals[i] = d;
			}

			// apply the appropriate normalization if needed
			attrVals = Normalizations.normalize(attrVals, normalization);

			allImagesFeatures.add(attrVals);
		}
		reader.close();
		return allImagesFeatures;
	}
}
