package org.tedtalk.api.exceptions;

public class InvalidCsvDataException extends CsvImportException {
    public InvalidCsvDataException(String message, int rowNumber) {
        super("Invalid data at row " + rowNumber + ": " + message);
    }

    public InvalidCsvDataException(String message, int rowNumber, Throwable cause) {
        super("Invalid data at row " + rowNumber + ": " + message, cause);
    }
}
