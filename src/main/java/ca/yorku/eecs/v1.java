package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

// Goal of v1 class: Handle requests being received and invoke the corresponding function in Neo4JDatabase
public class v1 implements HttpHandler {
	
	private Neo4JDatabase nb = new Neo4JDatabase();
	public void handle(HttpExchange request) throws IOException {
		// TODO Auto-generated method stub
		
        try {
        	URI uri = request.getRequestURI();
            String path = uri.getPath();

            if (path.contains("addActor")) {
            	nb.addActor(request);
            }
            else if (path.contains("addMovie")) {
            	nb.addMovie(request);
            }
            else if (path.contains("addRelationship")) {
            	nb.addRelationship(request);
            }
            else if (path.contains("getActor")) {
            	nb.getActor(request);
            }
            else if (path.contains("getMovie")) {
            	nb.getMovie(request);
            }
            else if (path.contains("hasRelationship")) {
            	nb.hasRelationship(request);
            }
            
            else if (path.contains("movieRecommendations")) {
            	nb.movieRecommendations(request);
            }
            
            else if (path.contains("computeBaconNumber")) {
            	nb.computeBaconNumber(request);
            }
            else if (path.contains("computeBaconPath")) {
            	nb.computeBaconPath(request);
            }
            else
            {
            	Utils.sendString(request, "Unimplemented method\n", 501);
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	Utils.sendString(request, "Server error\n", 500);
        }
		
	}
}
