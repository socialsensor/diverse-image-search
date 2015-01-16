package eu.socialSensor.diverseImages2014.utils;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

/**
 * 
 * @author Eleftherios Spyromitros-Xioufis
 */
public class Distances {

	private static double euclideanDistance(double[] vec1, double[] vec2) {
		return Math.sqrt(euclideanDistanceSquared(vec1, vec2));
	}

	private static double euclideanDistanceSquared(double[] vec1, double[] vec2) {
		double distance = 0;
		for (int i = 0; i < vec1.length; i++) {
			distance += (vec1[i] - vec2[i]) * (vec1[i] - vec2[i]);
		}
		return distance;
	}

	private static double innerProductDistance(double[] vec1, double[] vec2) {
		double innerp = 0;
		for (int i = 0; i < vec1.length; i++) {
			innerp += vec1[i] * vec2[i];
		}
		return 1 - innerp;
	}

	public static double cosineDistance(double[] vec1, double[] vec2) {
		double innerp = 0;
		double norm1 = 0;
		double norm2 = 0;
		for (int i = 0; i < vec1.length; i++) {
			innerp += vec1[i] * vec2[i];
			norm1 += vec1[i] * vec1[i];
			norm2 += vec2[i] * vec2[i];
		}
		return 1 - (innerp / (Math.sqrt(norm1) * Math.sqrt(norm2)));
	}

	private static double rankDistance(double[] rank1, double[] rank2) {
		return Math.abs(rank1[0] - rank2[0]) / (double) 300;
	}

	private static double diffDistance(double[] score1, double[] score2) {
		return Math.abs(score1[0] - score2[0]);
	}

	private static double rankAverage(double[] score1, double[] score2) {
		return (score1[0] + score2[0]) / (2.0 * 300);
	}

	private static double diffSquaredDistance(double[] score1, double[] score2) {
		return Math.abs(score1[0] - score2[0]) * Math.abs(score1[0] - score2[0]);
	}

	private static double geoDistance(double[] geo1, double[] geo2) throws Exception {
		if (geo1[0] == 0 || geo2[0] == 0) { // images with no gps have 0s in the xml file
			return -1000; // unknown distance
		}

		LatLng im1 = new LatLng(geo1[0], geo1[1]);
		LatLng im2 = new LatLng(geo2[0], geo2[1]);

		double distance = LatLngTool.distance(im1, im2, LengthUnit.KILOMETER);
		if (distance < 0) {
			throw new Exception("Negative distance!");
		}
		return distance;
	}

	public static double computeDistance(double[] fVec1, double[] fVec2, String dfunc) throws Exception {
		if (dfunc.equals("innerp")) {
			return innerProductDistance(fVec1, fVec2);
		} else if (dfunc.equals("cosine")) {
			return cosineDistance(fVec1, fVec2);
		} else if (dfunc.equals("euclidean")) {
			return euclideanDistance(fVec1, fVec2);
		} else if (dfunc.equals("geo")) {
			return geoDistance(fVec1, fVec2);
		} else if (dfunc.equals("rank")) {
			return rankDistance(fVec1, fVec2);
		} else if (dfunc.equals("diff")) {
			return diffDistance(fVec1, fVec2);
		} else if (dfunc.equals("diff2")) {
			return diffSquaredDistance(fVec1, fVec2);
		} else if (dfunc.equals("avg")) {
			return rankAverage(fVec1, fVec2);
		} else {
			throw new Exception("Unknown distance function!");
		}
	}
}
