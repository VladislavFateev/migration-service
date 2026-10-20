package ru.itfb.symphony.service.migration.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itfb.symphony.common.model.dto.Result;
import ru.itfb.symphony.service.migration.enums.MigrationTaskType;
import ru.itfb.symphony.service.migration.filter.Filter;
import ru.itfb.symphony.service.migration.model.dto.MigrationTaskDto;
import ru.itfb.symphony.service.migration.schema.TransportSchema;
import ru.itfb.symphony.service.migration.service.ExportService;
import ru.itfb.symphony.service.migration.service.ImportService;
import ru.itfb.symphony.service.migration.service.MigrationTaskService;
import ru.itfb.symphony.service.migration.utills.FileEntityHolderService;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/public/task")

public class MigrationTaskController {

    private final MigrationTaskService migrationTaskService;
    private final ExportService exportService;
    private final ImportService importService;
    private final FileEntityHolderService fileEntityHolderService;

    @PostMapping("/export/{datasourceName}/{schemaName}")
    @Operation(summary = "Создание задачи для экспорта файла")
    public Result<MigrationTaskDto> createExportMigrationTask(
            @PathVariable String datasourceName,
            @PathVariable String schemaName,
            @RequestBody(required = false) Filter filters) {
        MigrationTaskDto migrationTaskDto = migrationTaskService.generateMigrationTask(MigrationTaskType.EXPORT, schemaName);
        exportService.createExportFile(schemaName, filters, migrationTaskDto, datasourceName);
        return Result.success(migrationTaskDto);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/{datasourceName}")
    @Operation(summary = "Создание задачи для импорта файла")
    public Result<MigrationTaskDto> createImportMigrationTask(
            @PathVariable String datasourceName,
            @RequestPart MultipartFile multipartFile,
            @RequestParam(required = false, defaultValue = "true") Boolean isCreatedData,
            @RequestParam(required = false, defaultValue = "false") Boolean isUpdateData,
            @RequestParam(required = false, defaultValue = "false") Boolean isCollisionCreateNewEntity
    ) throws IOException {
        File file = null;
        if (multipartFile.getOriginalFilename() != null) {
            file = new File(multipartFile.getOriginalFilename());
        }
        multipartFile.transferTo(file.toPath());
        List<File> directory = fileEntityHolderService.convertZipToFile(file);
        List<TransportSchema> schemaList = importService.getSchemaFromImportDirectory(directory);
        MigrationTaskDto migrationTaskDto = migrationTaskService.generateMigrationTask(MigrationTaskType.IMPORT, schemaList.getLast().getPriority().getLast());
        importService.importEntityHolder(directory, schemaList, isCreatedData, isUpdateData, isCollisionCreateNewEntity, migrationTaskDto, datasourceName);
        return Result.success(migrationTaskDto);
    }

}
