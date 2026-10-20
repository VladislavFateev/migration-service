package ru.itfb.symphony.service.migration.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itfb.symphony.service.migration.schema.TransportSchema;
import ru.itfb.symphony.service.migration.pojo.FileHolderEntity;

@Service
@Slf4j
@RequiredArgsConstructor
public class JsonConverter {

    private final ObjectMapper jacksonMapper;

    public TransportSchema toSchema(String jsonString) {
        try {
            return jacksonMapper.readValue(jsonString, new TypeReference<TransportSchema>() {
            });
        } catch (JsonProcessingException e) {
            log.error("ошибка при преобразовании в объект");
            throw new RuntimeException(e);
        }
    }

    public String toJson(FileHolderEntity file) {
        try {
            return jacksonMapper.writeValueAsString(file);
        } catch (JsonProcessingException e) {
            log.error("ошибка при преобразовании в JSON");
            throw new RuntimeException(e);
        }
    }

    public FileHolderEntity toFileHolderEntity(String jsonString) {
        try {
            return jacksonMapper.readValue(jsonString, new TypeReference<FileHolderEntity>() {
            });
        } catch (JsonProcessingException e) {
            log.error("ошибка при преобразовании в объект");
            throw new RuntimeException(e);
        }
    }
}
