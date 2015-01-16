package eu.socialSensor.diverseImages2014;

/**
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public abstract class MEDI2014ImageBase extends AbstractDiversificationObject {

	/** root directory of the image's collection */
	protected String rootDir;
	/** name of the image's landmark/location etc. */
	protected String locationName;
	/** filename of the image */
	protected String imageFilename;

	/** MEDI2014 features for this image */
	protected MEDI2014Features features;

	public MEDI2014ImageBase(long id, String rootDir, String locationName) {
		super(id);
		this.rootDir = rootDir;
		this.locationName = locationName;
	}

	public String getImageFilename() {
		return imageFilename;
	}

	public String getLocationName() {
		return locationName;
	}

	public MEDI2014Features getFeatures() {
		return features;
	}

}
