package org.tedtalk.api.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvImportExceptionTest {

    @Test
    void testCsvImportExceptionWithMessage() {
        String message = "Test error";
        CsvImportException exception = new CsvImportException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testCsvImportExceptionWithMessageAndCause() {
        String message = "Test error";
        Throwable cause = new RuntimeException("Cause");
        CsvImportException exception = new CsvImportException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
