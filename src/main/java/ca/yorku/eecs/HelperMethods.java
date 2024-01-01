package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import com.sun.net.httpserver.HttpExchange;

// Goal of HelperMethods class: Decrease bloat in Neo4JDatabase. Contains functions for entity checking, retrieving data, computing bacon attributes
public class HelperMethods {
	
	private final String kevinId = "nm0000102";
	
	// Function: Verify if actorId exists in the database
	public boolean checkActorId(String actorId, Session session) throws IOException
	{
		// Send instruction to determine if actor object exists with actorId
		try (Transaction tx = session.beginTransaction())
		{
			StatementResult hasActor = tx.run("MATCH (a:actor {actorId:$y})\nRETURN a IS NOT NULL",
					parameters("y", actorId));

			boolean b;

			// set boolean b to true/false based on StatementResult
			if (hasActor.hasNext())
			{
				b = true;
			}
			else 
			{
				b = false;
			}

			return b;
		}
	}

	// Function: Verify if movieId exists in the database
	public boolean checkMovieId(String movieId, Session session) throws IOException 
	{
		// Send instruction to determine if movie object exists with movieId
		try (Transaction tx = session.beginTransaction())
		{
			StatementResult hasMovie = tx.run("MATCH (m:movie {movieId:$y})\nRETURN m IS NOT NULL",
					parameters("y", movieId));

			boolean b;

			// set boolean b to true/false based on StatementResult
			if (hasMovie.hasNext())
			{
				b = true;
			}
			else 
			{
				b = false;
			}

			return b;
		}
	}
	
	// Function: Verify if Relationship exists b/w actorId, movieId
	public boolean checkRelationship(String actorId, String movieId, Session session) throws IOException
	{
		// Send instruction to determine if relationship exists 
			try (Transaction tx = session.beginTransaction())
			{
				StatementResult hasR = tx.run("MATCH (a:actor {actorId:$x})-[r:ACTED_IN]-> " +
														  "(m:movie {movieId:$y})\n" +
														  "RETURN r IS NOT NULL",					
														parameters("x", actorId, "y", movieId));
				boolean b;

				// set boolean b to true/false based on StatementResult
				if (hasR.hasNext())
				{
					b = true;
				}
				else 
				{
					b = false;
				}
				return b;
			}
	}
		
	// Function: Retrieve all actorIds associated with movieId
	public StatementResult getActorsInMovie(String movieId, Transaction tx)
	{
		// Send instruction to retrieve list of actors and return StatementResult 
		StatementResult movieActors = tx.run("Match (a:actor)-[r:ACTED_IN]->(m:movie {movieId:$y})\n"
				+ "return a.actorId;",	
				parameters("y", movieId));
		
		return movieActors;
	}
	
	// Function: Retrieve all movieIds associated with actorId
	public StatementResult getMoviesWithActor(String actorId, Transaction tx)
	{
		// Send instruction to retrieve list of actors and return StatementResult
		StatementResult actorMovies = tx.run("Match(a:actor {actorId:$y})-[r:ACTED_IN]->(m:movie)\n"
				+ "return m.movieId;",	
				parameters("y", actorId));
		
		return actorMovies;
	}
	
	// Function: Recursively retrieve shortest path to Kevin Bacon from given originalId and place in bacon map if a path exists
	public void computeBacon(String originalId, String actorId, List<String> path, Map<String, List<String>> actorsVisited, Map<String, Boolean> moviesVisited, List<String> actorDatabase, List<String> movieDatabase, Map<String, Bacon> bacon, Transaction tx, HttpExchange request) throws IOException, JSONException
	{
		// Initialize variables
		LinkedList<String> actors = new LinkedList<String>();
		List<String> newPath = new ArrayList<String>();	
		List<String> newPath2 = new ArrayList<String>();
		List<String> newPath3 = new ArrayList<String>();
		List<String> res = new ArrayList<String>();
		// Create initial list of movies associated with current actorId
		StatementResult movies = getMoviesWithActor(actorId, tx);
		
		// convert movies to list
		res = Utils.convertList(movies);
		
		ListIterator<String> li = res.listIterator();
		
		// Iterate over set of movies associated with current actorId
		while (li.hasNext())
		{
			// convert next value from iterator to string
			String movie = li.next().toString();
			if (!moviesVisited.containsKey(movie))	// Check if movie has been encountered
			{
				// get all actors associated with current movie, create iterator below
				StatementResult actorsInMovie = getActorsInMovie(movie, tx);
				List<String> currentActors = Utils.convertListBacon(actorsInMovie, actorsVisited);	
				ListIterator<String> curAct = currentActors.listIterator();
				
				// Queue curAct into actors for use later
				while (curAct.hasNext())
				{
					String cActId = curAct.next();
					if (!actors.contains(cActId))
					{
						actors.add(cActId);
					}
				}

				// Clear and create deep copy of path provided and add current movie to path. Place movie in moviesVisited map
				newPath.clear();
				newPath.addAll(path);
				newPath.add(movie);
				moviesVisited.put(movie, true);
				
				if (currentActors.contains(kevinId))	// Check if currentActors list contains Kevin Bacon
				{
					// Calculate baconNumber as shown below. add newPath to baconPath
					int baconNumber = (int)Math.floor(newPath.size()/2);
					List<String> baconPath = new ArrayList<String>();

					for(int i = 0; i < newPath.size(); i++)
					{
						baconPath.add((newPath.get(i).toString()).replace("\"", ""));
					}

					// add Kevin Bacon ID to baconPath. Place originalId on map with new Bacon object with baconNumber and baconPath
					baconPath.add(kevinId);
					bacon.put(originalId, new Bacon(baconNumber, baconPath));
				}
				else	// Kevin Bacon ID not found in current list
				{
					// List Iterator for currentActors Queue
					ListIterator<String> li2 = currentActors.listIterator();

					// Clear and create deep copy of newPath
					newPath2.clear();
					newPath2.addAll(newPath);
					
					// Iterate over currentActors Queue and add all actorIds encountered onto actorsVisited map
					while(li2.hasNext())
					{
						String actId = li2.next();
						if (!actorsVisited.containsKey(actId))
						{
							actorsVisited.put(actId, newPath);
						}
					}
				}	
			}
		}
		
		// Once movie queue for actorId has been completed, iterate over actors queue
		while (!actors.isEmpty())
		{
			// poll actors queue, clear and create deep copy of newPath2 and recursively call computeBacon with the next actorId and updated path
			String newActor = actors.poll();
			newPath3.clear();
			newPath3.addAll(newPath2);
			newPath3.add(newActor);
			computeBacon(originalId, newActor, newPath3, actorsVisited, moviesVisited, actorDatabase, movieDatabase, bacon, tx, request);
		}
	}
	
	// Function: Retrieve list of all actors in current Database
	public List<String> getActorDatabase(Transaction tx) throws IOException
	{
		// Send instruction to retrieve actor database and return StatementResult
		StatementResult database = tx.run("Match(a:actor)\n"
											+ "return a.actorId;");
		
		List<String> actors = Utils.convertList(database);
		return actors;
	}
	
	// Function: Retrieve list of all movies in current Database
	public List<String> getMovieDatabase(Transaction tx) throws IOException
	{
		// Send instruction to retrieve movie database and return StatementResult
		StatementResult database = tx.run("Match(m:movie)\n"
											+ "return m.movieId;");
		
		List<String> movies = Utils.convertList(database);
		return movies;
	}
	
}
