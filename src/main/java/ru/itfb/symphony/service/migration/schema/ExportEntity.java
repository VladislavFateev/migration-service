package ru.itfb.symphony.service.migration.schema;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@RequiredArgsConstructor
public class ExportEntity {

    private String name;
    private Boolean unique;
    private String uniqueColumn;
    private String pathToTable;
    private String sequence;
    private Map<String, FieldAttribute> attributes;
}
