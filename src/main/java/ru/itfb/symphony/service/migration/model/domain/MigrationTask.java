package ru.itfb.symphony.service.migration.model.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "migration_task")
public class MigrationTask {

    @Id
    @Column(name = "guid")
    private String guid;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "start_created_time", nullable = false)
    private LocalDateTime startCreatedTime;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "schema_name", nullable = false)
    private String schemaName;
}
