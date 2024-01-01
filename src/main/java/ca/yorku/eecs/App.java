package ca.yorku.eecs;

import java.util.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App 
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        // TODO: two lines of code are expected to be added here
        // please refer to the HTML server example 
        v1 v1 = new v1();

        server.createContext("/api/v1/", v1::handle);

        
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
