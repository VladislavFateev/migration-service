package ru.itfb.symphony.service.migration.schema;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itfb.symphony.service.migration.enums.DataType;
import ru.itfb.symphony.service.migration.enums.FieldType;
import ru.itfb.symphony.service.migration.enums.LinkType;
import ru.itfb.symphony.service.migration.json.JsonConverter;
import ru.itfb.symphony.service.migration.service.SchemaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaProcessorService {

    private final JsonConverter jsonMapper;
    private final SchemaService schemaService;

    public TransportSchema createTransportSchemaByJson(String path) {
        String jsonText = getJsonTextFromSchema(path);
        return jsonMapper.toSchema(jsonText);
    }

    public TransportSchema createTransportSchemaByContext(String context) {
        return jsonMapper.toSchema(context);
    }

    private String getJsonTextFromSchema(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            log.error("Failed to read json from schema", e);
            throw new RuntimeException(e);
        }
    }

    public static String searchNameIdColumnInSchema(ExportEntity exportEntity) {
        List<String> fields = new ArrayList<>();
        exportEntity.getAttributes().forEach((key, value) -> {
            if (value.getDataType() != null && (value.getDataType().equals(DataType.GUID) || value.getDataType().equals(DataType.ID))) {
                fields.add(key);
            }
        });
        if (fields.isEmpty()) {
            log.error("No fields parent id in schema!");
        }
        return fields.get(0);
    }

    public List<String> searchParentColumnInSchema(ExportEntity exportEntity) {
        List<String> fields = new ArrayList<>();
        exportEntity.getAttributes().forEach((key, value) -> {
            if (value.getDataType() != null && value.getLinkType() != null && value.getDataType().equals(DataType.PARENT_ID)) {
                fields.add(key);
            }
        });
        return fields;
    }

    public List<String> searchParentSelfLinkColumnInSchema(ExportEntity exportEntity) {
        List<String> fields = new ArrayList<>();
        exportEntity.getAttributes().forEach((key, value) -> {
            if (value.getDataType() != null && value.getLinkType() == LinkType.SELF_LINK && value.getDataType().equals(DataType.PARENT_ID)) {
                fields.add(key);
            }
        });
        return fields;
    }

    public void searchDoubleTypeFromExportEntity(ExportEntity exportEntity, List<ExportEntity> entitiesWithDoubleReferenceLink) {
        exportEntity.getAttributes().forEach((key, value) -> {
            if (value.getDataType() != null && value.getLinkType() != null && value.getLinkType().equals(LinkType.DOUBLE_LINK)) {
                entitiesWithDoubleReferenceLink.add(exportEntity);
            }
        });
    }

    public List<String> searchJsonColumnInSchema(ExportEntity exportEntity) {
        List<String> fields = new ArrayList<>();
        exportEntity.getAttributes().forEach((key, value) -> {
            if (value.getDataType() != null && value.getFieldType().equals(FieldType.JSONB_TYPE)) {
                fields.add(key);
            }
        });
        return fields;
    }

    public List<String> searchUuidColumnInSchema(ExportEntity exportEntity) {
        List<String> fields = new ArrayList<>();
        exportEntity.getAttributes().forEach((key, value) -> {
            if (value.getDataType() != null && value.getFieldType().equals(FieldType.UUID_TYPE)) {
                fields.add(key);
            }
        });
        return fields;
    }

    public List<String> searchColumnByteaInSchema(ExportEntity exportEntity) {
        List<String> fields = new ArrayList<>();
        exportEntity.getAttributes().forEach((key, value) -> {
            if (value.getDataType() != null && value.getFieldType().equals(FieldType.BYTEA_TYPE)) {
                fields.add(key);
            }
        });
        return fields;
    }

    public ExportEntity searchEntityByPathToTable(List<ExportEntity> entities, String pathToTable) {
        return entities.stream().filter(entity -> entity.getPathToTable().equals(pathToTable)).toList().get(0);
    }

    private List<String> getListPriorityByPathToTable(String fullPathToTable) {
        TransportSchema schema = jsonMapper.toSchema(schemaService.getSchemaJsonByFullPathToTable(fullPathToTable));
        return schema.getPriority();
    }

    public List<TransportSchema> getAllTransportSchemaByFullPathToTable(String fullPathToTable) {
        List<String> listPriority = getListPriorityByPathToTable(fullPathToTable);
        List<String> listSchemaJson = schemaService.getAllSchemaJsonByFullPathToTable(listPriority);
        List<TransportSchema> listTransportSchemas = new ArrayList<>();
        listSchemaJson.forEach(json -> {
            listTransportSchemas.add(jsonMapper.toSchema(json));
        });
        return listTransportSchemas;
    }

    public List<ExportEntity> getAllEntityByListTransportSchema(List<TransportSchema> listTransportSchema) {
        List<ExportEntity> listEntities = new ArrayList<>();
        listTransportSchema.forEach(schema -> {
            listEntities.add(schema.getExportEntity());
        });
        return listEntities;
    }

    public TransportSchema getSchemaByFullPathToTable(String fullPathToTable) {
        String schemaJson = schemaService.getSchemaJsonByFullPathToTable(fullPathToTable);
        return jsonMapper.toSchema(schemaJson);
    }
}
