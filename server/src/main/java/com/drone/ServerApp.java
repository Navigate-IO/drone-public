package com.drone;

import com.drone.data.BaseAreaSensorDump;
import com.drone.gps.DroneGpsSerialReader;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

public class ServerApp {

    // Base URI the Grizzly HTTP server will listen on every network interfaces
    public static String baseUri = "http://0.0.0.0:80/";
    private static final DroneGpsSerialReader droneGpsSerialReader = DroneGpsSerialReader.getInstance();

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(String arg) {
        // Create a resource config that scans for JAX-RS resources and providers in the package
        final ResourceConfig rc = new ResourceConfig().packages("com.drone");
        Config config = Config.loadDefaultConfigFile(arg);
        baseUri = "http://" + config.getIpAddress() + ":" + config.getPortNumber();
        System.out.println("URI=" + baseUri);
        BaseAreaSensorDump.initialize(config.getTeamName(), config.getTakeOffTime());
        DataHandler.ipAddress = config.getActualIpAddress();
        droneGpsSerialReader.start(
            config.getDroneGpsSerialDevice(),
            config.getDroneGpsBaudRate(),
            config.isDroneGpsAutoPortScan()
        );

        // Create and start a new instance of the Grizzly HTTP server
        // Exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), rc);
    }

    public static void main(String[] args) throws IOException {
        // Start the server
        String argument = null;
        if (args.length > 0) {
            argument = args[0];
        }
        final HttpServer server = startServer(argument);
        System.out.println(String.format("Jersey app started at %s%s", baseUri, ""));
        System.out.println("Hit enter to stop it...");
        System.in.read();
        droneGpsSerialReader.stop();
        server.shutdownNow();
    }
}
