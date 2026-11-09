package com.auradock.dock.world

import java.time.ZoneId

/** A city the world dock can mark: its time zone, its name, the short label the map uses ("SF", "London") and where it is. */
data class WorldCity(val zoneId: String, val name: String, val short: String, val lat: Double, val lon: Double) {
    val point: GeoPoint get() = GeoPoint(lat, lon)
}

/**
 * The searchable list the founder variant picks from: one city for each of the main time zones. Android has no coordinates for a
 * time zone, so these are bundled (a few hundred bytes each; no network).
 */
object WorldCities {
    val all: List<WorldCity> = listOf(
        WorldCity("Pacific/Honolulu", "Honolulu", "HNL", 21.31, -157.86),
        WorldCity("America/Anchorage", "Anchorage", "Anchorage", 61.22, -149.90),
        WorldCity("America/Los_Angeles", "Los Angeles", "LA", 34.05, -118.24),
        WorldCity("America/Vancouver", "Vancouver", "Vancouver", 49.28, -123.12),
        WorldCity("America/Phoenix", "Phoenix", "Phoenix", 33.45, -112.07),
        WorldCity("America/Denver", "Denver", "Denver", 39.74, -104.99),
        WorldCity("America/Chicago", "Chicago", "Chicago", 41.88, -87.63),
        WorldCity("America/Mexico_City", "Mexico City", "CDMX", 19.43, -99.13),
        WorldCity("America/New_York", "New York", "NYC", 40.71, -74.01),
        WorldCity("America/Toronto", "Toronto", "Toronto", 43.65, -79.38),
        WorldCity("America/Bogota", "Bogotá", "Bogotá", 4.71, -74.07),
        WorldCity("America/Lima", "Lima", "Lima", -12.05, -77.04),
        WorldCity("America/Halifax", "Halifax", "Halifax", 44.65, -63.58),
        WorldCity("America/Caracas", "Caracas", "Caracas", 10.48, -66.90),
        WorldCity("America/Santiago", "Santiago", "Santiago", -33.45, -70.67),
        WorldCity("America/Sao_Paulo", "São Paulo", "São Paulo", -23.55, -46.63),
        WorldCity("America/Argentina/Buenos_Aires", "Buenos Aires", "BA", -34.60, -58.38),
        WorldCity("Atlantic/Reykjavik", "Reykjavik", "Reykjavik", 64.15, -21.94),
        WorldCity("Europe/Lisbon", "Lisbon", "Lisbon", 38.72, -9.14),
        WorldCity("Europe/London", "London", "London", 51.51, -0.13),
        WorldCity("Europe/Dublin", "Dublin", "Dublin", 53.35, -6.26),
        WorldCity("Africa/Casablanca", "Casablanca", "Casablanca", 33.57, -7.59),
        WorldCity("Africa/Lagos", "Lagos", "Lagos", 6.52, 3.38),
        WorldCity("Europe/Paris", "Paris", "Paris", 48.86, 2.35),
        WorldCity("Europe/Madrid", "Madrid", "Madrid", 40.42, -3.70),
        WorldCity("Europe/Amsterdam", "Amsterdam", "Amsterdam", 52.37, 4.90),
        WorldCity("Europe/Berlin", "Berlin", "Berlin", 52.52, 13.40),
        WorldCity("Europe/Rome", "Rome", "Rome", 41.90, 12.50),
        WorldCity("Europe/Zurich", "Zurich", "Zurich", 47.38, 8.54),
        WorldCity("Europe/Stockholm", "Stockholm", "Stockholm", 59.33, 18.07),
        WorldCity("Europe/Warsaw", "Warsaw", "Warsaw", 52.23, 21.01),
        WorldCity("Africa/Johannesburg", "Johannesburg", "Joburg", -26.20, 28.05),
        WorldCity("Africa/Cairo", "Cairo", "Cairo", 30.04, 31.24),
        WorldCity("Europe/Athens", "Athens", "Athens", 37.98, 23.73),
        WorldCity("Europe/Helsinki", "Helsinki", "Helsinki", 60.17, 24.94),
        WorldCity("Europe/Kyiv", "Kyiv", "Kyiv", 50.45, 30.52),
        WorldCity("Europe/Istanbul", "Istanbul", "Istanbul", 41.01, 28.98),
        WorldCity("Africa/Nairobi", "Nairobi", "Nairobi", -1.29, 36.82),
        WorldCity("Asia/Jerusalem", "Jerusalem", "Jerusalem", 31.77, 35.22),
        WorldCity("Europe/Moscow", "Moscow", "Moscow", 55.76, 37.62),
        WorldCity("Asia/Riyadh", "Riyadh", "Riyadh", 24.71, 46.68),
        WorldCity("Asia/Baghdad", "Baghdad", "Baghdad", 33.31, 44.36),
        WorldCity("Asia/Tehran", "Tehran", "Tehran", 35.69, 51.39),
        WorldCity("Asia/Dubai", "Dubai", "Dubai", 25.20, 55.27),
        WorldCity("Asia/Kabul", "Kabul", "Kabul", 34.53, 69.17),
        WorldCity("Asia/Karachi", "Karachi", "Karachi", 24.86, 67.01),
        WorldCity("Asia/Tashkent", "Tashkent", "Tashkent", 41.30, 69.24),
        WorldCity("Asia/Kolkata", "Mumbai", "Mumbai", 19.08, 72.88),
        WorldCity("Asia/Kolkata", "Delhi", "Delhi", 28.61, 77.21),
        WorldCity("Asia/Kolkata", "Bengaluru", "Bengaluru", 12.97, 77.59),
        WorldCity("Asia/Colombo", "Colombo", "Colombo", 6.93, 79.86),
        WorldCity("Asia/Kathmandu", "Kathmandu", "Kathmandu", 27.72, 85.32),
        WorldCity("Asia/Dhaka", "Dhaka", "Dhaka", 23.81, 90.41),
        WorldCity("Asia/Yangon", "Yangon", "Yangon", 16.87, 96.20),
        WorldCity("Asia/Bangkok", "Bangkok", "Bangkok", 13.76, 100.50),
        WorldCity("Asia/Ho_Chi_Minh", "Ho Chi Minh City", "HCMC", 10.82, 106.63),
        WorldCity("Asia/Jakarta", "Jakarta", "Jakarta", -6.21, 106.85),
        WorldCity("Asia/Singapore", "Singapore", "Singapore", 1.35, 103.82),
        WorldCity("Asia/Kuala_Lumpur", "Kuala Lumpur", "KL", 3.14, 101.69),
        WorldCity("Asia/Hong_Kong", "Hong Kong", "HK", 22.32, 114.17),
        WorldCity("Asia/Shanghai", "Shanghai", "Shanghai", 31.23, 121.47),
        WorldCity("Asia/Shanghai", "Beijing", "Beijing", 39.90, 116.41),
        WorldCity("Asia/Taipei", "Taipei", "Taipei", 25.03, 121.57),
        WorldCity("Asia/Manila", "Manila", "Manila", 14.60, 120.98),
        WorldCity("Australia/Perth", "Perth", "Perth", -31.95, 115.86),
        WorldCity("Asia/Seoul", "Seoul", "Seoul", 37.57, 126.98),
        WorldCity("Asia/Tokyo", "Tokyo", "Tokyo", 35.68, 139.69),
        WorldCity("Australia/Adelaide", "Adelaide", "Adelaide", -34.93, 138.60),
        WorldCity("Australia/Brisbane", "Brisbane", "Brisbane", -27.47, 153.03),
        WorldCity("Australia/Sydney", "Sydney", "Sydney", -33.87, 151.21),
        WorldCity("Australia/Melbourne", "Melbourne", "Melbourne", -37.81, 144.96),
        WorldCity("Pacific/Guam", "Guam", "Guam", 13.44, 144.79),
        WorldCity("Pacific/Auckland", "Auckland", "Auckland", -36.85, 174.76),
        WorldCity("America/Los_Angeles", "San Francisco", "SF", 37.77, -122.42),
        WorldCity("America/Los_Angeles", "Seattle", "Seattle", 47.61, -122.33),
        WorldCity("America/New_York", "Boston", "Boston", 42.36, -71.06),
        WorldCity("America/New_York", "Miami", "Miami", 25.76, -80.19),
        WorldCity("America/Chicago", "Austin", "Austin", 30.27, -97.74),
        WorldCity("America/Edmonton", "Edmonton", "Edmonton", 53.55, -113.49),
        WorldCity("America/Havana", "Havana", "Havana", 23.11, -82.37),
        WorldCity("America/Panama", "Panama City", "Panama", 8.98, -79.52),
        WorldCity("America/Montevideo", "Montevideo", "Montevideo", -34.90, -56.16),
        WorldCity("Africa/Accra", "Accra", "Accra", 5.60, -0.19),
        WorldCity("Africa/Addis_Ababa", "Addis Ababa", "Addis", 9.03, 38.74),
        WorldCity("Africa/Algiers", "Algiers", "Algiers", 36.75, 3.04),
        WorldCity("Europe/Brussels", "Brussels", "Brussels", 50.85, 4.35),
        WorldCity("Europe/Vienna", "Vienna", "Vienna", 48.21, 16.37),
        WorldCity("Europe/Prague", "Prague", "Prague", 50.08, 14.44),
        WorldCity("Europe/Copenhagen", "Copenhagen", "Copenhagen", 55.68, 12.57),
        WorldCity("Europe/Oslo", "Oslo", "Oslo", 59.91, 10.75),
        WorldCity("Asia/Doha", "Doha", "Doha", 25.29, 51.53),
        WorldCity("Asia/Muscat", "Muscat", "Muscat", 23.59, 58.41),
        WorldCity("Asia/Baku", "Baku", "Baku", 40.41, 49.87),
        WorldCity("Asia/Almaty", "Almaty", "Almaty", 43.24, 76.89),
        WorldCity("Asia/Novosibirsk", "Novosibirsk", "Novosibirsk", 55.03, 82.92),
        WorldCity("Asia/Vladivostok", "Vladivostok", "Vladivostok", 43.12, 131.89),
        WorldCity("Pacific/Fiji", "Suva", "Suva", -18.14, 178.44)
    )

    fun byName(name: String): WorldCity? = all.firstOrNull { it.name == name }

    /** Cities whose name, label or zone contains the typed text (case ignored); everything when it is blank. */
    fun search(query: String): List<WorldCity> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all.sortedBy { it.name }
        return all.filter { it.name.lowercase().contains(q) || it.short.lowercase().contains(q) || it.zoneId.lowercase().replace('_', ' ').contains(q) }
            .sortedBy { if (it.name.lowercase().startsWith(q)) 0 else 1 }
    }

    /**
     * The time zone's own city, for a home place when there is no location: the bundled city of that zone (the first one listed), or
     * a name made from the zone id with its longitude guessed from the zone's offset. Its dot is not drawn (it is only a guess).
     */
    fun forZone(zone: ZoneId, atMillis: Long): HomeFallback {
        all.firstOrNull { it.zoneId == zone.id }?.let { return HomeFallback(it.name, it.point) }
        val offsetHours = zone.rules.getOffset(java.time.Instant.ofEpochMilli(atMillis)).totalSeconds / 3600.0
        val name = zone.id.substringAfterLast('/').replace('_', ' ')
        return HomeFallback(name, GeoPoint(0.0, offsetHours * 15.0))
    }
}

/** A home place derived from the time zone alone. */
data class HomeFallback(val name: String, val point: GeoPoint)
