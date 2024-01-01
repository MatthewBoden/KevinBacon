package ca.yorku.eecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.StatementResult;

import com.sun.net.httpserver.HttpExchange;
// Test
// Goal of Utils Class: Provide utility functions for parsing JSON/Query data, splitting data for map pairings and sending REST signals to client
class Utils {        
	
	// JSON split method. Pulls name, actorId/movieId and maps to HashMap
    public static Map<String, String> splitBody(String query) throws UnsupportedEncodingException {
        String string = query.replaceAll("\"", "");
    	
        /*
        if (!string.startsWith("name") && !string.contains("name"))
        {
        	string = "name" + string;
        }
        else if (!string.startsWith("actorId") && !string.contains("actorId") && !string.contains("movieId"))
        {
        	string = "actor" + string;
        }
        else if (!string.startsWith("movieId") && !string.contains("movieId") && !string.contains("actorId"))
        {
        	string = "movie" + string;
        }
		*/
    	Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = string.split(",");
    	
        for (String pair : pairs) {
            int idx = pair.indexOf(":");
            String pairKey = pair.substring(0, idx);
            String pairValue = pair.substring(idx + 1);
            pairKey = pairKey.replaceAll("\\s", "");
            pairValue = pairValue.replaceFirst("\\s", "").replace("\n", "").replace("\r", "");
            query_pairs.put(pairKey, pairValue);
        }
        return query_pairs;
    }
    
    // JSON relationship split method. Pulls actorId, movieId and maps to HashMap
    public static Map<String, String> splitRelationshipBody(String query) throws UnsupportedEncodingException {
        String string = query.replaceAll("\"", "");
        
        /*
        if (!string.startsWith("movieId") && !string.contains("movieId") && string.contains("actorId"))
        {
        	string = "movie" + string;
        }
        else if (!string.startsWith("actorId") && !string.contains("actorId") && string.contains("movieId"))
        {
        	string = "actor" + string;
        }
        */
    	Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = string.split(",");
        
        for (String pair : pairs) {
            pair = pair.replaceAll("\\s", "");
            int idx = pair.indexOf(":");
            String pairKey = pair.substring(0, idx);
            String pairValue = pair.substring(idx + 1);
            pairKey = pairKey.replaceAll("\\s", "");
            pairValue = pairValue.replace(":", "").replaceFirst("\\s", "").replace("\n", "");
            query_pairs.put(pairKey, pairValue);
        }
        return query_pairs;
    }
	
    // Query split method. Separates components of query and maps accordingly
    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
    
    // Converts JSON file contents to String
	public static String convert(InputStream inputStream) throws IOException {
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String result = br.lines().collect(Collectors.joining(System.lineSeparator()));
            result = result.substring(7, result.length() - 1);
            return result;
        }
    }
	
	// Sends REST API response and string data to client
	public static void sendString(HttpExchange request, String data, int restCode) 
			throws IOException {
		request.sendResponseHeaders(restCode, data.length());
        OutputStream os = request.getResponseBody();
        os.write(data.getBytes());
        os.close();
	}
	
	// Convert StatementResult to an ArrayList<String>
	public static List<String> convertList(StatementResult res) throws IOException
	{
		List<String> result = new ArrayList<String>();
		
		while (res.hasNext())
		{
			result.add((res.next().get(0).toString()).replace("\"", ""));
		}
		
		return result;
	}
	
	// Convert StatementResult to an ArrayList<String>. Places actorId onto actorsVisited map
	public static List<String> convertListBacon(StatementResult res, Map<String, List<String>> actorsVisited) throws IOException
	{
		List<String> result = new ArrayList<String>();
		
		while (res.hasNext())
		{
			String actor = (res.next().get(0).toString()).replace("\"", "");
			if (!actorsVisited.containsKey(actor))
			{
				result.add(actor);
				actorsVisited.put(actor, null);
			}
		}
		
		return result;
	}
}