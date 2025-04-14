package com.example.gateway.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "active_log")
@Data
public class ActiveLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transaction;

    @Column(name = "log_timestamp")
    private LocalDateTime logTimestamp = LocalDateTime.now();

    @Column(name = "log_stage")
    private String logStage; // e.g., "CoreToGateway", "GatewayToNapas", etc.

    @Column(name = "json_data", columnDefinition = "CLOB") // CLOB for JSON payload
    private String jsonData;

    @Column(name = "status")
    private String status; // "SUCCESS" or "FAILED"
}