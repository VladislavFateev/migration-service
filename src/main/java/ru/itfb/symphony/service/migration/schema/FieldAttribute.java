package ru.itfb.symphony.service.migration.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.itfb.symphony.service.migration.enums.DataType;
import ru.itfb.symphony.service.migration.enums.FieldType;
import ru.itfb.symphony.service.migration.enums.LinkType;

@RequiredArgsConstructor
@Data
public class FieldAttribute {
    @JsonProperty("dataType")
    private DataType dataType;

    @JsonProperty("linkType")
    private LinkType linkType;

    @JsonProperty("link")
    private String link;

    @JsonProperty("columnLinkName")
    private String columnLinkName;

    @JsonProperty("entityLinkName")
    private String entityLinkName;

    @JsonProperty("fieldType")
    private FieldType fieldType;

    @JsonProperty("isPk")
    private Boolean isPk;

    @JsonProperty("isGenerateOnDbSide")
    private Boolean isGenerateOnDbSide;
}
