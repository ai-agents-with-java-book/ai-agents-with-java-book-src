package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.spec.A2AError;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CustomTaskAuthorizationProvider implements TaskAuthorizationProvider {

    // In-memory mapping of taskId -> ownerId (Use a DB/Store in production)
    private final Map<String, String> taskOwners = new ConcurrentHashMap<>();

    @Override
    public boolean checkRead(ServerCallContext context, String taskId, TaskOperation op) {
        String currentUserId = extractUserId(context);
        String ownerId = taskOwners.get(taskId);

        // If the task exists, only allow the task owner to read it
        return ownerId != null && ownerId.equals(currentUserId);
    }

    @Override
    public boolean checkWrite(ServerCallContext context, String taskId, TaskOperation op) {
        String currentUserId = extractUserId(context);
        String ownerId = taskOwners.get(taskId);

        // Only allow the owner to cancel or update the task
        return ownerId != null && ownerId.equals(currentUserId);
    }

    @Override
    public boolean checkCreate(ServerCallContext context, TaskOperation operation) throws A2AError {
        return extractUserId(context) != null;
    }

    @Override
    public boolean isTaskRecorded(String taskId) throws A2AError {
        return taskOwners.containsKey(taskId);
    }

    @Override
    public void recordOwnership(ServerCallContext context, String taskId, TaskOperation operation) throws A2AError {
        String currentUserId = extractUserId(context);
        if (currentUserId != null) {
            // Idempotent association between the new task and the calling user
            taskOwners.putIfAbsent(taskId, currentUserId);
        }
    }

    /**
     * Helper to extract authenticated subject/user ID from the ServerCallContext or SecurityContext.
     */
    private String extractUserId(ServerCallContext context) {
        if (context == null) {
            return null;
        }

        // Extract principal/subject claim injected into ServerCallContext attributes by OIDC/OAuth filter
        return context.getUser().getUsername();
    }
}
