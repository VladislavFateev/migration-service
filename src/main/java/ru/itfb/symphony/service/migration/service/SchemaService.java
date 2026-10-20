package ru.itfb.symphony.service.migration.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itfb.symphony.service.migration.model.dto.ExportSchemaDto;
import ru.itfb.symphony.service.migration.model.domain.Schema;
import ru.itfb.symphony.service.migration.mapper.ExportSchemaMapper;
import ru.itfb.symphony.service.migration.repository.SchemaRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaService {

    private final SchemaRepository schemaRepository;
    private final ExportSchemaMapper mapper;
    private static final String SCHEMA_RESOURCE_PATH = "schemas";

    @PostConstruct
    public void init() {
        initSchemaInTableByResources();
    }

    public List<String> getAllSchemaJsonByFullPathToTable(List<String> priorityList) {
        List<String> listSchemaJson = new ArrayList<>();
        for (String schemaFullPathToTable : priorityList) {
            listSchemaJson.add(getSchemaJsonByFullPathToTable(schemaFullPathToTable));
        }
        return listSchemaJson;
    }

    public String getSchemaJsonByFullPathToTable(String schemaFullPathToTable) {
        return schemaRepository.findByFullPathToTable(schemaFullPathToTable).getExportSchema();
    }

    private void initSchemaInTableByResources() {
        List<String> schemaNamesInTable = getAllSchemaNamesFromTable();
        List<String> allSchemaFullPathToTable = getNotCreatedSchemaNames(schemaNamesInTable);
        List<String> allSchemaJsonFromResources = getAllSchemaJsonFromResources(allSchemaFullPathToTable);
        List<Schema> schemaList = new ArrayList<>();
        for (int schemaIndex = 0; schemaIndex < allSchemaFullPathToTable.size(); schemaIndex++) {
            String str = allSchemaFullPathToTable.get(schemaIndex);
            Pattern p = Pattern.compile(File.separator, Pattern.LITERAL);
            String schemaFullPathToTable = List.of(p.split(str)).getLast();
            Schema schema = Schema.builder()
                    .fullPathToTable(schemaFullPathToTable)
                    .exportSchema(allSchemaJsonFromResources.get(schemaIndex))
                    .version("1")
                    .build();
            schemaList.add(schema);
        }
        schemaRepository.saveAll(schemaList);
    }

    @Transactional
    public List<ExportSchemaDto> createSchemas(List<ExportSchemaDto> exportSchemaDto) {
        List<Schema> schemas =  mapper.dtosToDomains(exportSchemaDto);
        schemaRepository.saveAll(schemas);
        return exportSchemaDto;
    }

    @Transactional
    public ExportSchemaDto createSchema(ExportSchemaDto exportSchemaDto) {
        schemaRepository.save(mapper.dtoToDomain(exportSchemaDto));
        return exportSchemaDto;
    }

    @Transactional
    public List<ExportSchemaDto> updateSchemasInTableByResources() {
        List<ExportSchemaDto> exportSchemaDtoList = new ArrayList<>();
        List<String> allSchemaFullPathToTable = getAllSchemaNamesFromTable();
        List<String> schemaJsom = new ArrayList<>(getAllSchemaJsonFromResources(allSchemaFullPathToTable));
        for (int schemaIndex = 0; schemaIndex < allSchemaFullPathToTable.size(); schemaIndex++) {
            exportSchemaDtoList.add(updateSchemaInTable(allSchemaFullPathToTable.get(schemaIndex), schemaJsom.get(schemaIndex)));
        }
        return exportSchemaDtoList;
    }

    @Transactional
    public ExportSchemaDto deleteSchemaInTable(String schemaFullPathToTable) {
        Schema schema = schemaRepository.findByFullPathToTable(schemaFullPathToTable);
        schemaRepository.delete(schema);
        return mapper.domainToDto(schema);
    }

    private List<String> getAllServicesFromResources() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SCHEMA_RESOURCE_PATH);
        String result;
        try {
            result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return List.of(result.split("\\r?\\n"));
    }

    private Map<String, String> getSchemaNamesSchemaPathFromResourcesMap(List<String> servicesName) {
        List<String> schemaNamesList = new ArrayList<>();
        Map<String, String> schemaTransferPathMap = new HashMap<>();
        for (String serviceName : servicesName) {

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SCHEMA_RESOURCE_PATH + File.separator + serviceName);
            StringBuilder result = new StringBuilder();
            try {
                result.append(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
                schemaNamesList = List.of(result.toString().split("\\r?\\n"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            schemaNamesList.forEach(schemaName -> {
                schemaTransferPathMap.put(schemaName, serviceName + File.separator + schemaName);
            });
        }
        return schemaTransferPathMap;
    }

    public List<String> getAllSchemaJsonFromResources(List<String> allSchemaFullPathToTable) {
        List<String> allSchemaList = new ArrayList<>();
        for (String schemaFullPathToTable : allSchemaFullPathToTable) {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SCHEMA_RESOURCE_PATH + File.separator + schemaFullPathToTable);
            try {
                allSchemaList.add(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return allSchemaList;
    }

    private List<String> getAllSchemaNamesFromTable() {
        return schemaRepository.getAllFullPathToTable();
    }

    public List<String> getNotCreatedSchemaNames(List<String> schemaNamesInTable) {
        List<String> servicesNamesFromResources = getAllServicesFromResources();
        Map<String, String> schemaTransferPathMap = getSchemaNamesSchemaPathFromResourcesMap(servicesNamesFromResources);
        List<String> schemasNamesFromResources = new ArrayList<>(schemaTransferPathMap.keySet());
        List<String> filteredSchemaNames = new ArrayList<>();
        for (String schemaName : schemasNamesFromResources) {
            if (!schemaNamesInTable.contains(schemaName)) {
                filteredSchemaNames.add(schemaTransferPathMap.get(schemaName));
            }
        }
        return filteredSchemaNames;
    }

    private ExportSchemaDto updateSchemaInTable(String schemaFullPathToTable, String schemaJson) {
        Schema schema = schemaRepository.findByFullPathToTable(schemaFullPathToTable);
        schema.setExportSchema(schemaJson);
        schemaRepository.save(schema);
        return mapper.domainToDto(schema);
    }
}
