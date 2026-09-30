package com.proveedores.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MuseoTimeTest {

    @Test
    void nowInterpretaUnInstanteUtcEnLaZonaDelMuseo() {
        Clock relojUtc = Clock.fixed(Instant.parse("2026-09-30T06:19:00Z"), ZoneOffset.UTC);

        assertThat(MuseoTime.now(relojUtc)).isEqualTo(LocalDateTime.of(2026, 9, 30, 3, 19));
    }

    @Test
    void todayUsaLaFechaArgentinaAlCruzarElBordeUtc() {
        Clock relojUtc = Clock.fixed(Instant.parse("2026-10-01T01:30:00Z"), ZoneOffset.UTC);

        assertThat(MuseoTime.today(relojUtc)).isEqualTo(LocalDate.of(2026, 9, 30));
    }
}
