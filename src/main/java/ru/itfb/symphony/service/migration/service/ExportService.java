package ru.itfb.symphony.service.migration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itfb.symphony.service.migration.cte.HierarchyHolderEntity;
import ru.itfb.symphony.service.migration.filter.Filter;
import ru.itfb.symphony.service.migration.jdbc.JdbcTemplateService;
import ru.itfb.symphony.service.migration.json.JsonConverter;
import ru.itfb.symphony.service.migration.model.dto.MigrationTaskDto;
import ru.itfb.symphony.service.migration.pojo.EntityFieldHolder;
import ru.itfb.symphony.service.migration.pojo.EntityHolder;
import ru.itfb.symphony.service.migration.pojo.FileHolderEntity;
import ru.itfb.symphony.service.migration.pojo.FileMetaDataEntityHolder;
import ru.itfb.symphony.service.migration.schema.ExportEntity;
import ru.itfb.symphony.service.migration.schema.SchemaProcessorService;
import ru.itfb.symphony.service.migration.schema.TransportSchema;
import ru.itfb.symphony.service.migration.utills.EntityHolderUtils;
import ru.itfb.symphony.service.migration.utills.FileEntityHolderService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    @Value("${export.file.path:}")
    private String exportPathFile;

    @Value("${export.entity.limit:300000}")
    private Integer limit;

    private static final String FILE_NAME = "export-";
    private static final String FORMAT_FILE = ".symphony";

    private final JdbcTemplateService jdbcTemplateService;
    private final SchemaProcessorService schemaProcessorService;
    private final BuildProperties buildProperties;
    private final JsonConverter jsonConverter;
    private final FileEntityHolderService fileEntityHolderService;
    private final FilterService filterService;
    private final MigrationTaskService migrationTaskService;

    @Async
    @Transactional
    public void createExportFile(String schemaName, Filter filter, MigrationTaskDto migrationTaskDto, String datasourceName) {
        Path zipPath = fileEntityHolderService.createZipFile(migrationTaskDto);

        TransportSchema schema = schemaProcessorService.getSchemaByFullPathToTable(schemaName);
        List<TransportSchema> schemaList = new ArrayList<>(schemaProcessorService.getAllTransportSchemaByFullPathToTable(schemaName));

        Map<ExportEntity, Set<String>> filtredMap = new HashMap<>();
        List<ExportEntity> entities = new ArrayList<>(schemaProcessorService.getAllEntityByListTransportSchema(schemaList));
        if (filter.getValues() != null) {
            filtredMap.putAll(filterService.createMapByFilter(filter, schema, datasourceName));
            enrichFilteredMap(entities, filtredMap, datasourceName);
        }

        List<File> files = new ArrayList<>();
        Long count = 0L;

        for (TransportSchema transportSchema : schemaList) {
            files.add(createArchiveWithFiles(transportSchema, count, filtredMap, datasourceName));
            count ++;
        }
        CompletableFuture<File> completableFuture = CompletableFuture.completedFuture(fileEntityHolderService.putFileToZip(zipPath, files));
        completableFuture.thenRun(() -> {
            migrationTaskService.updateStatusReady(migrationTaskDto.getGuid());
            log.info("migration task completed guid: {}", migrationTaskDto.getGuid());
        });
    }

    private void enrichFilteredMap(List<ExportEntity> entities, Map<ExportEntity, Set<String>> filtredIdsMap, String datasourceName) {
        for (int entityIndex = entities.size() - 1; entityIndex > 0 ; entityIndex--) {
            ExportEntity exportEntity = entities.get(entityIndex);
            List<String> filteredId = new ArrayList<>(filtredIdsMap.get(exportEntity));
            jdbcTemplateService.getParentIds(entities, exportEntity, filteredId, filtredIdsMap, datasourceName);
        }
    }

    private File createArchiveWithFiles(TransportSchema schema, Long count, Map<ExportEntity, Set<String>> filtredMap, String datasourceName) {
        File directory = generateDirectory();

        Integer offset = 0;
        ExportEntity exportEntity = schema.getExportEntity();
        List<EntityHolder> entities = jdbcTemplateService.findAllEntityHoldersByEntity(exportEntity, filtredMap, datasourceName);
        Map<String, Integer> transferToIdMap = new HashMap<>();
        List<EntityHolder> entitiesByLimit = new ArrayList<>();

        List<String> recursiveParentColumns = schemaProcessorService.searchParentSelfLinkColumnInSchema(exportEntity);
        if (!recursiveParentColumns.isEmpty()) {
            safelyCreateTransferCteMap(transferToIdMap, entities.size(), exportEntity, datasourceName);
            enrichHierarchyEntityHolder(entities, transferToIdMap);
        }
        List<EntityHolder> sortedEntityHolders = entities.stream()
                .sorted(Comparator.comparing(EntityHolderUtils::getHierarchyLevel
                )).toList();
        do {
             entitiesByLimit = sortedEntityHolders.stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();

            writeTextInFile(generateFile(directory, count), jsonConverter.toJson(createFileEntityHolder(entitiesByLimit, schema)));

            offset += limit;
            count++;
        } while (!entitiesByLimit.isEmpty() && entitiesByLimit.size() == limit);
        return directory;
    }

    private void safelyCreateTransferCteMap(Map<String, Integer> transferToIdMap, int quantityEntityHolder, ExportEntity exportEntity, String datasourceName) {
        int limit = 800000;
        int offset = 0;
        do {

            Map<String, Integer> transfer  = createTransferCteMap(jdbcTemplateService.getRecursiveHierarchyHolderEntity(limit, offset, exportEntity, datasourceName));
            transferToIdMap.putAll(transfer);
            offset += limit;
            if ((offset + limit) > quantityEntityHolder) {
                limit = quantityEntityHolder - offset;
            }
        } while (transferToIdMap.size() < quantityEntityHolder);
    }

    private Map<String, Integer> createTransferCteMap(List<HierarchyHolderEntity> cteHolderEntity) {
        Map<String, Integer> transferToIdMap = new HashMap<>();
        cteHolderEntity.forEach(cte -> {
            transferToIdMap.put(cte.getId(), cte.getLvl());
        });
        return transferToIdMap;
    }

    private FileMetaDataEntityHolder generateMetaData(TransportSchema schema) {
        return new FileMetaDataEntityHolder(buildProperties.getVersion(), LocalDateTime.now(), schema);
    }

    private FileHolderEntity createFileEntityHolder(List<EntityHolder> entityHolders, TransportSchema schema) {
        return new FileHolderEntity(generateMetaData(schema), entityHolders);
    }

    private File writeTextInFile(File file, String jsonString) {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(jsonString);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    private File generateDirectory() {
        File dir = new File(exportPathFile + File.separator + "exp" + UUID.randomUUID());
        dir.mkdir();
        return dir;
    }

    private File generateFile(File directory, Long count) {
        count++;
        return new File(exportPathFile + File.separator + directory.getName() + File.separator + FILE_NAME + count + FORMAT_FILE);
    }

    private void enrichHierarchyEntityHolder(List<EntityHolder> entityHolders, Map<String, Integer> transferToIdMap) {

        entityHolders.forEach(entityHolder -> {
            List<Map<String, EntityFieldHolder>> entity = entityHolder.getEntity();
            String id = EntityHolderUtils.getFistField(entity).get("id").getValue();
                EntityHolderUtils.getLastField(entity).forEach((key, value) -> value.setValue(transferToIdMap.get(id).toString()));
        });
    }
}
