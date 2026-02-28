package com.drone.api;

import com.drone.data.DroneGpsReading;
import com.drone.gps.DroneGpsSerialReader;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/drone-gps")
public class DroneGpsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestDroneGps() {
        DroneGpsReading reading = DroneGpsSerialReader.getInstance().getLatestReading();
        if (reading == null) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        return Response.ok(reading).build();
    }
}
