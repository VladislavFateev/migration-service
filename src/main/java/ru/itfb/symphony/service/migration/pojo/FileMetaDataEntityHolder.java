package ru.itfb.symphony.service.migration.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.itfb.symphony.service.migration.schema.TransportSchema;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Data
@AllArgsConstructor
public class FileMetaDataEntityHolder {
    private String version;
    private LocalDateTime creationDateTime;
    private TransportSchema schema;
}
