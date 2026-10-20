package ru.itfb.symphony.service.migration.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.itfb.symphony.service.migration.component.DataSourceRegistry;
import ru.itfb.symphony.service.migration.enums.DataType;
import ru.itfb.symphony.service.migration.enums.FieldType;
import ru.itfb.symphony.service.migration.enums.LinkType;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor

public class JdbcSchemaGenerator {

    private final DataSourceRegistry dataSourceRegistry;

    public String generateSchemaJson(String datasourceName, String schema, String table) throws Exception {

        Map<String, Object> exportEntity = new HashMap<>();
        List<String> alreadyGenerated = new ArrayList<>();

        List<String> priority = new ArrayList<>();

        Connection connection = dataSourceRegistry.getDataSource(datasourceName).getDataSource().getConnection();

        generateEntity(alreadyGenerated, exportEntity, table, priority, schema, connection);
        String dataSourceName = connection.getCatalog();

        Map<String, Object> migration_schema = new HashMap<>();
        migration_schema.put("priority", priority);
        migration_schema.put("exportEntity", exportEntity);
        migration_schema.put("dataSourceName", dataSourceName);
        migration_schema.put("schemaName", schema);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(migration_schema);
    }
    private void generateEntity(List<String> alreadyGenerated, Map<String, Object> exportEntity, String table, List<String> priority, String schema, Connection connection) throws SQLException {
        if(alreadyGenerated.contains(table)){
            return;
        }
        alreadyGenerated.add(table);

        connection.setSchema(schema);
        String dbSchema = connection.getSchema();

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, dbSchema, table, new String[]{"TABLE"});
        tables.next();
        String tableName = tables.getString("TABLE_NAME");
        String fullPath = dbSchema + "." + tableName;

        exportEntity.put("name", tableName);
        exportEntity.put("unique", true);
        exportEntity.put("pathToTable", fullPath);
        priority.add(fullPath);

        Map<String, Object> attributes = new LinkedHashMap<>();

        ResultSet columns = metaData.getColumns(null, null, tableName, null);

        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String dataType = columns.getString("TYPE_NAME");

            Map<String, Object> attribute = new HashMap<>();
            attribute.put("dataType", DataType.SIMPLE);
            attribute.put("fieldType", mapFieldType(dataType));

            attributes.put(columnName, attribute);
        }

        ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, tableName);
        while (primaryKeys.next()) {
            String pkColumnName = primaryKeys.getString("COLUMN_NAME");

            if (attributes.containsKey(pkColumnName)) {
                Map<String, Object> pkAttribute = (Map<String, Object>) attributes.get(pkColumnName);
                pkAttribute.put("isPk", true);
                pkAttribute.put("dataType", DataType.ID);
                pkAttribute.put("isGenerateOnDbSide", false);
            }
        }

        ResultSet foreignKeys = metaData.getImportedKeys(null, null, tableName);
        while (foreignKeys.next()) {
            String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");

            String fkTableName = foreignKeys.getString("PKTABLE_NAME");
            String fkPrimaryColumnName = foreignKeys.getString("PKCOLUMN_NAME");
            if (attributes.containsKey(fkColumnName)) {
                Map<String, Object> fkAttribute = (Map<String, Object>) attributes.get(fkColumnName);
                fkAttribute.put("dataType", DataType.PARENT_ID);
                if (tableName.equals(fkTableName)) {
                    fkAttribute.put("linkType", LinkType.SELF_LINK);
                } else {
                    fkAttribute.put("linkType", LinkType.OUTER_LINK);
                    fkAttribute.put("link", dbSchema + "." + fkTableName);
                    fkAttribute.put("columnLinkName", fkPrimaryColumnName);
                    priority.add(dbSchema + "." + fkTableName);
                }
            }
        }
        exportEntity.put("attributes", attributes);
        while (tables.next()) {
            String name = tables.getString("TABLE_NAME");
            String path = dbSchema + "." + name;
            priority.add(path);
        }
    }

    private FieldType mapFieldType(String sqlType) {
        switch (sqlType.toUpperCase()) {
            case "CHARACTER VARYING":
            case "VARCHAR":
            case "TEXT":
                return FieldType.STRING_TYPE;

            case "UUID":
                return FieldType.UUID_TYPE;

            case "JSONB":
                return FieldType.JSONB_TYPE;

            case "INT8", "BIGINT", "BIGSERIAL":
                return FieldType.LONG_TYPE;

            case "INTEGER", "INT4":
                return FieldType.INTEGER_TYPE;

            case "BOOLEAN":
            case "BOOL":
                return FieldType.BOOLEAN_TYPE;

            case "DATE":
                return FieldType.DATE_TYPE;

            case "NUMERIC":
                return FieldType.DOUBLE_TYPE;

            case "SMALLINT":
            case "INT2":
                return FieldType.SHORT_TYPE;

            case "TIMESTAMP WITHOUT TIME ZONE":
            case "TIMESTAMP":
                return FieldType.TIMESTAMP_TYPE;

            default:
                log.error("Unknown type: " + sqlType.toUpperCase());
                return FieldType.STRING_TYPE;
        }
    }
}
