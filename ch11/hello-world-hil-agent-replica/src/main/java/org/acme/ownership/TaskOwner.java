package org.acme.ownership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "a2a_task_owner")
public class TaskOwner {
    @Id
    @Column(name = "task_id", nullable = false, updatable = false)
    public String taskId;

    @Column(name = "owner_id", nullable = false, updatable = false, length = 2048)
    public String ownerId;
}
