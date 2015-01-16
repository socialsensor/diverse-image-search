package eu.socialSensor.diverseImages2014;

/**
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public abstract class AbstractDiversificationObject {

	/**
	 * The id of this object
	 */
	protected long id;

	/**
	 * The predicted relevance of this object
	 */
	protected double relevanceScore;

	public AbstractDiversificationObject(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public double getRelevanceScore() {
		return relevanceScore;
	}

	public void setRelevanceScore(double relevanceScore) {
		this.relevanceScore = relevanceScore;
	}

}
