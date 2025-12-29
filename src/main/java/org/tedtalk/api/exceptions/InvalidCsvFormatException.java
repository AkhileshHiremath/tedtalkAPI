package org.tedtalk.api.exceptions;

public class InvalidCsvFormatException extends CsvImportException {
    public InvalidCsvFormatException(String message) {
        super(message);
    }

    public InvalidCsvFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
