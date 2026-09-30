package com.proveedores.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Clock helpers for application events and business dates in the Museum's zone.
 */
public final class MuseoTime {

    public static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private MuseoTime() {
    }

    public static LocalDateTime now() {
        return now(Clock.systemUTC());
    }

    public static LocalDateTime now(Clock clock) {
        return LocalDateTime.now(clock.withZone(ZONE));
    }

    public static LocalDate today() {
        return today(Clock.systemUTC());
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }
}
