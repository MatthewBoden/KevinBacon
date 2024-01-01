package ca.yorku.eecs;

// Goal of movieRecommendation class: Store the current top recommendation for the movieRecommendations method in Neo4JDatabase
public class movieRecommendation {

	// private attributes
	private String name;
	private String id;
	private int rating;
	
	// Empty Constructor
	public movieRecommendation()
	{
		this.name = null;
		this.id=null;
		this.rating = -1;
	}
	
	// Set methods for attributes
	public void setName(String name)
	{
		this.name = name;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	
	public void setRating(int rating)
	{
		this.rating = rating;
	}
	
	// Get methods for attributes
	public String getName()
	{
		return this.name;
	}
	public String getId()
	{
		return this.id;
	}
	
	public int getRating()
	{
		return this.rating;
	}
	
}
