package org.tedtalk.api.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidCsvDataExceptionTest {

    @Test
    void constructor_WithMessageAndRowNumber_ShouldCreateException() {
        // Given
        String message = "views must be a valid number";
        int rowNumber = 42;

        // When
        InvalidCsvDataException exception = new InvalidCsvDataException(message, rowNumber);

        // Then
        assertNotNull(exception);
        assertEquals("Invalid data at row 42: views must be a valid number", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_WithMessageRowNumberAndCause_ShouldCreateException() {
        // Given
        String message = "views must be a valid number, got: abc";
        int rowNumber = 100;
        NumberFormatException cause = new NumberFormatException("For input string: \"abc\"");

        // When
        InvalidCsvDataException exception = new InvalidCsvDataException(message, rowNumber, cause);

        // Then
        assertNotNull(exception);
        assertEquals("Invalid data at row 100: views must be a valid number, got: abc", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertTrue(exception.getCause() instanceof NumberFormatException);
    }

    @Test
    void shouldExtendCsvImportException() {
        // Given
        InvalidCsvDataException exception = new InvalidCsvDataException("test", 1);

        // Then
        assertTrue(exception instanceof CsvImportException);
    }
}

