package org.tedtalk.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvImportResultTest {

    @Test
    void success_WithValidData_ShouldCreateSuccessResult() {
        // Given
        long recordsImported = 100L;
        String message = "Import successful";

        // When
        CsvImportResult.Success result = new CsvImportResult.Success(recordsImported, message);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.recordsImported());
        assertEquals("Import successful", result.message());
    }

    @Test
    void success_WithZeroRecords_ShouldCreateSuccessResult() {
        // Given
        long recordsImported = 0L;
        String message = "No records to import";

        // When
        CsvImportResult.Success result = new CsvImportResult.Success(recordsImported, message);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.recordsImported());
        assertEquals("No records to import", result.message());
    }

    @Test
    void success_WithNegativeRecords_ShouldThrowException() {
        // Given
        long recordsImported = -1L;
        String message = "Invalid";

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.Success(recordsImported, message));
    }

    @Test
    void partialSuccess_WithValidData_ShouldCreatePartialSuccessResult() {
        // Given
        long recordsImported = 50L;
        long recordsFailed = 10L;
        String message = "Partial import";

        // When
        CsvImportResult.PartialSuccess result = new CsvImportResult.PartialSuccess(
            recordsImported, recordsFailed, message);

        // Then
        assertNotNull(result);
        assertEquals(50L, result.recordsImported());
        assertEquals(10L, result.recordsFailed());
        assertEquals("Partial import", result.message());
    }

    @Test
    void partialSuccess_WithZeroFailed_ShouldCreatePartialSuccessResult() {
        // Given
        long recordsImported = 100L;
        long recordsFailed = 0L;
        String message = "All succeeded";

        // When
        CsvImportResult.PartialSuccess result = new CsvImportResult.PartialSuccess(
            recordsImported, recordsFailed, message);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.recordsImported());
        assertEquals(0L, result.recordsFailed());
    }

    @Test
    void partialSuccess_WithNegativeImported_ShouldThrowException() {
        // Given
        long recordsImported = -1L;
        long recordsFailed = 5L;
        String message = "Invalid";

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.PartialSuccess(recordsImported, recordsFailed, message));
    }

    @Test
    void partialSuccess_WithNegativeFailed_ShouldThrowException() {
        // Given
        long recordsImported = 10L;
        long recordsFailed = -1L;
        String message = "Invalid";

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.PartialSuccess(recordsImported, recordsFailed, message));
    }

    @Test
    void partialSuccess_WithBothNegative_ShouldThrowException() {
        // Given
        long recordsImported = -5L;
        long recordsFailed = -10L;
        String message = "Invalid";

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.PartialSuccess(recordsImported, recordsFailed, message));
    }

    @Test
    void failure_WithValidData_ShouldCreateFailureResult() {
        // Given
        String errorMessage = "Import failed due to database error";
        Throwable cause = new RuntimeException("Database connection lost");

        // When
        CsvImportResult.Failure result = new CsvImportResult.Failure(errorMessage, cause);

        // Then
        assertNotNull(result);
        assertEquals("Import failed due to database error", result.errorMessage());
        assertEquals(cause, result.cause());
    }

    @Test
    void failure_WithNullCause_ShouldCreateFailureResult() {
        // Given
        String errorMessage = "Import failed";
        Throwable cause = null;

        // When
        CsvImportResult.Failure result = new CsvImportResult.Failure(errorMessage, cause);

        // Then
        assertNotNull(result);
        assertEquals("Import failed", result.errorMessage());
        assertNull(result.cause());
    }

    @Test
    void failure_WithNullMessage_ShouldThrowException() {
        // Given
        String errorMessage = null;
        Throwable cause = new RuntimeException("Error");

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.Failure(errorMessage, cause));
    }

    @Test
    void failure_WithBlankMessage_ShouldThrowException() {
        // Given
        String errorMessage = "   ";
        Throwable cause = new RuntimeException("Error");

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.Failure(errorMessage, cause));
    }

    @Test
    void failure_WithEmptyMessage_ShouldThrowException() {
        // Given
        String errorMessage = "";
        Throwable cause = new RuntimeException("Error");

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.Failure(errorMessage, cause));
    }

    @Test
    void allResults_ShouldImplementCsvImportResult() {
        // Given & When
        CsvImportResult success = new CsvImportResult.Success(10L, "Success");
        CsvImportResult partial = new CsvImportResult.PartialSuccess(5L, 2L, "Partial");
        CsvImportResult failure = new CsvImportResult.Failure("Failed", null);

        // Then
        assertTrue(success instanceof CsvImportResult);
        assertTrue(partial instanceof CsvImportResult);
        assertTrue(failure instanceof CsvImportResult);
    }

    @Test
    void success_WithLargeNumbers_ShouldHandle() {
        // Given
        long recordsImported = Long.MAX_VALUE;
        String message = "Large import";

        // When
        CsvImportResult.Success result = new CsvImportResult.Success(recordsImported, message);

        // Then
        assertEquals(Long.MAX_VALUE, result.recordsImported());
    }

    @Test
    void partialSuccess_WithLargeNumbers_ShouldHandle() {
        // Given
        long recordsImported = Long.MAX_VALUE - 1;
        long recordsFailed = Long.MAX_VALUE - 2;
        String message = "Large partial import";

        // When
        CsvImportResult.PartialSuccess result = new CsvImportResult.PartialSuccess(
            recordsImported, recordsFailed, message);

        // Then
        assertEquals(Long.MAX_VALUE - 1, result.recordsImported());
        assertEquals(Long.MAX_VALUE - 2, result.recordsFailed());
    }

    @Test
    void failure_WithDetailedMessage_ShouldPreserveMessage() {
        // Given
        String errorMessage = "Import failed at row 42: invalid date format 'abc' expected 'MMMM yyyy'";
        Throwable cause = new IllegalArgumentException("Invalid format");

        // When
        CsvImportResult.Failure result = new CsvImportResult.Failure(errorMessage, cause);

        // Then
        assertEquals(errorMessage, result.errorMessage());
        assertTrue(result.cause() instanceof IllegalArgumentException);
    }

    @Test
    void records_ShouldBeImmutable() {
        // Given
        CsvImportResult.Success success = new CsvImportResult.Success(10L, "Success");

        // Then - Records are implicitly final, testing that values don't change
        assertEquals(10L, success.recordsImported());
        assertEquals("Success", success.message());

        // Creating a new instance doesn't affect the old one
        CsvImportResult.Success success2 = new CsvImportResult.Success(20L, "Success2");
        assertEquals(10L, success.recordsImported());
        assertEquals(20L, success2.recordsImported());
    }

    @Test
    void success_IsSuccessful_ShouldReturnTrue() {
        // Given
        CsvImportResult.Success result = new CsvImportResult.Success(100L, "All imported");

        // When & Then
        assertTrue(result.isSuccessful());
    }

    @Test
    void success_GetStatusMessage_ShouldReturnFormattedMessage() {
        // Given
        CsvImportResult.Success result = new CsvImportResult.Success(100L, "All imported");

        // When
        String statusMessage = result.getStatusMessage();

        // Then
        assertTrue(statusMessage.contains("Success"));
        assertTrue(statusMessage.contains("100"));
        assertTrue(statusMessage.contains("All imported"));
    }

    @Test
    void partialSuccess_WithRecordsImported_IsSuccessful_ShouldReturnTrue() {
        // Given
        CsvImportResult.PartialSuccess result = new CsvImportResult.PartialSuccess(
            50L, 10L, "Partial import");

        // When & Then
        assertTrue(result.isSuccessful());
    }

    @Test
    void partialSuccess_WithZeroImported_IsSuccessful_ShouldReturnFalse() {
        // Given
        CsvImportResult.PartialSuccess result = new CsvImportResult.PartialSuccess(
            0L, 10L, "All failed");

        // When & Then
        assertFalse(result.isSuccessful());
    }

    @Test
    void partialSuccess_GetStatusMessage_ShouldReturnFormattedMessage() {
        // Given
        CsvImportResult.PartialSuccess result = new CsvImportResult.PartialSuccess(
            75L, 25L, "Some errors");

        // When
        String statusMessage = result.getStatusMessage();

        // Then
        assertTrue(statusMessage.contains("Partial"));
        assertTrue(statusMessage.contains("75"));
        assertTrue(statusMessage.contains("25"));
        assertTrue(statusMessage.contains("Some errors"));
    }

    @Test
    void failure_IsSuccessful_ShouldReturnFalse() {
        // Given
        CsvImportResult.Failure result = new CsvImportResult.Failure(
            "Database error", new RuntimeException("Connection lost"));

        // When & Then
        assertFalse(result.isSuccessful());
    }

    @Test
    void failure_GetStatusMessage_ShouldReturnFormattedMessage() {
        // Given
        CsvImportResult.Failure result = new CsvImportResult.Failure(
            "Database error", new RuntimeException("Connection lost"));

        // When
        String statusMessage = result.getStatusMessage();

        // Then
        assertTrue(statusMessage.contains("Failed"));
        assertTrue(statusMessage.contains("Database error"));
    }

    @Test
    void failure_WithNullErrorMessage_ShouldThrowException() {
        // Testing the null check branch in Failure
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.Failure(null, new RuntimeException()));
    }

    @Test
    void failure_WithBlankErrorMessage_ShouldThrowException() {
        // Testing the isBlank() check branch in Failure
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.Failure("   ", new RuntimeException()));
        assertThrows(IllegalArgumentException.class,
            () -> new CsvImportResult.Failure("", new RuntimeException()));
    }

    @Test
    void partialSuccess_BothRecordsZero_IsSuccessful_ShouldReturnFalse() {
        // Edge case: both imported and failed are zero
        CsvImportResult.PartialSuccess result = new CsvImportResult.PartialSuccess(
            0L, 0L, "No records processed");

        // When & Then
        assertFalse(result.isSuccessful());
    }
}

