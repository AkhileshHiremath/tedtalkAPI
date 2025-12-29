package org.tedtalk.api.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidCsvFormatExceptionTest {

    @Test
    void testInvalidCsvFormatExceptionWithMessage() {
        String message = "Invalid format";
        InvalidCsvFormatException exception = new InvalidCsvFormatException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testInvalidCsvFormatExceptionWithMessageAndCause() {
        // Given
        String message = "CSV parse error";
        Throwable cause = new RuntimeException("Underlying cause");

        // When
        InvalidCsvFormatException exception = new InvalidCsvFormatException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void shouldExtendCsvImportException() {
        // Given
        InvalidCsvFormatException exception = new InvalidCsvFormatException("test");

        // Then
        assertTrue(exception instanceof CsvImportException);
    }

    @Test
    void testInvalidCsvFormatExceptionWithNullMessage() {
        // Given & When
        InvalidCsvFormatException exception = new InvalidCsvFormatException(null);

        // Then
        assertNull(exception.getMessage());
    }

    @Test
    void testInvalidCsvFormatExceptionWithEmptyMessage() {
        // Given
        String message = "";

        // When
        InvalidCsvFormatException exception = new InvalidCsvFormatException(message);

        // Then
        assertEquals(message, exception.getMessage());
    }
}


