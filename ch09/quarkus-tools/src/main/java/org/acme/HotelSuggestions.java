package org.acme;

import java.time.Instant;
import java.util.List;

public record HotelSuggestions(List<Hotel> hotels, String source, Instant asOf) {
}
