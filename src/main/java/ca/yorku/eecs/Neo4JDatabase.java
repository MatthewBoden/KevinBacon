package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;
import org.neo4j.driver.v1.*;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.*;


// Goal of Neo4JDatabase class: perform functions outlined in provided API with associated Neo4J database.

public class Neo4JDatabase {

	private Driver driver;
	private String uriDb;
	private Map<String, List<String>> actorsVisited;
	private Map<String, Boolean> moviesVisited;
	private Map<String, Bacon> bacon;
	private List<String> actorDatabase;
	private List<String> movieDatabase;
	private HelperMethods helper;
	
	/* 
	 * Kevin Bacon ALWAYS has
	 * actorId: nm0000102
	 */

	private final String kevinId = "nm0000102";
	
	// Initialize NEO4J connections and bacon, helper objects
	public Neo4JDatabase() {
		uriDb = "bolt://localhost:7687";
		Config config = Config.builder().withoutEncryption().build();
		driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","12345678"), config);
		
		bacon = new HashMap<String, Bacon>();
		helper = new HelperMethods();
	}
	
	// Function: Add actorId to database
	public void addActor(HttpExchange request) throws IOException, JSONException
	{
		// Parse JSON file and place name, actorId in Map
		String query = Utils.convert(request.getRequestBody());	
		query = "name" + query;
		Map<String, String> queryParam = Utils.splitBody(query);
		
    	String name = queryParam.get("name");
        String id = queryParam.get("actorId");
               
        // Initialize JSONObject
        JSONObject result = new JSONObject();
        if (id == null) // Determine if id is null
        {
        	result.append("Error", "BAD REQUEST");
			Utils.sendString(request, (result.getString("Error")).toString(), 400);
        }
        else		// Perform regex pattern checking. Reject if incompatible
        {
        	Pattern p = Pattern.compile("nm\\d{7}");
        	Matcher m = p.matcher(id);
        	if (name == null || !m.matches())
        	{
        		result.append("Error", "BAD REQUEST");
    			Utils.sendString(request, (result.getString("Error")).toString(), 400);

        	}
        	else 
        	{
        		// Initialize session for Neo4J Connection
        		try (Session session = driver.session()) { 
        			if (!helper.checkActorId(id, session))	// Check if actorId exists in database. Reject if present
        			{
        				// Communicate with Neo4J and create actor node with name, actorId
        				session.writeTransaction(tx -> tx.run("CREATE (a:actor {name:$x, actorId:$y});",	
        						parameters("x", name, "y", id)));
        				session.close();

        				if(id.equals(kevinId)) 	// if id = nm0000102 (Kevin Bacon), assign Kevin Bacon data and attach to bacon Map(SHOULD ALWAYS BE PRESENT)
        				{
        					int baconNumber = 0;
        					List <String> baconPath = new ArrayList<String>();
        					baconPath.add(kevinId);
        					bacon.put(kevinId, new Bacon(baconNumber, baconPath));	
        				}
        				
        				// Send success signal to client
        				result.append("No Error", "OK");
            			Utils.sendString(request, (result.getString("No Error")).toString(), 200);
        			}
        			else 
        			{
        				// Send actor exists signal to client
        				result.append("Error", "ACTOR EXISTS");
            			Utils.sendString(request, (result.getString("Error")).toString(), 404);
        			}
        			session.close();
        		}
        	}
        }

	}	
	
	// Function: add movieId to database
	public void addMovie(HttpExchange request) throws IOException, JSONException
	{
		// Parse JSON file and place name, movieId in Map
		String query = Utils.convert(request.getRequestBody());
		query = "name" + query;
		Map<String, String> queryParam = Utils.splitBody(query);
		
    	String name = queryParam.get("name");
        String id = queryParam.get("movieId");
        
        // Initialize JSONObject
        JSONObject result = new JSONObject();
        if (id == null)	// Determine if id is null
        {
        	result.append("Error", "BAD REQUEST");
			Utils.sendString(request, (result.getString("Error")).toString(), 400);
        }
        else	// Perform regex pattern checking. Reject if incompatible
        {
        	Pattern p = Pattern.compile("nm\\d{7}|null");
        	Matcher m = p.matcher(id);
        	if (name.equals(null) || !m.matches())
        	{
        		result.append("Error", "BAD REQUEST");
    			Utils.sendString(request, (result.getString("Error")).toString(), 400);
        	}
        	else 
        	{
        		// Initialize session for Neo4J Connection
        		try (Session session = driver.session()) {            
        			if (!helper.checkMovieId(id, session))	// Check if movieId exists in database. Reject if present
        			{
        				// Communicate with Neo4J and create movie node with name, movieId
        				session.writeTransaction(tx ->tx.run("CREATE (m: movie{name:$x, movieId:$y})",	
        						parameters("x", name, "y", id)));
        				session.close();
        				
        				// Send success signal to client
        				result.append("No Error", "OK");
            			Utils.sendString(request, (result.getString("No Error")).toString(), 200);
        			}
        			else 
        			{
        				// Send movie exists signal to client
        				result.append("Error", "MOVIE EXISTS");
            			Utils.sendString(request, (result.getString("Error")).toString(), 404);
        			}	
        		}
        	}
        }

	}
	
	// Function: add Relationship b/w actor, movie to database
	public void addRelationship(HttpExchange request) throws IOException, JSONException
	{
		// Parse JSON file w/ relationshipQuery method and place actorId, movieId in Map
		String query = Utils.convert(request.getRequestBody());
		query = "movie" + query;
		Map<String, String> queryParam = Utils.splitRelationshipBody(query);
    	String actorId = queryParam.get("actorId");
        String movieId = queryParam.get("movieId");
        
        // Initialize JSONObject
        JSONObject result = new JSONObject();
        if (actorId == null || movieId == null)	// Determine if either id is null
        {
        	result.append("Error", "BAD REQUEST");
			Utils.sendString(request, (result.getString("Error")).toString(), 400);
        }
        else		// Perform regex pattern checking. Reject if incompatible
        {
        Pattern p = Pattern.compile("nm\\d{7}");
        Matcher m1 = p.matcher(actorId);
        Matcher m2 = p.matcher(movieId);
        if (!(m1.matches() || m2.matches()))
        {
        	result.append("Error", "BAD REQUEST");
			Utils.sendString(request, (result.getString("Error")).toString(), 400);
        	
        }
        else 
        {
        	// Initialize session for Neo4J Connection
        	try (Session session = driver.session()){  
        		if (!helper.checkActorId(actorId, session))		// Check if actorId exists in database. Reject if non-existent
        		{
        			result.append("Error", "NOT FOUND");
        			Utils.sendString(request, (result.getString("Error")).toString(), 404);
        		}
        		else if (!helper.checkMovieId(movieId, session))	// Check if movieId exists in database. Reject if non-existent
        		{
        			result.append("Error", "NOT FOUND");
        			Utils.sendString(request, (result.getString("Error")).toString(), 404);
        		}
        		else if (!helper.checkRelationship(actorId, movieId, session))		// Check if Relationship exists in database. Reject if present
        		{
        			// Communicate with Neo4J and create relationship actorId, movieId
        			session.writeTransaction(tx ->tx.run("MATCH (a:actor {actorId:$x})\n" +
        					"MATCH (m:movie {movieId:$y})\n" +
        					"CREATE (a)-[r:ACTED_IN]->(m)",					
        					parameters("x", actorId, "y", movieId)));
        			session.close();
        			
        			// Send success signal to client
        			result.append("No Error", "OK");
        			Utils.sendString(request, (result.getString("No Error")).toString(), 200);

        		}
        		else		
        		{
        			// Send Relationship exists signal to client
        			result.append("Error", "RELATIONSHIP EXISTS");
        			Utils.sendString(request, (result.getString("Error")).toString(), 404);
        		}
        	}
        }
        }

	}
	
	// Function: Return actor name, movies acted in for actorId
	public void getActor(HttpExchange request) throws IOException, JSONException {
		
		// Parse query and place actorId in map
		URI uri = request.getRequestURI();
        String query = uri.getQuery();
        Map<String, String> queryParam = Utils.splitQuery(query);
    	String id = queryParam.get("actorId");
    	// Initialize JSONObject
    	JSONObject result = new JSONObject();
    	if (id == null)		// Determine if actorId is null
    	{
    		result.append("Error", "BAD REQUEST");
			Utils.sendString(request, (result.getString("Error")).toString(), 400);
    	}
    	else			// Perform regex pattern checking. Reject if incompatible
    	{
    		Pattern p = Pattern.compile("actorId=nm\\d{7}");
    		Matcher m = p.matcher(query);
    		if (m.matches())
    		{

    			// Initialize session for Neo4J Connection
    			try (Session session = driver.session()) { 
    				if (helper.checkActorId(id, session))		// Check if actorId exists in database. Reject if non-existent
    				{ 
    					try (Transaction tx = session.beginTransaction())
    					{
    						// Communicate with Neo4J and retrieve actor name, movies acted in
    						StatementResult actorName = tx.run("MATCH (a:actor {actorId:$y})\nRETURN a.name;",	
    								parameters("y", id));
    						StatementResult actorMovies = helper.getMoviesWithActor(id, tx);

    						// Convert actorMovies to list
    						List<String> list = new ArrayList<String>();
    						while (actorMovies.hasNext())
    						{
    							list.add(actorMovies.next().get(0).toString());
    						}

    						// Insert actorId, name into JSONObject
    						result.put("actorId", id);
    						String name = actorName.next().get(0).toString();
    						result.put("name", name.replace("\"",""));

    						// Create ListIterator to iterate over values of list
    						ListIterator<String> li = list.listIterator();

    						if (!li.hasNext())		// if list is empty, place empty list
    						{
    							result.put("movies:", list);
    						}
    						else
    						{
    							// Continue to append to "movies" key until all values have been observed
    							while (li.hasNext())	
    							{
    								result.append("movies:", (li.next()).replace("\"", ""));
    							}
    						}
    						// Send success signal to client
    						Utils.sendString(request, result.toString(2), 200);
    					}

    				}
    				else 
    				{
    					// Send Actor not found signal to client
    					result.append("Error", "NOT FOUND");
    	    			Utils.sendString(request, (result.getString("Error")).toString(), 404);
    				}
    			}
    		}
    		else	
    		{
    			// Send Request format is wrong signal to client
    			result.append("Error", "BAD REQUEST");
    			Utils.sendString(request, (result.getString("Error")).toString(), 400);
    		}
    	}
	}

	// Function: Return movie name, actors acted in for movieId
	public void getMovie(HttpExchange request) throws IOException, JSONException {
		
		// Parse query and place actorId in map
		URI uri = request.getRequestURI();
        String query = uri.getQuery();
		Map<String, String> queryParam = Utils.splitQuery(query);
    	String id = queryParam.get("movieId");
    	// Initialize JSONObject
		JSONObject result = new JSONObject();
    	if (id == null)		// Determine if actorId is null
    	{
    		result.append("Error", "BAD REQUEST");
			Utils.sendString(request, (result.getString("Error")).toString(), 400);
    	}
    	else				// Perform regex pattern checking. Reject if incompatible
    	{
    		Pattern p = Pattern.compile("movieId=nm\\d{7}");
    		Matcher m = p.matcher(query);
    		if (m.matches())
    		{
    			
    			// Initialize session for Neo4J Connection
    			try (Session session = driver.session()) { 
    				if (helper.checkMovieId(id, session))		// Check if movieId exists in database. Reject if non-existent
    				{ 
    					try (Transaction tx = session.beginTransaction())
    					{
    						// Communicate with Neo4J and retrieve movie name, actors acted in
    						StatementResult movieName = tx.run("MATCH (m:movie {movieId:$y})\nRETURN m.name;",	
    								parameters("y", id));
    						StatementResult movieActors = helper.getActorsInMovie(id, tx);
    						
    						// Convert movieActors to list
    						List<String> list = new ArrayList<String>();
    						while (movieActors.hasNext())
    						{
    							list.add(movieActors.next().get(0).toString());
    						}

    						// Insert movieId, name into JSONObject
    						result.put("movieId", id);
    						String name = movieName.next().get(0).toString();
    						result.put("name", name.replace("\"",""));

    						// Create ListIterator to iterate over values of list
    						ListIterator<String> li = list.listIterator();

    						if (!li.hasNext())		// if list is empty, place empty list
    						{
    							result.put("actors:", list);
    						}
    						else
    						{
    							// Continue to append to "actors" key until all values have been observed
    							while (li.hasNext())
    							{
    								result.append("actors:", (li.next()).replace("\"", ""));
    							}
    						}
    						// Send success signal to client
    						Utils.sendString(request, result.toString(2), 200);
    					}

    				}
    				else 
    				{
    					// Send Request format is wrong signal to client
    					result.append("Error", "NOT FOUND");
    					Utils.sendString(request, (result.getString("Error")).toString(), 404);
    				}
    			}
    		}
    		else
    		{
    			result.append("Error", "BAD REQUEST");
    			Utils.sendString(request, (result.getString("Error")).toString(), 400);
    		}
    	}
	}
	
	// Function: Return actor name, movies acted in for actorId
	public void hasRelationship(HttpExchange request) throws IOException, JSONException {
		
		// Parse query and place actorId, movieId in map
		URI uri = request.getRequestURI();
		String query = uri.getQuery();
		Map<String, String> queryParam = Utils.splitQuery(query);
		String actorId = queryParam.get("actorId");
		String movieId = queryParam.get("movieId");
		
		// Initialize JSONObject, boolean for relationship status
		JSONObject result = new JSONObject();
		Boolean b;	

		if (actorId == null || movieId == null) 	// Determine if either id is null
		{
			result.append("Error", "BAD REQUEST");
			Utils.sendString(request, (result.getString("Error")).toString(), 400);
		}
		else			// Perform regex pattern checking. Reject if incompatible
		{
			Pattern p = Pattern.compile("[a-z]{5}Id=nm\\d{7}&[a-z]{5}Id=nm\\d{7}");
			Matcher m = p.matcher(query);
			if (m.matches())
			{
				// Initialize session for Neo4J Connection
				try (Session session = driver.session()) { 
					if (!helper.checkActorId(actorId, session))		// Check if actorId exists in database. Reject if non-existent
					{
						result.append("Error", "NOT FOUND");
						Utils.sendString(request, (result.get("Error")).toString(), 404);
					}
					else if (!helper.checkMovieId(movieId, session))	// Check if movieId exists in database. Reject if non-existent
					{
						result.append("Error", "NOT FOUND");
						Utils.sendString(request, (result.get("Error")).toString(), 404);
					}
					else
					{ 
						// Check Relationship status b/w actorId, movieId. Return boolean to b
						b = helper.checkRelationship(actorId, movieId, session);
						
						// Append actorId, movieId, b to JSONObject
						actorId = actorId.replace("\"", "");		
						result.put("actorId", actorId);
						movieId = movieId.replace("\"", "");
						result.put("movieId", movieId);
						result.put("hasRelationship", b);

						// Send success signal to client
						Utils.sendString(request, result.toString(2), 200);
					}
					session.close();
				}
			}
			else
			{
				// Send Request format is wrong signal to client
				result.append("Error", "BAD REQUEST");
    			Utils.sendString(request, (result.getString("Error")).toString(), 400);
			}
		}
	}
	
	// Function: Compute Bacon Number based on actorId
	public void computeBaconNumber(HttpExchange request) throws IOException, JSONException
	{
		// Parse query and place actorId in map
		URI uri = request.getRequestURI();
		String query = uri.getQuery();
		Map<String, String> queryParam = Utils.splitQuery(query);
		String actorId = queryParam.get("actorId");
		// Initialize JSONObject
		JSONObject result = new JSONObject();
		
		if (!(actorId == null))		// Determine if actorId is null
		{
			// Perform regex pattern checking. Reject if incompatible
			Pattern p = Pattern.compile("actorId=nm\\d{7}");
			Matcher m = p.matcher(query);
			if (m.matches())
			{
				if (actorId.equals(kevinId)) // if actorId = nm0000102 (Kevin Bacon), return 0 (Kevin Bacon's bacon number) to client
				{
					List<String> baconPath = new ArrayList<String>();
					baconPath.add(kevinId);
					bacon.put(kevinId, new Bacon(0, baconPath));
					result.put("baconNumber", bacon.get(kevinId).getBaconNumber());
					Utils.sendString(request, result.toString(2), 200);
				}
				else if (bacon.containsKey(actorId))	// Check bacon and return baconNumber if mapping exists
				{
					result.put("baconNumber", bacon.get(actorId).getBaconNumber());
					Utils.sendString(request, result.toString(2), 200);
				}
				else 
				{
					// Initialize session for Neo4J Connection
					try (Session session = driver.session()) { 
						if (!helper.checkActorId(actorId, session))		// Check if actorId exists in database. Reject if non-existent
						{
							result.append("Error", "NOT FOUND");
			    			Utils.sendString(request, (result.getString("Error")).toString(), 404);
						}
						else
						{
							try (Transaction tx = session.beginTransaction())
							{
								// initialize actorsVisited, moviesVisited map. Populate actorDatabase, movieDatabase w/ current database contents
								actorsVisited = new HashMap<String, List<String>>();
								moviesVisited = new HashMap<String, Boolean>();
								actorDatabase = helper.getActorDatabase(tx);
								movieDatabase = helper.getMovieDatabase(tx);

								// Assign initial "baconPath" to actorId. Put actorId on map w/ path
								List<String> path = new ArrayList<String>();
								path.add(actorId);
								actorsVisited.put(actorId, path);
								
								// Call recursive function computeBacon to find baconNumber
								helper.computeBacon(actorId, actorId, path, actorsVisited, moviesVisited, actorDatabase, movieDatabase, bacon, tx, request);

								if (bacon.containsKey(actorId)) // return baconNumber to client if bacon map has actorId assigned as a key
								{
									result.put("baconNumber", bacon.get(actorId).getBaconNumber());
									Utils.sendString(request, result.toString(2), 200);
								}
								else
								{
									// Send No Path Found signal to client
									result.append("Error", "NO PATH");
					    			Utils.sendString(request, (result.getString("Error")).toString(), 404);
								}
							}
						}
						session.close();
					}
				}
			}
			else
			{
				// Send Bad Request found signal to client
				result.append("Error", "BAD REQUEST");
    			Utils.sendString(request, (result.getString("Error")).toString(), 400);
			}
		}
	}
	
	// Function: Compute Bacon Path based on actorId
	public void computeBaconPath(HttpExchange request) throws IOException, JSONException
	{
		// Parse query and place actorId in map
		URI uri = request.getRequestURI();
		String query = uri.getQuery();
		Map<String, String> queryParam = Utils.splitQuery(query);
		String actorId = queryParam.get("actorId");
		// Initialize JSONObject
		JSONObject result = new JSONObject();

		if (!(actorId == null))		// Determine if actorId is null
		{
			// Perform regex pattern checking. Reject if incompatible
			Pattern p = Pattern.compile("actorId=nm\\d{7}");
			Matcher m = p.matcher(query);
			if (m.matches())
			{
				if (actorId.equals(kevinId))	// if actorId = nm0000102 (Kevin Bacon), return 'nm0000102' (Kevin Bacon's bacon Path) to client
				{
					List<String> baconPath = new ArrayList<String>();
					baconPath.add(kevinId);
					bacon.put(kevinId, new Bacon(0, baconPath));
					result.put("baconPath", bacon.get(kevinId).getBaconPath());
					Utils.sendString(request, result.toString(2), 200);
				}
				else if (bacon.containsKey(actorId))		// Check bacon and return baconPath if mapping exists
				{
					result.put("baconPath", bacon.get(actorId).getBaconPath());
					Utils.sendString(request, result.toString(2), 200);
				}
				else 
				{
					// Initialize session for Neo4J Connection
					try (Session session = driver.session()) { 
						if (!helper.checkActorId(actorId, session))		// Check if actorId exists in database. Reject if non-existent
						{
							result.append("Error", "NOT FOUND");
			    			Utils.sendString(request, (result.getString("Error")).toString(), 404);
						}
						else
						{
							try (Transaction tx = session.beginTransaction())
							{
								// initialize actorsVisited, moviesVisited map. Populate actorDatabase, movieDatabase w/ current database contents
								actorsVisited = new HashMap<String, List<String>>();
								moviesVisited = new HashMap<String, Boolean>();
								actorDatabase = helper.getActorDatabase(tx);
								movieDatabase = helper.getMovieDatabase(tx);
								
								// Assign initial "baconPath" to actorId. Put actorId on map w/ path
								List<String> path = new ArrayList<String>();
								path.add(actorId);

								// Call recursive function computeBacon to find baconPath
								helper.computeBacon(actorId, actorId, path, actorsVisited, moviesVisited, actorDatabase, movieDatabase, bacon, tx, request);

								if (bacon.containsKey(actorId))	// return baconPath to client if bacon map has actorId assigned as a key
								{
									result.put("baconPath", bacon.get(actorId).getBaconPath());
									Utils.sendString(request, result.toString(2), 200);
								}
								else
								{
									// Send No Path Found signal to client
									result.append("Error", "NO PATH");
					    			Utils.sendString(request, (result.getString("Error")).toString(), 404);
								}
							}
						}
						session.close();
					}
				}
			}
			else
			{
				// Send Bad Request found signal to client
				result.append("Error", "BAD REQUEST");
    			Utils.sendString(request, (result.getString("Error")).toString(), 400);
			}
		}
	}

	// Function: Retrieve movie with highest percentage of similar cast members
	public void movieRecommendations (HttpExchange request) throws IOException, JSONException
	{
		// Parse query and place movieId in map
		URI uri = request.getRequestURI();
		String query = uri.getQuery();
		Map<String, String> queryParam = Utils.splitQuery(query);
		String id = queryParam.get("movieId");
		// Initialize JSONObject
		JSONObject result = new JSONObject();
		if (id == null)		// Determine if actorId is null
		{
			result.append("Error", "BAD REQUEST");
			Utils.sendString(request, (result.getString("Error")).toString(), 400);
		}
		else
		{
			// Perform regex pattern checking. Reject if incompatible
			Pattern p = Pattern.compile("movieId=nm\\d{7}");
			Matcher m = p.matcher(query);
			if (m.matches())
			{
				
				// Initialize session for Neo4J Connection
				try (Session session = driver.session()) { 
					if (helper.checkMovieId(id, session))		// Check if movieId exists in database. Reject if non-existent
					{ 
						try (Transaction tx = session.beginTransaction())
						{
							// Retrieve list of all movieIds in database w/o queried movieId
							StatementResult movieIds = tx.run("MATCH (m:movie)\nWhere m.movieId <> $x\nRETURN m.movieId;",
															parameters("x", id));
							
							// Initialize topRecommendation object
							movieRecommendation topRecommendation = new movieRecommendation();
							
							// Convert movieIds to List, convert getActorsInMovie for movieId to list
							List<String> movieList = Utils.convertList(movieIds);
							List<String> actorsInMovie = Utils.convertList(helper.getActorsInMovie(id, tx));
							
							// List Iterator for movieList
							ListIterator<String> li = movieList.listIterator();

							if (actorsInMovie.size() == 0)	// If no actors associated with movie, return non-existence signal to client
							{
								result.append("Error", "NO MOVIES");
				    			Utils.sendString(request, (result.getString("Error")).toString(), 404);
							}
							else if (movieList.size() >= 1)	// Check if there are more movies beyond the provided movieId
							{
								while (li.hasNext())	// Continue to iterate while li has objects
								{
									String movieId = li.next().toString();	// Extract movieId from next and move iterator position

									// Retrieve movie name associated with movieId, convert getActorsInMovie to list
									StatementResult movieName = tx.run("MATCH (m:movie {movieId:$y})\nRETURN m.name;",	
											parameters("y", movieId));
									String mName = Utils.convertList(movieName).toString();
									List<String> actors = Utils.convertList(helper.getActorsInMovie(movieId, tx));

									if (!(actors.size() == 0))	// Check if there is at least one actor in the movie
									{
										// initialize likeness counter
										int counter = 0;

										// List Iterator for actorsInMovie for current movie observed
										ListIterator<String> li2 = actorsInMovie.listIterator();

										while(li2.hasNext()) // compare actors in movieId to current movie observed. Increment for each matching cast member
										{
											if (actors.contains((li2.next().toString()).replace("\"", "")))
											{
												counter++;
											}
										}

										// Set likeness to percentage of actors shared between both movies
										int likeness = (int) ((counter*100)/actorsInMovie.size());
										if (likeness > topRecommendation.getRating())	// if likeness is a higher percentage than current topRecommendation, set to current movie
										{
											topRecommendation.setName(mName);
											topRecommendation.setId(movieId);
											topRecommendation.setRating(likeness);
										}
									}
								}
								// Append to JSONObject result and send success signal to client
								result.append("Recommendation",topRecommendation.getName());
								result.append("Score", topRecommendation.getRating());

								Utils.sendString(request, result.toString(2), 200);
							}
						}
					}
					else 
					{
						// Send movie not found signal to client
						result.append("Error", "NOT FOUND");
		    			Utils.sendString(request, (result.getString("Error")).toString(), 404);
					}
				}
			}
			
			else 
			{
				// Send Bad request signal to client
				result.append("Error", "BAD REQUEST");
    			Utils.sendString(request, (result.getString("Error")).toString(), 400);
			}
		}	

	}
}
