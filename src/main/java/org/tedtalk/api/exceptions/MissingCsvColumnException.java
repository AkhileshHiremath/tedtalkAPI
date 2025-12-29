package org.tedtalk.api.exceptions;

public class MissingCsvColumnException extends RuntimeException {
    public MissingCsvColumnException(String columnName) {
        super("Missing required CSV column: " + columnName);
    }
}
