package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class HotelCache {
    public record Entry(List<Hotel> hotels, Instant asOf) {
    }

    private final ValueCommands<String, Entry> entries;

    @Inject
    public HotelCache(RedisDataSource redis) {
        entries = redis.value(Entry.class);
    }

    static String key(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Hotel location is required");
        }
        return "hotel-suggestions:" + location.trim().toLowerCase(Locale.ROOT);
    }

    public void store(String location, List<Hotel> hotels, Instant asOf) {
        entries.set(key(location), new Entry(List.copyOf(hotels), asOf));
    }

    public HotelSuggestions suggestions(String location) {
        Entry cached = entries.get(key(location));
        return cached == null
                ? new HotelSuggestions(List.of(), "cache-miss", null)
                : new HotelSuggestions(cached.hotels(), "cache", cached.asOf());
    }
}
