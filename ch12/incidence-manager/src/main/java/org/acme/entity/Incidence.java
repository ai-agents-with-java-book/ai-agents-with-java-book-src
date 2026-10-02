package org.acme.entity;

import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.LocalDate;
import java.util.List;

@Entity
public class Incidence {

    @Id
    @GeneratedValue
    public Long id;

    @Column
    public LocalDate createdAt;

    @Lob
    @Column
    public String message;

    @Lob
    @Column
    public String response;

    @Column
    public String userEmail;

    @Column
    public Long bookingId;

    public interface Repo extends PanacheRepository.Stateless<Incidence, Long> {

        @Find
        List<Incidence> findByUserEmail(String userEmail);

        @Query("WHERE createdAt >= :limit")
        List<Incidence> findByDate(LocalDate limit);

    }

}
