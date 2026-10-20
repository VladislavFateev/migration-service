package ru.itfb.symphony.service.migration.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.itfb.symphony.service.migration.jdbc.JdbcSchemaGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/public/generate/schema")
public class GeneratorSchemaController {

    @Autowired
    private JdbcSchemaGenerator generator;

    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Генерация схем по ddl таблицы")
    @PostMapping(value = "/create/{datasourceName}/{schema}/{table}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileSystemResource> generateSchema(
            @PathVariable(name = "datasourceName") String datasourceName,
            @PathVariable(name = "schema") String schema,
            @PathVariable(name = "table") String table
    ) {
        String exportSchema;
        try {
            exportSchema = generator.generateSchemaJson(datasourceName, schema, table);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        try {
            File tempFile = File.createTempFile(table, ".json");

            try (FileWriter fileWriter = new FileWriter(tempFile)) {
                fileWriter.write(exportSchema);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDisposition(ContentDisposition.attachment().filename(tempFile.getName()).build());

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentLength(tempFile.length())
                    .body(new FileSystemResource(tempFile));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}