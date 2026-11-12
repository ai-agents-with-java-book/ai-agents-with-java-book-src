package org.acme;

import io.quarkus.hibernate.orm.PersistenceUnit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.acme.ownership.TaskOwner;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Transactional
public class PersistentTaskAuthorizationProvider implements TaskAuthorizationProvider {
    @Inject
    @PersistenceUnit("a2a-java")
    EntityManager entityManager;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String issuer;

    @Override
    public boolean checkRead(ServerCallContext context,
            String taskId, TaskOperation operation) {
        String owner = owner(context);
        if (owner == null || taskId == null || taskId.isBlank()) return false;
        TaskOwner stored = entityManager.find(TaskOwner.class, taskId);
        return stored != null && owner.equals(stored.ownerId);
    }

    @Override
    public boolean checkWrite(ServerCallContext context,
            String taskId, TaskOperation operation) {
        return checkRead(context, taskId, operation);
    }

    @Override
    public boolean checkCreate(ServerCallContext context, TaskOperation operation) {
        return owner(context) != null;
    }

    @Override
    public boolean isTaskRecorded(String taskId) {
        return taskId != null && !taskId.isBlank()
                && entityManager.find(TaskOwner.class, taskId) != null;
    }

    @Override
    public void recordOwnership(ServerCallContext context,
            String taskId, TaskOperation operation) {
        String owner = owner(context);
        if (owner == null || taskId == null || taskId.isBlank()) {
            throw new TaskNotFoundError();
        }
        entityManager.createNativeQuery("""
                INSERT INTO a2a_task_owner (task_id, owner_id) VALUES (?1, ?2)
                ON CONFLICT (task_id) DO NOTHING
                """).setParameter(1, taskId).setParameter(2, owner)
                .executeUpdate();
        if (!owner.equals(entityManager.find(TaskOwner.class, taskId).ownerId)) {
            throw new TaskNotFoundError();
        }
    }

    private String owner(ServerCallContext context) {
        if (context == null || context.getUser() == null || !context.getUser().isAuthenticated()) {
            return null;
        }
        String subject = context.getUser().getUsername();
        // OIDC verifies tokens; principal-claim=sub supplies the stable subject.
        return subject == null || subject.isBlank() ? null : issuer + "\n" + subject;
    }
}
