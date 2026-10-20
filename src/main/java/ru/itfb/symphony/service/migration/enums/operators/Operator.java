package ru.itfb.symphony.service.migration.enums.operators;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Operator {
    ALL("all"),
    BETWEEN("between"),
    EXISTS("exists"),
    IN("in"),
    LIKE("ilike"),
    NOT("not"),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    EQUALITY("="),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<=");

    private final String operator;
}
