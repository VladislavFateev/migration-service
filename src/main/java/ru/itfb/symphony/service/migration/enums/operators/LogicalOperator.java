package ru.itfb.symphony.service.migration.enums.operators;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LogicalOperator {
    AND("and"),
    OR("or");

    private final String operator;
}
