package com.docuagent.localapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tab_id", nullable = false)
    private Long tabId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "user_context", nullable = false, length = 8000)
    private String userContext;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Task() {
    }

    public Task(Long tabId, String title, String userContext, String status) {
        this.tabId = tabId;
        this.title = title;
        this.userContext = userContext;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void update(String title, String userContext) {
        this.title = title;
        this.userContext = userContext;
    }

    public Long getId() {
        return id;
    }

    public Long getTabId() {
        return tabId;
    }

    public String getTitle() {
        return title;
    }

    public String getUserContext() {
        return userContext;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
