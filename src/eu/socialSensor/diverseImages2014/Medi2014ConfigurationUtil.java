package eu.socialSensor.diverseImages2014;

import java.io.File;
import java.util.ResourceBundle;

/**
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class Medi2014ConfigurationUtil {
	private static ResourceBundle rb;

	static {
		rb = ResourceBundle.getBundle("resources" + File.separator + "mediaeval-settings2014");
	}

	public static String getRoot() {
		return rb.getString("mediaEval2014Root");
	}

	public static String getDevset() {
		return getRoot() + rb.getString("mediaEval2014Devset");
	}

	public static String getTestset() {
		return getRoot() + rb.getString("mediaEval2014Testset");
	}

	public static String getSubmissionsDevset() {
		return getRoot() + rb.getString("mediaEval2014SubmissionsDevset");
	}

	public static String getSubmissionsTestset() {
		return getRoot() + rb.getString("mediaEval2014SubmissionsTestset");
	}
}
