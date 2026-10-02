package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.entity.Incidence;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class IncidenceRepoTest {

    @Inject
    Incidence.Repo incidenceRepo;

    @Test
    public void shouldGetIncidences() {
        List<Incidence> incidenceRepoByDate = incidenceRepo
            .findByDate(
                LocalDate.now().minus(Period.ofDays(90))
            );

        assertThat(incidenceRepoByDate).hasSize(1);

    }

}
