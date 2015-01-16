package eu.socialSensor.diverseImages2014.utils;

import java.util.Comparator;

/**
 * Comparator implementation.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 */
public class MediImageComparator2014 implements Comparator {

	private String imageId;

	private int locationId;

	private int flickrRank;

	private int imageIndexInTestSet;

	public int getImageIndexInTestSet() {
		return imageIndexInTestSet;
	}

	public void setImageIndexInTestSet(int imageIndexInTestSet) {
		this.imageIndexInTestSet = imageIndexInTestSet;
	}

	private double relevanceRank;

	private double diversityRank;

	private double combinedRank;

	private double relevanceScore;

	private double diversityScore;

	private double combinedScore;

	public MediImageComparator2014() {
	}

	public MediImageComparator2014(String imageId, int locationId, double relevanceScore) {
		this.imageId = imageId;
		this.locationId = locationId;
		this.relevanceScore = relevanceScore;
	}

	public MediImageComparator2014(String imageId, int locaitonId, int flickrRank, double relevanceScore) {
		this(imageId, locaitonId, relevanceScore);
		this.flickrRank = flickrRank;
	}

	public MediImageComparator2014(String imageId, int locaitonId, int flickrRank, int imageIndexInTestSet,
			double relevanceScore) {
		this(imageId, locaitonId, flickrRank, relevanceScore);
		this.imageIndexInTestSet = imageIndexInTestSet;
	}

	public int compare(Object o1, Object o2) {
		MediImageComparator2014 im1 = (MediImageComparator2014) o1;
		MediImageComparator2014 im2 = (MediImageComparator2014) o2;
		if (im1.getRelevanceScore() > im2.getRelevanceScore()) {
			return 1;
		} else if (im1.getRelevanceScore() < im2.getRelevanceScore()) {
			return -1;
		} else {
			if (im1.getFlickrRank() < im2.getFlickrRank()) {
				return 1;
			} else if (im1.getFlickrRank() > im2.getFlickrRank()) {
				return -1;
			} else {
				return 0;
			}
		}
	}

	public double getCombinedRank() {
		return combinedRank;
	}

	public double getCombinedScore() {
		return combinedScore;
	}

	public double getDiversityRank() {
		return diversityRank;
	}

	public double getDiversityScore() {
		return diversityScore;
	}

	public String getImageId() {
		return imageId;
	}

	public int getFlickrRank() {
		return flickrRank;
	}

	public int getLocationId() {
		return locationId;
	}

	public double getRelevanceRank() {
		return relevanceRank;
	}

	public double getRelevanceScore() {
		return relevanceScore;
	}

	public void setCombinedRank(double combinedRank) {
		this.combinedRank = combinedRank;
	}

	public void setCombinedScore(double combinedScore) {
		this.combinedScore = combinedScore;
	}

	public void setDiversityRank(double diversityRank) {
		this.diversityRank = diversityRank;
	}

	public void setDiversityScore(double diversityScore) {
		this.diversityScore = diversityScore;
	}

	public void setRelevanceRank(double relevanceRank) {
		this.relevanceRank = relevanceRank;
	}

	public void setRelevanceScore(double relevanceScore) {
		this.relevanceScore = relevanceScore;
	}

	public String toString() {
		return imageId + ":" + relevanceScore;
	}
}
