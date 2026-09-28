package org.example.krishantutioncenter.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_quota_lock")
public class AiQuotaLock {

    @Id
    private int id;

    protected AiQuotaLock() {
    }

    public AiQuotaLock(int id) {
        this.id = id;
    }
}
