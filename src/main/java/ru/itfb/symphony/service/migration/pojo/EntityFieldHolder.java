package ru.itfb.symphony.service.migration.pojo;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@RequiredArgsConstructor
@Slf4j
public class EntityFieldHolder {
    private String value;
}
