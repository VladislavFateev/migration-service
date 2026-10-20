package ru.itfb.symphony.service.migration.pojo;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
@Slf4j
public class EntityHolder {
    private List<Map<String, EntityFieldHolder>> entity;
}
