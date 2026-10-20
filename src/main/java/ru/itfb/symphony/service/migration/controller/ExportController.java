package ru.itfb.symphony.service.migration.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itfb.symphony.service.migration.service.MigrationTaskService;
import ru.itfb.symphony.service.migration.utills.DeleteAfterReadFileSystemResource;

import java.io.File;

@RequestMapping("/public/export")
@RestController
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    @Value("${export.file.path:}")
    private String exportPathFile;

    private final MigrationTaskService migrationTask;

    @GetMapping("/migration-task/{guid}")
    @Operation(summary = "Получить экспорт файл по guid миграционной задаче")
    public ResponseEntity<DeleteAfterReadFileSystemResource> getExportFile(@PathVariable String guid) {
        if (migrationTask.isReadyTask(guid)) {
            HttpHeaders headers = new HttpHeaders();
            File file = new File(exportPathFile + File.separator + guid + ".zip");
            headers.setContentDisposition(
                ContentDisposition.inline().filename(file.getName()).build()
        );
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new DeleteAfterReadFileSystemResource(file));
        }
        log.warn("Файл ещё не создан");
        throw new ru.itfb.symphony.common.exception.BadRequestException("Файл ещё не создан");
    }
}
