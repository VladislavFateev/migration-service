package ru.itfb.symphony.service.migration.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.itfb.symphony.service.migration.service.SchemaService;
import ru.itfb.symphony.service.migration.model.dto.ExportSchemaDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("public/schema")
public class SchemaController {

    private final SchemaService schemaService;

    @PostMapping("/create")
    @Operation(summary = "Создание схемы")
    public ExportSchemaDto createSchema(ExportSchemaDto exportSchemaDto) {
        return schemaService.createSchema(exportSchemaDto);
    }

    @PostMapping("/create/schemas")
    @Operation(summary = "Создание схем")
    public List<ExportSchemaDto> createSchemas(List<ExportSchemaDto> exportSchemaDtos) {
        return schemaService.createSchemas(exportSchemaDtos);
    }

    @PutMapping
    @Operation(summary = "Обновление схем")
    public List<ExportSchemaDto> updateSchemasByResources() {
        return schemaService.updateSchemasInTableByResources();
    }

    @DeleteMapping
    @Operation(summary = "Удаление схем")
    public ExportSchemaDto deleteSchemas(@RequestParam String schemaFullPathToTable) {
        return schemaService.deleteSchemaInTable(schemaFullPathToTable);
    }
}
