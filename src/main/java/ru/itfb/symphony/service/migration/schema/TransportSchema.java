package ru.itfb.symphony.service.migration.schema;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Data
public class TransportSchema {

    private String dataSourceName;
    private String schemaName;
    private List<String> priority;
    private ExportEntity exportEntity;
}
