package org.acme;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.entity.Incidence;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;

@ApplicationScoped
public class DbPopulation {

    @Inject
    Incidence.Repo incidenceRepo;

    @Startup
    @Transactional
    public void createIncidences() {

        LocalDate createdDate = LocalDate.now().minus(Period.ofDays(5));

        Incidence incidence = new Incidence();

        incidence.createdAt  = createdDate;
        incidence.message = "The flight was very pleasant";
        incidence.response = "All was great";
        incidence.userEmail = "a@example.com";
        incidence.bookingId = 2L;

        incidenceRepo.insert(incidence);
    }

}
