package ru.itfb.symphony.service.migration.model.dto;

import lombok.Data;

@Data
public class ExportSchemaDto {

    private String fullPathToTable;
    private String exportSchema;
    private String version;
}
