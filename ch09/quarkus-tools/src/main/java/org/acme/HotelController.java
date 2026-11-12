package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "hotel-catalog")
@Path("/hotels")
public interface HotelController {
    @GET
    List<Hotel> searchHotel(@QueryParam("location") String location);
}
