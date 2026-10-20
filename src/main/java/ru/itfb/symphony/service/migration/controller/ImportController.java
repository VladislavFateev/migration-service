package ru.itfb.symphony.service.migration.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itfb.symphony.common.model.dto.Result;
import ru.itfb.symphony.service.migration.model.dto.MigrationTaskDto;
import ru.itfb.symphony.service.migration.service.MigrationTaskService;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/public/import")

public class ImportController {

    private final MigrationTaskService migrationTaskService;

    @GetMapping("/{guid}")
    @Operation(summary = "Получить отчет о создание сущностей по guid миграционной задаче")
    public Result<MigrationTaskDto> getReportImportMigrationTaskByGuid(@PathVariable String guid) {
        if (migrationTaskService.isReadyTask(guid)) {
            return Result.success(migrationTaskService.findMigrationTask(guid));
        }
        log.warn("Файл ещё не создан");
        throw new ru.itfb.symphony.common.exception.BadRequestException("Файл ещё не создан");
    }
}
