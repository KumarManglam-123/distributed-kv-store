package com.kvstore.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;

    @Column(name = "entry_key")
    private String key;

    @Column(name = "entry_value")
    private String value;

    private String nodeId;
    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(String action, String key, String value, String nodeId) {
        this.action = action;
        this.key = key;
        this.value = value;
        this.nodeId = nodeId;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public String getNodeId() { return nodeId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}