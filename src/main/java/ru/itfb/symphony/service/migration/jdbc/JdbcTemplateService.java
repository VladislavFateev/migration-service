package ru.itfb.symphony.service.migration.jdbc;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import ru.itfb.symphony.service.migration.component.DataSourceRegistry;
import ru.itfb.symphony.service.migration.config.JdbcConfig;
import ru.itfb.symphony.service.migration.cte.HierarchyHolderEntity;
import ru.itfb.symphony.service.migration.enums.DataType;
import ru.itfb.symphony.service.migration.enums.FieldType;
import ru.itfb.symphony.service.migration.exception.ExportException;
import ru.itfb.symphony.service.migration.mapper.RowEntityHolderMapper;
import ru.itfb.symphony.service.migration.pojo.EntityFieldHolder;
import ru.itfb.symphony.service.migration.pojo.EntityHolder;
import ru.itfb.symphony.service.migration.pojo.UpdatedDataId;
import ru.itfb.symphony.service.migration.schema.ExportEntity;
import ru.itfb.symphony.service.migration.schema.FieldAttribute;
import ru.itfb.symphony.service.migration.schema.SchemaProcessorService;
import ru.itfb.symphony.service.migration.utills.EntityHolderUtils;
import ru.itfb.symphony.service.migration.utills.QueryBuilderUtils;

import java.sql.Date;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ru.itfb.symphony.service.migration.utills.QueryBuilderUtils.createQueryInsertInto;

@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcTemplateService {

    private final SchemaProcessorService schemaProcessorService;
    private final DateTimeFormatter dateFormatterTimestamp = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private JdbcConfig jdbcConfig;
    private final DataSourceRegistry dataSourceRegistry;

    public List<EntityHolder> findAllEntityHoldersByEntity(ExportEntity exportEntity, Map<ExportEntity, Set<String>> filtredMap, String datasourceName) {
        JdbcTemplate jdbcTemplateExport = dataSourceRegistry.getDataSource(datasourceName);
        String pathDataBase = exportEntity.getPathToTable();
        List<EntityHolder> entityHolders = new ArrayList<>();
        String sql = QueryBuilderUtils.createQueryFindAll(exportEntity, pathDataBase);
        if (!filtredMap.isEmpty()) {
            String columnIdName = SchemaProcessorService.searchNameIdColumnInSchema(exportEntity);

            if (filtredMap.get(exportEntity) != null) {
                FieldType type = exportEntity.getAttributes().get(columnIdName).getFieldType();
                List<String> columnIds = new ArrayList<>(filtredMap.get(exportEntity));
                try {

                    sql += QueryBuilderUtils.createQueryWhereEnumerationIds(columnIdName, columnIds, type);
                    entityHolders = jdbcTemplateExport.query(sql, new RowEntityHolderMapper());
                } catch (ExportException e) {
                    log.error("не найдены ids");
                }
            }
        } else {
            entityHolders = jdbcTemplateExport.query(sql, new RowEntityHolderMapper());
        }
        return entityHolders;
    }

    public Map<String, String> getTransferMap(String entityColumnPath) {
        JdbcTemplate jdbcTemplateImport = jdbcConfig.jdbcTemplate();
        String sql = QueryBuilderUtils.createQueryGetTransferMapByNameColumn(entityColumnPath);
        return jdbcTemplateImport.query(sql, (ResultSetExtractor<Map<String, String>>) rs -> {
            HashMap<String, String> transferMap = new HashMap<>();
            while (rs.next()) {
                transferMap.put(rs.getString("old_id"), rs.getString("new_id"));
            }
            return transferMap;
        });
    }

    public void getParentIds(List<ExportEntity> entities,
                             ExportEntity exportEntity,
                             List<String> ids,
                             Map<ExportEntity, Set<String>> filtredIdsMap,
                             String datasourceName
                             ) {
        List<String> parentColumnNames = schemaProcessorService.searchParentColumnInSchema(exportEntity);
        Map<String, FieldAttribute> attributes = exportEntity.getAttributes();
        String columnIdName = SchemaProcessorService.searchNameIdColumnInSchema(exportEntity);
        String pathDataBase = exportEntity.getPathToTable();
        for (String parentColumnName : parentColumnNames) {
            FieldType type = attributes.get(parentColumnName).getFieldType();
            Set<String> parentIds = new HashSet<>();
            try {

                String sql = QueryBuilderUtils.createQueryGetParentIdsByColumnName(parentColumnName, pathDataBase, columnIdName, ids, type);
                List<String> result = dataSourceRegistry.getDataSource(datasourceName).queryForList(sql, String.class);
                if (!result.isEmpty()) {
                    parentIds.addAll(dataSourceRegistry.getDataSource(datasourceName).queryForList(sql, String.class));
                }
            } catch (ExportException e) {
                log.warn("не найдены ids у таблицы");
            }

            String pathToTable = attributes.get(parentColumnName).getLink();
            ExportEntity parentExportEntity = schemaProcessorService.searchEntityByPathToTable(entities, pathToTable);
            filtredIdsMap.put(parentExportEntity, parentIds);
        }
    }

    public List<HierarchyHolderEntity> getRecursiveHierarchyHolderEntity(Integer limit, Integer offset, ExportEntity exportEntity, String datasourceName) {
        String pathDataBase = exportEntity.getPathToTable();
        // В будущем может быть несколько parentColumn с рекурсивной ссылкой
        String parentColumn = schemaProcessorService.searchParentSelfLinkColumnInSchema(exportEntity).get(0);
        String id = SchemaProcessorService.searchNameIdColumnInSchema(exportEntity);

        String sql = QueryBuilderUtils.createQueryRecursiveHierarchyEntityHolder(pathDataBase, id, parentColumn, limit, offset);
        return dataSourceRegistry.getDataSource(datasourceName).query(sql, new BeanPropertyRowMapper<>(HierarchyHolderEntity.class));
    }

    public void updateEntities(List<EntityHolder> entityHolders, ExportEntity exportEntity, String datasourceName) {
        LinkedHashMap<String, FieldType> columnsMap = findNameAndTypeColumns(exportEntity);
        List<String> columnsName = new ArrayList<>(columnsMap.keySet());

        String columnIdName = SchemaProcessorService.searchNameIdColumnInSchema(exportEntity);
        String pathDataBase = exportEntity.getPathToTable();

        List<String> jsonbColumnNames = new ArrayList<>(schemaProcessorService.searchJsonColumnInSchema(exportEntity));
        List<String> uuidColumnNames = new ArrayList<>(schemaProcessorService.searchUuidColumnInSchema(exportEntity));
        String sql = QueryBuilderUtils.createQueryUpdate(columnsMap, pathDataBase, columnIdName, jsonbColumnNames, uuidColumnNames);
        dataSourceRegistry.getDataSource(datasourceName).batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                int countColumns = columnsName.size();

                for (int columnIndex = 0; columnIndex < countColumns; columnIndex++) {

                    String columnName = columnsName.get(columnIndex);
                    EntityFieldHolder field = entityHolders.get(i).getEntity().get(columnIndex).get(columnName);

                    //при генерации квери id должна быть последней
                    int index = columnIndex;
                    if (columnName.equals(columnIdName)) {
                        index = countColumns;
                    }

                    FieldType fieldType = columnsMap.get(columnName);
                    enrichEntityFieldHolder(ps, field, fieldType, index);
                }
            }

            @Override
            public int getBatchSize() {
                return entityHolders.size();
            }
        });
    }

    public UpdatedDataId saveAllEntityHolder(List<EntityHolder> entityHolders, Boolean isCollisionCreateNewEntity, ExportEntity exportEntity, String datasourceName) {
        Integer sequenceIndex = null;
        if (exportEntity.getSequence() != null) {
            sequenceIndex = Integer.valueOf(getNextIdBySequence(exportEntity, datasourceName));
            Integer newEntitiesSize = entityHolders.size();
            if (newEntitiesSize > 0) {
                newEntitiesSize -= 1;
            }
            Integer newSequenceIndex = sequenceIndex + newEntitiesSize;
            updateSequence(exportEntity, newSequenceIndex, datasourceName);
        }
        Integer finalSequenceIndex = sequenceIndex;

        String columnIdName = SchemaProcessorService.searchNameIdColumnInSchema(exportEntity);
        List<String> jsonbColumnNames = new ArrayList<>(schemaProcessorService.searchJsonColumnInSchema(exportEntity));
        List<String> uuidColumnNames = new ArrayList<>(schemaProcessorService.searchUuidColumnInSchema(exportEntity));
        List<String> byteaColumnNames = new ArrayList<>(schemaProcessorService.searchColumnByteaInSchema(exportEntity));
        LinkedHashMap<String, FieldType> columnsMap = findNameAndTypeColumns(exportEntity);
        List<String> columnsName = new ArrayList<>(columnsMap.keySet());
        FieldType fieldTypeId = columnsMap.get(columnIdName);

        String sql = createQueryInsertInto(columnsMap, exportEntity.getPathToTable(), jsonbColumnNames, uuidColumnNames, byteaColumnNames);

        FieldAttribute fieldAttributeId = exportEntity.getAttributes().get(columnIdName);
        DataType dataType = fieldAttributeId.getDataType();
        Boolean isGenerateOnDbSide = fieldAttributeId.getIsGenerateOnDbSide();

        dataSourceRegistry.getDataSource(datasourceName).batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                for (int columnIndex = 0; columnIndex < columnsMap.size(); columnIndex++) {
                    int index = columnIndex + 1;
                    String columnName = columnsName.get(columnIndex);
                    EntityFieldHolder field = entityHolders.get(i).getEntity().get(columnIndex).get(columnName);

                    if (columnName.equals(columnIdName)){
                        updateDataId(columnIdName, field, isCollisionCreateNewEntity, fieldTypeId, dataType, isGenerateOnDbSide, i, finalSequenceIndex, exportEntity, datasourceName);
                    }

                    FieldType fieldType = columnsMap.get(columnName);
                    enrichEntityFieldHolder(ps, field, fieldType, index);
                }
            }

            @Override
            public int getBatchSize() {
                return entityHolders.size();
            }
        });

        return createUpdateDateId(fieldTypeId);
    }

    private void enrichEntityFieldHolder(PreparedStatement ps, EntityFieldHolder field, FieldType fieldType, int columnIndex) throws SQLException {
        switch (fieldType) {

            case BOOLEAN_TYPE:
                if (field.getValue() != null) {
                    ps.setBoolean(columnIndex, EntityHolderUtils.convertStringToBoolean(field.getValue()));
                } else {
                    ps.setNull(columnIndex, Types.BOOLEAN);
                }
                break;
            case DATE_TYPE:
                ps.setDate(columnIndex, field.getValue() != null ? toDate(field.getValue()) : null);
                break;
            case TIMESTAMP_TYPE:
                ps.setTimestamp(columnIndex, field.getValue() != null ? toTimestamp(field.getValue()) : null);
                break;
            case INTEGER_TYPE:
                if (field.getValue() != null) {
                    ps.setInt(columnIndex, Integer.parseInt(field.getValue()));
                } else {
                    ps.setNull(columnIndex, Types.INTEGER);
                }
                break;
            case STRING_TYPE, JSONB_TYPE, UUID_TYPE, BYTEA_TYPE:
                ps.setString(columnIndex, field.getValue() != null ? field.getValue() : null);
                break;
            case DOUBLE_TYPE:
                if (field.getValue() != null) {
                    ps.setDouble(columnIndex, Double.parseDouble(field.getValue()));
                } else {
                    ps.setNull(columnIndex, Types.DOUBLE);
                }
                break;
            case FLOAT_TYPE:
                if (field.getValue() != null) {
                    ps.setFloat(columnIndex, Float.parseFloat(field.getValue()));
                } else {
                    ps.setNull(columnIndex, Types.FLOAT);
                }
                break;
            case LONG_TYPE:
                if (field.getValue() != null) {
                    ps.setLong(columnIndex, Long.parseLong(field.getValue()));
                } else {
                    ps.setNull(columnIndex, Types.BIGINT);
                }
                break;
            case SHORT_TYPE:
                if (field.getValue() != null) {
                    ps.setShort(columnIndex, Short.parseShort(field.getValue()));
                } else {
                    ps.setNull(columnIndex, Types.SMALLINT);
                }
                break;
            case BYTE_TYPE:
                ps.setByte(columnIndex, field.getValue() != null ? Byte.valueOf(field.getValue()) : null);
                break;
            case CHAR_TYPE:
                ps.setCharacterStream(columnIndex, null);
                break;
        }
    }

    public List<String> getAllIdsFromTableByEntity(ExportEntity exportEntity, String datasourceName) {
        String columnIdName = SchemaProcessorService.searchNameIdColumnInSchema(exportEntity);
        String sql = QueryBuilderUtils.createQuerySelectAllIds(columnIdName, exportEntity.getPathToTable());
        return dataSourceRegistry.getDataSource(datasourceName).queryForList(sql, String.class);
    }

    public List<String> getAllUniqueColumnFromTableByEntity(ExportEntity exportEntity, String datasourceName) {
        String uniqueColumnName = exportEntity.getUniqueColumn();
        String sql = QueryBuilderUtils.createQuerySelectAllByUniqueColumn(uniqueColumnName, exportEntity.getPathToTable());
        return dataSourceRegistry.getDataSource(datasourceName).queryForList(sql, String.class);
    }

    public Integer createQuerySelectPositionColumn(String tableName, String columnName, String datasourceName) {
        String sql = QueryBuilderUtils.createQuerySelectPositionColumn(tableName, columnName);
        return dataSourceRegistry.getDataSource(datasourceName).queryForObject(sql, Integer.class);
    }

    private LinkedHashMap<String, FieldType> findNameAndTypeColumns(ExportEntity exportEntity) {
        LinkedHashMap<String, FieldType> nameAndTypeColumns = new LinkedHashMap<>();
        exportEntity.getAttributes()
                .forEach((key, value) -> nameAndTypeColumns.put(key, value.getFieldType()));
        return nameAndTypeColumns;
    }

    private Timestamp toTimestamp(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        LocalDateTime parsedDate = LocalDateTime.parse(date.trim().replaceAll("\\s+", "T"), dateFormatterTimestamp);
        return Timestamp.valueOf(parsedDate);
    }

    private Date toDate(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        return Date.valueOf(date);
    }

    private UpdatedDataId createUpdateDateId(FieldType fieldTypeId) {
        UpdatedDataId updatedData = new UpdatedDataId();
        Map<String, String> transferMap = new HashMap<>();

        updatedData.setFieldType(fieldTypeId);
        updatedData.setTransferMap(transferMap);
        return updatedData;
    }

    private void updateDataId(
            String columnId,
            EntityFieldHolder field,
            Boolean isCollisionCreateNewEntity,
            FieldType fieldTypeId,
            DataType dataType,
            Boolean isGenerateOnDbSide,
            int index,
            Integer sequenceIndex,
            ExportEntity exportEntity,
            String datasourceName
    ) {

        String old = field.getValue() != null ? field.getValue() : null;
        if (isCollisionCreateNewEntity && !isGenerateOnDbSide && exportEntity.getSequence() == null) {

            switch (fieldTypeId) {
                case STRING_TYPE:
                    if (dataType.equals(DataType.GUID)){
                        field.setValue(UUID.randomUUID().toString());
                    }
                    if (dataType.equals(DataType.ID)) {
                        Integer id = getNextId(FieldType.STRING_TYPE, index, exportEntity, datasourceName);
                        field.setValue(String.valueOf(id));
                    }
                    break;
                case LONG_TYPE, INTEGER_TYPE:
                    Integer id = getNextId(FieldType.INTEGER_TYPE, index, exportEntity, datasourceName);
                    field.setValue(String.valueOf(id));
                    break;
            }
        }

        if (isCollisionCreateNewEntity && exportEntity.getSequence() != null) {
            field.setValue(String.valueOf(sequenceIndex + index));
        }
        String columnLink = exportEntity.getPathToTable() + columnId;

        setTransferMap(columnLink, old, field.getValue());
    }

    private String getNextIdBySequence(ExportEntity exportEntity, String datasourceName) {
        String sql = QueryBuilderUtils.createQuerySelectNextSequence(exportEntity);
        return dataSourceRegistry.getDataSource(datasourceName).queryForObject(sql, String.class);
    }

    private void updateSequence(ExportEntity exportEntity, Integer sequenceIndex, String datasourceName) {
        String sql = QueryBuilderUtils.createQueryUpdateSequence(exportEntity, sequenceIndex);
        dataSourceRegistry.getDataSource(datasourceName).queryForObject(sql, String.class);
    }

    private String getLastIdFromTableByIntegerId(ExportEntity exportEntity, FieldType type, String datasourceName) {
        String pathDataBase = exportEntity.getPathToTable();
        String columnIdName = SchemaProcessorService.searchNameIdColumnInSchema(exportEntity);
        String sql = QueryBuilderUtils.createQuerySelectLastId(pathDataBase, columnIdName, type);

        return dataSourceRegistry.getDataSource(datasourceName).queryForObject(sql, String.class);
    }

    private Integer getNextId(FieldType type, int index, ExportEntity exportEntity, String datasourceName) {
        if (getLastIdFromTableByIntegerId(exportEntity, type, datasourceName) == null) {
            return 1 + index;
        }
        return Integer.parseInt(getLastIdFromTableByIntegerId(exportEntity, type, datasourceName)) + 1 + index;
    }

    public void createTransferTable(String datasourceName) {
        String sql = QueryBuilderUtils.createQueryCreateTransferTable();
        dataSourceRegistry.getDataSource(datasourceName).execute(sql);
    }

    public void dropTransferTable(String datasourceName) {
        String sql = QueryBuilderUtils.createQueryDropTransferTable();
        dataSourceRegistry.getDataSource(datasourceName).execute(sql);
    }

    private void setTransferMap(String columnLink, String oldId, String newId) {
        String sql = QueryBuilderUtils.createQuerySetTransferMap(columnLink, oldId, newId);
        JdbcTemplate jdbcTemplateImport = jdbcConfig.jdbcTemplate();
        jdbcTemplateImport.execute(sql);
    }

}
