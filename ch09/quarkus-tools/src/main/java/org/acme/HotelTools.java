package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.time.Instant;

@ApplicationScoped
public class HotelTools {
    @Inject
    @RestClient
    HotelController hotelController;

    @Inject
    HotelCache cache;

    public static class SearchUnavailable extends RuntimeException {
        SearchUnavailable(Throwable cause) {
            super("Hotel search unavailable", cause);
        }
    }

    @Tool("Search for hotel suggestions in a city")
    @Fallback(fallbackMethod = "searchCommonHotels",
            applyOn = SearchUnavailable.class)
    public HotelSuggestions searchHotels(
            @P(name = "location", value = "City to search") String location) {
        HotelCache.key(location);
        java.util.List<Hotel> hotels;
        try {
            hotels = hotelController.searchHotel(location.trim());
        } catch (RuntimeException failure) {
            throw new SearchUnavailable(failure);
        }
        Instant fetchedAt = Instant.now();
        cache.store(location, hotels, fetchedAt);
        return new HotelSuggestions(hotels, "live", fetchedAt);
    }

    public HotelSuggestions searchCommonHotels(String location) {
        return cache.suggestions(location);
    }
}
