package eu.socialSensor.diverseImages2014;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Element;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

/**
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class MEDI2014Image extends MEDI2014ImageBase {

	// The following fields are filled from location XML file: (e.g. "xml\Abbey of Saint Gall.xml")
	private int fRank; // rank in list retrieved from Flickr

	private double latitude;
	private double longitude;

	private String title;
	private String description;
	private String tags;

	private int numViews; // number of times the photo has been displayed on Flickr
	private int numComments;
	private int license; // Creative Commons license type

	private Date date_taken;
	private String url_b;
	private String username;

	/** for parsing dates. E.g. date_taken="2009-02-21 16:43:18" */
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public MEDI2014Image(Element eElement, String rootDir, String locationName) throws Exception {
		super(Long.parseLong(eElement.getAttribute("id")), rootDir, locationName);

		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		date_taken = sdf.parse(eElement.getAttribute("date_taken"));
		description = eElement.getAttribute("description");
		latitude = Float.parseFloat(eElement.getAttribute("latitude"));
		license = Integer.parseInt(eElement.getAttribute("license"));
		longitude = Float.parseFloat(eElement.getAttribute("longitude"));
		numComments = Integer.parseInt(eElement.getAttribute("nbComments"));
		fRank = Integer.parseInt(eElement.getAttribute("rank"));
		tags = eElement.getAttribute("tags");
		title = eElement.getAttribute("title");
		url_b = eElement.getAttribute("url_b");
		username = eElement.getAttribute("username");
		numViews = Integer.parseInt(eElement.getAttribute("views"));
		imageFilename = this.id + ".jpg";
	}

	/**
	 * Calculates and returns the distance (in meters) of this image from the given location.
	 * 
	 * @return The distance in meters, or -1000 if the location of this image is unknown.
	 * @throws Exception
	 */
	public double distanceFromLocation(double locLat, double locLong) throws Exception {
		if (this.latitude == 0 || this.longitude == 0) { // images with no gps have 0s in the xml file
			// System.out.println("No coordinates for image " + this.getId());
			return -1000; // unknown image location!
		}
		LatLng loc = new LatLng(locLat, locLong);
		LatLng img = new LatLng(latitude, longitude);

		double distance = LatLngTool.distance(loc, img, LengthUnit.METER);
		if (distance < 0) {
			throw new Exception("Negative distance!");
		}
		return distance;
	}

	public Date getDate_taken() {
		return date_taken;
	}

	public String getDescription() {
		return description;
	}

	public long getId() {
		return id;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public int getLicense() {
		return license;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public int getNbComments() {
		return numComments;
	}

	public int getRank() {
		return fRank;
	}

	public String getTags() {
		return tags;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl_b() {
		return url_b;
	}

	public String getUsername() {
		return username;
	}

	public int getViews() {
		return numViews;
	}

}