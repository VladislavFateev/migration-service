package ru.itfb.symphony.service.migration.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MigrationTaskDto {

    private String guid;
    private String createdBy;
    private LocalDateTime startCreatedTime;
    private LocalDateTime createdTime;
    private String status;
    private String taskType;
    private String schemaName;
}
