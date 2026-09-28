package org.example.krishantutioncenter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_usage")
public class AiUsage {

    @Id
    @Column(name = "month_key", length = 7)
    private String monthKey;

    @Column(nullable = false)
    private int requestsUsed;

    protected AiUsage() {
    }

    public AiUsage(String monthKey) {
        this.monthKey = monthKey;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public int getRequestsUsed() {
        return requestsUsed;
    }

    public void increment() {
        requestsUsed++;
    }
}
