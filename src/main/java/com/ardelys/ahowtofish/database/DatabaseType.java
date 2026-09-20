package com.ardelys.ahowtofish.database;

public enum DatabaseType {
    SQLITE,
    MYSQL,
    MARIADB;

    public static DatabaseType fromString(String type) {
        if (type == null) return SQLITE;
        return switch (type.toUpperCase().trim()) {
            case "MYSQL" -> MYSQL;
            case "MARIADB" -> MARIADB;
            default -> SQLITE;
        };
    }
}
