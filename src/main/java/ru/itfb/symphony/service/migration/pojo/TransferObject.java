package ru.itfb.symphony.service.migration.pojo;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Data
@Slf4j
@RequiredArgsConstructor
public class TransferObject {
    private String linkColumn;
    private Map<String, String> transferMap;


}
