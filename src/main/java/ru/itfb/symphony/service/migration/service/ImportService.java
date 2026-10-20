package ru.itfb.symphony.service.migration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itfb.symphony.service.migration.jdbc.JdbcTemplateService;
import ru.itfb.symphony.service.migration.model.dto.MigrationTaskDto;
import ru.itfb.symphony.service.migration.pojo.EntityFieldHolder;
import ru.itfb.symphony.service.migration.pojo.EntityHolder;
import ru.itfb.symphony.service.migration.pojo.FileHolderEntity;
import ru.itfb.symphony.service.migration.pojo.UpdatedDataId;
import ru.itfb.symphony.service.migration.schema.ExportEntity;
import ru.itfb.symphony.service.migration.schema.FieldAttribute;
import ru.itfb.symphony.service.migration.schema.SchemaProcessorService;
import ru.itfb.symphony.service.migration.schema.TransportSchema;
import ru.itfb.symphony.service.migration.utills.EntityHolderUtils;
import ru.itfb.symphony.service.migration.utills.FileEntityHolderService;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final FileEntityHolderService fileEntityHolderService;
    private final JdbcTemplateService jdbcTemplateService;
    private final SchemaProcessorService schemaProcessorService;
    private final MigrationTaskService migrationTaskService;

    @Async
    @Transactional
    public void importEntityHolder(List<File> directory,
                                      List<TransportSchema> schemaList,
                                      Boolean isCreatedData,
                                      Boolean isUpdateData,
                                      Boolean isCollisionCreateNewEntity,
                                      MigrationTaskDto migrationTaskDto,
                                   String datasourceName
    )  {
//        jdbcTemplateService.createTransferTable(datasourceName);

        List<ExportEntity> exportEntitiesOrderPriority = new ArrayList<>(schemaProcessorService.getAllEntityByListTransportSchema(schemaList));
        Map<ExportEntity, List<EntityHolder>> entityHolderMap = new HashMap<>();
        for (int entityIndex = 0; entityIndex < directory.size(); entityIndex++) {
            createEntityHolderMap(entityHolderMap, exportEntitiesOrderPriority, directory.get(entityIndex), entityIndex);
        }

        List<ExportEntity> entitiesWithDoubleReferenceLink = new ArrayList<>();
        checkExportEntityDoubleLink(entityHolderMap, entitiesWithDoubleReferenceLink);

        importEntityHoldersByPriority(entityHolderMap, exportEntitiesOrderPriority, isCreatedData, isUpdateData, isCollisionCreateNewEntity, datasourceName);

        Map<ExportEntity, List<EntityHolder>> entityHoldersDoubleLinkMap = new HashMap<>();
        if (!entitiesWithDoubleReferenceLink.isEmpty()) {

            for (int entityIndex = 0; entityIndex < directory.size(); entityIndex++) {
                createEntityHolderMap(entityHoldersDoubleLinkMap, exportEntitiesOrderPriority, directory.get(entityIndex), entityIndex);
            }
        }

        if (!entitiesWithDoubleReferenceLink.isEmpty()) {
            enrichDoubleLinkColumn(entityHoldersDoubleLinkMap, entitiesWithDoubleReferenceLink, datasourceName);
        }

        directory.clear();

//        jdbcTemplateService.dropTransferTable(datasourceName);

        migrationTaskService.updateStatusReady(migrationTaskDto.getGuid());
        log.info("migration task completed guid: {}", migrationTaskDto.getGuid());
    }

    private void importEntityHoldersByPriority(Map<ExportEntity, List<EntityHolder>> entityHolderMap,
                                               List<ExportEntity> exportEntitiesOrderPriority,
                                                  Boolean isCreatedData,
                                                  Boolean isUpdateData,
                                                  Boolean isCollisionCreateNewEntity,
                                               String datasourceName
    ) {

        AtomicReference<Integer> entityHoldersCount = new AtomicReference<>(0);
        int count = 0;
        for (ExportEntity exportEntity : exportEntitiesOrderPriority) {
            count++;
            List<EntityHolder> entityHolders = entityHolderMap.get(exportEntity);
            if (isCreatedData) {
                entityHoldersCount.updateAndGet(v -> v + createEntityHolder(entityHolders, isCollisionCreateNewEntity, exportEntity, datasourceName));

            }
            if (isUpdateData) {
                entityHoldersCount.updateAndGet(v -> v + updateEntityHolder(entityHolders, exportEntity, datasourceName));
            }
        }

    }

    private void checkExportEntityDoubleLink(Map<ExportEntity, List<EntityHolder>> entityHolderMap, List<ExportEntity> entitiesWithDoubleReferenceLink) {
        List<ExportEntity> entities = new ArrayList<>(entityHolderMap.keySet());
        entities.forEach(exportEntity -> {
            schemaProcessorService.searchDoubleTypeFromExportEntity(exportEntity, entitiesWithDoubleReferenceLink);
        });
    }

    private void enrichDoubleLinkColumn(Map<ExportEntity, List<EntityHolder>> entityHolderMap, List<ExportEntity> entitiesWithDoubleReferenceLink, String datasourceName) {
        for (ExportEntity exportEntity : entitiesWithDoubleReferenceLink) {
            List<EntityHolder> entityHolders = entityHolderMap.get(exportEntity);
            updateEntityHolder(entityHolders, exportEntity, datasourceName);
        }
    }

    private void createEntityHolderMap(Map<ExportEntity, List<EntityHolder>> entityHolderMap, List<ExportEntity> exportEntitiesOrderPriority, File directory, int index) {
        List<FileHolderEntity> fileHolderEntities = new ArrayList<>(fileEntityHolderService.takeDataFromFile(directory));
        int size = fileHolderEntities.size();
        for (int entityIndex = 0; entityIndex < size; entityIndex++) {
            List<EntityHolder> entityHolders = fileEntityHolderService.takeDataFromFile(directory).get(entityIndex).getEntityHolders();
            entityHolderMap.put(exportEntitiesOrderPriority.get(index), entityHolders);
        }
    }

    public List<TransportSchema> getSchemaFromImportDirectory(List<File> files) {
        List<TransportSchema> schemaList = new ArrayList<>();
        for (File file: files) {
            schemaList.add(fileEntityHolderService.takeDataFromFile(file).get(0).getFileMetaData().getSchema());
        }
        return schemaList;
    }

    private Integer updateEntityHolder(List<EntityHolder> allEntityHolder, ExportEntity exportEntity, String datasourceName) {
        jdbcTemplateService.updateEntities(allEntityHolder, exportEntity, datasourceName);
        return allEntityHolder.size();
    }

    private Integer createEntityHolder(List<EntityHolder> entityHolders,
                                       Boolean isCollisionCreateNewEntity,
                                       ExportEntity exportEntity,
                                       String datasourceName
    ) {
        Integer count = 0;
        Integer resultCountNewEntityHolder = 0;
        List<EntityHolder> filteredEntityHolderByLevel = new ArrayList<>();

        if (exportEntity.getUnique()) {
            isCollisionCreateNewEntity = false;
        }

        // блок по удалению сущностей по id, которые уже лежат в таблице
        if (!isCollisionCreateNewEntity) {
            List<String> allEntityHolderIds = new ArrayList<>(jdbcTemplateService.getAllIdsFromTableByEntity(exportEntity, datasourceName));
            entityHolders.removeIf(entityHolder -> allEntityHolderIds
                    .contains(entityHolder.getEntity()
                            .get(0)
                            .get(SchemaProcessorService.searchNameIdColumnInSchema(exportEntity))
                            .getValue()));
        }

        // блок по удалению сущностей по unique column, которые уже лежат в таблице
        if (isCollisionCreateNewEntity && exportEntity.getUniqueColumn() != null) {
            List<String> allEntityHolderUniqueColumn = new ArrayList<>(jdbcTemplateService.getAllUniqueColumnFromTableByEntity(exportEntity, datasourceName));
            Integer columnPosition = jdbcTemplateService.createQuerySelectPositionColumn(exportEntity.getName(), exportEntity.getUniqueColumn(), datasourceName);
            entityHolders.removeIf(entityHolder -> allEntityHolderUniqueColumn
                    .contains(entityHolder.getEntity()
                            //номер колонки
                            .get(columnPosition)
                            //имя колонки
                            .get(exportEntity.getUniqueColumn())
                            .getValue()));
        }

        List<String> nameParentColumns = schemaProcessorService.searchParentColumnInSchema(exportEntity);

        // блок по простому созданию сущностей без парент айди
        if (nameParentColumns.isEmpty() && !isCollisionCreateNewEntity) {
            jdbcTemplateService.saveAllEntityHolder(entityHolders, isCollisionCreateNewEntity, exportEntity, datasourceName);
            return entityHolders.size();
        }

        // блок по созданию сущностей с парентами
        for (String nameParentColumn : nameParentColumns) {
            FieldAttribute attribute = exportEntity.getAttributes().get(nameParentColumn);
            String entityColumnPath = attribute.getLink() + attribute.getColumnLinkName();
            do {
                Map<String, String> transferMap = jdbcTemplateService.getTransferMap(entityColumnPath);
                filteredEntityHolderByLevel = getEntityHoldersByLevel(entityHolders, count);

                filteredEntityHolderByLevel.forEach(entityHolder -> {
                    entityHolder.getEntity().forEach(field -> {
                        EntityFieldHolder parentIdField = field.get(nameParentColumn);
                        if (parentIdField != null) {
                            String newParentId = transferMap.get(parentIdField.getValue());
                            field.get(nameParentColumn).setValue(newParentId);
                        }
                    });
                });

                UpdatedDataId updatedData = jdbcTemplateService.saveAllEntityHolder(filteredEntityHolderByLevel, isCollisionCreateNewEntity, exportEntity, datasourceName);

                updateTransferMap(updatedData, transferMap);
                resultCountNewEntityHolder += filteredEntityHolderByLevel.size();
                count++;
            } while (entityHolders.size() != resultCountNewEntityHolder);
        }

        //todo поправить для генерации id(поправить фильтр по иерархии)
        if (isCollisionCreateNewEntity) {
            jdbcTemplateService.saveAllEntityHolder(entityHolders, isCollisionCreateNewEntity, exportEntity, datasourceName);
        }

        return resultCountNewEntityHolder;
    }

    private List<EntityHolder> getEntityHoldersByLevel(List<EntityHolder> allEntityHolder, Integer count) {
        String levelCount = String.valueOf(count);
        return allEntityHolder.stream().filter(entity -> EntityHolderUtils.getLastField(entity.getEntity()).get("level").getValue().equals(levelCount)).toList();
    }

    private void updateTransferMap(UpdatedDataId updatedData, Map<String, String> transferMap) {
        Map<String, String> updatedMap = updatedData.getTransferMap();
        transferMap.putAll(updatedMap);
    }
}
