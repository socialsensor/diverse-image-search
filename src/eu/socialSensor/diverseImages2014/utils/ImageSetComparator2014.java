package eu.socialSensor.diverseImages2014.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

/**
 * Comparator implementation.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 */
public class ImageSetComparator2014 implements Comparator {

	public HashSet<Long> getImageIds() {
		return imageIdsSet;
	}

	public void setImageIds(HashSet<Long> imageIds) {
		this.imageIdsSet = imageIds;
	}

	private ArrayList<Long> imageIdsArray;

	public ArrayList<Long> getImageIdsArray() {
		return imageIdsArray;
	}

	public void setImageIdsArray(ArrayList<Long> imageIdsArray) {
		this.imageIdsArray = imageIdsArray;
	}

	private HashSet<Long> imageIdsSet;

	private double rdScore;

	public ImageSetComparator2014() {
	}

	public ImageSetComparator2014(ArrayList<Long> imageIds) {
		this.imageIdsArray = imageIds;
		imageIdsSet = new HashSet<Long>(imageIds.size());
		for (Long id : imageIds) {
			imageIdsSet.add(id);
		}
	}

	public ImageSetComparator2014(ArrayList<Long> imageIds, double rdScore) {
		this.imageIdsArray = imageIds;
		imageIdsSet = new HashSet<Long>(imageIds.size());
		for (Long id : imageIds) {
			imageIdsSet.add(id);
		}
		this.rdScore = rdScore;
	}

	public double getRdScore() {
		return rdScore;
	}

	public void setRdScore(double rdScore) {
		this.rdScore = rdScore;
	}

	public int compare(Object o1, Object o2) {
		ImageSetComparator2014 im1 = (ImageSetComparator2014) o1;
		ImageSetComparator2014 im2 = (ImageSetComparator2014) o2;
		if (im1.getRdScore() > im2.getRdScore()) {
			return 1;
		} else if (im1.getRdScore() == im2.getRdScore()) {
			return 0;
		} else {
			return -1;
		}
	}

	public String toString() {
		String out = rdScore + " : ";
		for (Long id : imageIdsArray) {
			out += id + " ";
		}
		return out;
	}

}
