package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class IncidentRepository {

  private final Map<String, Incident> store = new ConcurrentHashMap<>();

  public Incident create(String title, Severity severity) {
    String id = UUID.randomUUID().toString();
    Incident incident = new Incident(id, title, severity, Status.OPEN, null);
    store.put(id, incident);
    return incident;
  }

  public List<Incident> findAll() {
    return new ArrayList<>(store.values());
  }

  public Optional<Incident> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }

  public Incident assign(String id, String engineer) {
    Incident existing = store.get(id);
    Incident updated = new Incident(
        existing.id(),
        existing.title(),
        existing.severity(),
        existing.status(),
        engineer
    );
    store.put(id, updated);
    return updated;
  }

  public Incident resolve(String id) {
    Incident existing = store.get(id);
    Incident updated = new Incident(
        existing.id(),
        existing.title(),
        existing.severity(),
        Status.RESOLVED,
        existing.assignee()
    );
    store.put(id, updated);
    return updated;
  }
}
