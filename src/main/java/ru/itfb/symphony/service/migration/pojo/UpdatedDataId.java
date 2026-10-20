package ru.itfb.symphony.service.migration.pojo;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.itfb.symphony.service.migration.enums.FieldType;

import java.util.Map;

@Data
@RequiredArgsConstructor

public class UpdatedDataId {
    private FieldType fieldType;
    private Map<String, String> transferMap;
}
