package org.tedtalk.api.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private ServerHttpRequest mockRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        mockRequest = mock(ServerHttpRequest.class);
        when(mockRequest.getURI()).thenReturn(URI.create("/api/v1/talks"));
    }

    @Test
    void handleMissingCsvColumn_ShouldReturnBadRequest() {
        
        MissingCsvColumnException exception = new MissingCsvColumnException("title");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMissingCsvColumn(exception, mockRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Missing required CSV column: title", response.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleCsvImport_ShouldReturnUnprocessableEntity() {
        
        CsvImportException exception = new CsvImportException("CSV import failed");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCsvImport(exception, mockRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CSV import failed", response.getBody().getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getBody().getStatus());
    }

    @Test
    void handleValidation_ShouldReturnBadRequest() {
        
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.Collections.emptyList());

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(exception, mockRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
    }

    @Test
    void handleValidation_WithFieldErrors_ShouldReturnMessageWithFieldName() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        org.springframework.validation.FieldError fieldError = mock(org.springframework.validation.FieldError.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));
        when(fieldError.getField()).thenReturn("email");
        when(fieldError.getDefaultMessage()).thenReturn("must be valid");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(exception, mockRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("email: must be valid", response.getBody().getMessage());
    }

    @Test
    void handleValidation_WithEmptyFieldName_ShouldReturnDefaultMessage() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        org.springframework.validation.FieldError fieldError = mock(org.springframework.validation.FieldError.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));
        when(fieldError.getField()).thenReturn("");
        when(fieldError.getDefaultMessage()).thenReturn("must be valid");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(exception, mockRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().getMessage());
    }

    @Test
    void handleGeneric_ShouldReturnInternalServerError() {
        
        Exception exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGeneric(exception, mockRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unexpected error", response.getBody().getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
    }

    @Test
    void handleInvalidCsvDataException_ShouldReturnUnprocessableEntity() {
        // Given
        InvalidCsvDataException exception = new InvalidCsvDataException("views must be valid", 42);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCsvImport(exception, mockRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid data at row 42"));
    }

    @Test
    void errorResponse_ShouldContainAllFields() {
        // Given
        CsvImportException exception = new CsvImportException("Test error");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCsvImport(exception, mockRequest);

        // Then
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getTimestamp());
        assertEquals(422, body.getStatus());
        assertEquals("CSV import error", body.getError());
        assertEquals("Test error", body.getMessage());
        assertEquals("/api/v1/talks", body.getPath());
    }

    @Test
    void handleValidation_WithMultipleFieldErrors_ShouldReturnFirstError() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        org.springframework.validation.FieldError fieldError1 = mock(org.springframework.validation.FieldError.class);
        org.springframework.validation.FieldError fieldError2 = mock(org.springframework.validation.FieldError.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError1, fieldError2));
        when(fieldError1.getField()).thenReturn("name");
        when(fieldError1.getDefaultMessage()).thenReturn("must not be null");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(exception, mockRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("name: must not be null", response.getBody().getMessage());
    }
}

