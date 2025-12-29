package org.tedtalk.api.exceptions;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void constructor_ShouldCreateErrorResponseWithAllFields() {
        // Given
        int status = 400;
        String error = "Bad Request";
        String message = "Invalid input";
        String path = "/api/v1/talks";

        // When
        ErrorResponse response = new ErrorResponse(status, error, message, path);

        // Then
        assertNotNull(response);
        assertEquals(status, response.getStatus());
        assertEquals(error, response.getError());
        assertEquals(message, response.getMessage());
        assertEquals(path, response.getPath());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void getTimestamp_ShouldReturnCurrentTime() {
        // Given
        Instant before = Instant.now();
        ErrorResponse response = new ErrorResponse(404, "Not Found", "Resource not found", "/api/v1/talks/999");
        Instant after = Instant.now();

        // When
        Instant timestamp = response.getTimestamp();

        // Then
        assertNotNull(timestamp);
        assertTrue(timestamp.equals(before) || timestamp.isAfter(before));
        assertTrue(timestamp.equals(after) || timestamp.isBefore(after));
    }

    @Test
    void getStatus_ShouldReturnCorrectStatusCode() {
        // Given
        ErrorResponse response = new ErrorResponse(500, "Internal Server Error", "Unexpected error", "/api/v1/talks");

        // When
        int status = response.getStatus();

        // Then
        assertEquals(500, status);
    }

    @Test
    void getError_ShouldReturnCorrectErrorType() {
        // Given
        ErrorResponse response = new ErrorResponse(422, "Unprocessable Entity", "CSV import failed", "/api/v1/talks/import");

        // When
        String error = response.getError();

        // Then
        assertEquals("Unprocessable Entity", error);
    }

    @Test
    void getMessage_ShouldReturnCorrectMessage() {
        // Given
        String expectedMessage = "Missing required CSV column: title";
        ErrorResponse response = new ErrorResponse(400, "Bad Request", expectedMessage, "/api/v1/talks/import");

        // When
        String message = response.getMessage();

        // Then
        assertEquals(expectedMessage, message);
    }

    @Test
    void getPath_ShouldReturnCorrectPath() {
        // Given
        String expectedPath = "/api/v1/talks/year/2023";
        ErrorResponse response = new ErrorResponse(404, "Not Found", "No talks found", expectedPath);

        // When
        String path = response.getPath();

        // Then
        assertEquals(expectedPath, path);
    }

    @Test
    void multipleInstances_ShouldHaveDifferentTimestamps() throws InterruptedException {
        // Given
        ErrorResponse response1 = new ErrorResponse(400, "Error", "Message 1", "/path1");
        Thread.sleep(10); // Small delay to ensure different timestamps
        ErrorResponse response2 = new ErrorResponse(400, "Error", "Message 2", "/path2");

        // When & Then
        assertNotEquals(response1.getTimestamp(), response2.getTimestamp());
        assertTrue(response2.getTimestamp().isAfter(response1.getTimestamp()));
    }

    @Test
    void constructor_WithNullValues_ShouldCreateResponse() {
        // Given & When
        ErrorResponse response = new ErrorResponse(500, null, null, null);

        // Then
        assertNotNull(response);
        assertEquals(500, response.getStatus());
        assertNull(response.getError());
        assertNull(response.getMessage());
        assertNull(response.getPath());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void constructor_WithEmptyStrings_ShouldCreateResponse() {
        // Given & When
        ErrorResponse response = new ErrorResponse(400, "", "", "");

        // Then
        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertEquals("", response.getError());
        assertEquals("", response.getMessage());
        assertEquals("", response.getPath());
    }
}

