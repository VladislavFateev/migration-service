package ru.itfb.symphony.service.migration.cte;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class HierarchyHolderEntity {
    private String id;
    private Integer lvl;
}
