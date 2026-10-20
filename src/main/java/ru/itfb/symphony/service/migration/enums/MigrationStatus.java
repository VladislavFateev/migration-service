package ru.itfb.symphony.service.migration.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MigrationStatus {

    NOT_READY("in progress"),
    ERROR("failed"),
    READY("success");

    private final String status;

    }
