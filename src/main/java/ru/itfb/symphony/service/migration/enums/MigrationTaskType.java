package ru.itfb.symphony.service.migration.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MigrationTaskType {
    EXPORT("export"),
    IMPORT("import");

    private final String taskType;
}
