package com.example.store.enums;

import java.util.Arrays;
import java.util.Optional;

public enum OrderStatusName {
    NOT_PROCESSED("NOT_PROCESSED"),
    CONFIRMED("CONFIRMED"),
    SHIPPING("SHIPPING"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED"),
    RETURNED("RETURNED");

    private final String dbName;

    OrderStatusName(String dbName) { this.dbName = dbName; }

    public String getDbName() { return dbName; }

    public static Optional<OrderStatusName> fromDbName(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.replaceAll("[^A-Za-z]", "").toUpperCase(java.util.Locale.ROOT);
        return Arrays.stream(values())
                .filter(s -> s.name().replace("_", "").equals(normalized))
                .findFirst();
    }
}
