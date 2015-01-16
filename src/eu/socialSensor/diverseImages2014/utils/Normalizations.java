package eu.socialSensor.diverseImages2014.utils;

import java.util.Arrays;

/**
 * This class contains vector normalization methods.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * 
 */
public class Normalizations {

	/**
	 * This method applies L2 normalization on a given array of doubles. The passed vector is modified by the
	 * method.
	 * 
	 * @param vector
	 *            the original vector
	 * @return the L2 normalized vector
	 */
	private static double[] normalizeL2(double[] vector) {
		// compute vector 2-norm
		double norm2 = 0;
		for (int i = 0; i < vector.length; i++) {
			norm2 += vector[i] * vector[i];
		}
		norm2 = (double) Math.sqrt(norm2);

		if (norm2 == 0) {
			Arrays.fill(vector, 1);
		} else {
			for (int i = 0; i < vector.length; i++) {
				vector[i] = vector[i] / norm2;
			}
		}
		return vector;
	}

	/**
	 * This method applies L1 normalization on a given array of doubles. The passed vector is modified by the
	 * method.
	 * 
	 * @param vector
	 *            the original vector
	 * @return the L1 normalized vector
	 */
	private static double[] normalizeL1(double[] vector) {
		// compute vector 1-norm
		double norm1 = 0;
		for (int i = 0; i < vector.length; i++) {
			norm1 += Math.abs(vector[i]);
		}

		if (norm1 == 0) {
			Arrays.fill(vector, 1.0 / vector.length);
		} else {
			for (int i = 0; i < vector.length; i++) {
				vector[i] = vector[i] / norm1;
			}
		}
		return vector;
	}

	public static double[] normalize(double[] vector, String normalization) throws Exception {
		if (normalization.equals("no")) {
			return vector;
		} else if (normalization.equals("l2")) {
			return normalizeL2(vector);
		} else if (normalization.equals("l1")) {
			return normalizeL1(vector);
		} else {
			throw new Exception("Unknown normalization type!");
		}
	}
}
