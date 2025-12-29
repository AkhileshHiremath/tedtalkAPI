package org.tedtalk.api.utils;

import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.tedtalk.api.exceptions.InvalidCsvDataException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FileParsingUtilTest {

    @Test
    void appendDataBuffer_ShouldAppendContent() {
        // Given
        StringBuilder sb = new StringBuilder("Hello ");
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = factory.wrap("World".getBytes(StandardCharsets.UTF_8));

        // When
        StringBuilder result = FileParsingUtil.appendDataBuffer(sb, dataBuffer);

        // Then
        assertEquals("Hello World", result.toString());
    }

    @Test
    void appendDataBuffer_WithEmptyBuffer_ShouldReturnSameBuilder() {
        // Given
        StringBuilder sb = new StringBuilder("Test");
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = factory.wrap("".getBytes(StandardCharsets.UTF_8));

        // When
        StringBuilder result = FileParsingUtil.appendDataBuffer(sb, dataBuffer);

        // Then
        assertEquals("Test", result.toString());
    }

    @Test
    void createCsvParser_WithValidCsv_ShouldCreateParser() throws IOException {
        // Given
        String csvContent = "title,author,date\nTalk 1,Speaker 1,January 2020\n";
        BufferedReader reader = new BufferedReader(new StringReader(csvContent));

        // When
        CSVParser parser = FileParsingUtil.createCsvParser(reader);

        // Then
        assertNotNull(parser);
        assertEquals(3, parser.getHeaderMap().size());
        assertTrue(parser.getHeaderMap().containsKey("title"));
        assertTrue(parser.getHeaderMap().containsKey("author"));
        assertTrue(parser.getHeaderMap().containsKey("date"));
        parser.close();
    }

    @Test
    void createCsvParser_WithEmptyCsv_ShouldCreateParser() throws IOException {
        // Given
        String csvContent = "";
        BufferedReader reader = new BufferedReader(new StringReader(csvContent));

        // When
        CSVParser parser = FileParsingUtil.createCsvParser(reader);

        // Then
        assertNotNull(parser);
        parser.close();
    }

    @Test
    void closeQuietly_WithValidCloseable_ShouldCloseWithoutException() {
        // Given
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);

        // When & Then
        assertDoesNotThrow(() -> FileParsingUtil.closeQuietly(stream));
    }

    @Test
    void closeQuietly_WithNullCloseable_ShouldNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> FileParsingUtil.closeQuietly((Closeable) null));
    }

    @Test
    void closeQuietly_WithMultipleCloseables_ShouldCloseAll() {
        // Given
        ByteArrayInputStream stream1 = new ByteArrayInputStream(new byte[0]);
        ByteArrayInputStream stream2 = new ByteArrayInputStream(new byte[0]);

        // When & Then
        assertDoesNotThrow(() -> FileParsingUtil.closeQuietly(stream1, stream2));
    }

    @Test
    void closeQuietly_WithThrowingCloseable_ShouldSuppressException() {
        // Given
        Closeable throwingCloseable = () -> {
            throw new IOException("Test exception");
        };

        // When & Then
        assertDoesNotThrow(() -> FileParsingUtil.closeQuietly(throwingCloseable));
    }

    @Test
    void parseNumericValue_WithValidNumber_ShouldReturnLong() {
        // Given
        String value = "12345";
        long rowNumber = 1;
        String fieldName = "views";

        // When
        long result = FileParsingUtil.parseNumericValue(value, rowNumber, fieldName);

        // Then
        assertEquals(12345L, result);
    }

    @Test
    void parseNumericValue_WithValidNumberAndWhitespace_ShouldReturnLong() {
        // Given
        String value = "  9999  ";
        long rowNumber = 2;
        String fieldName = "likes";

        // When
        long result = FileParsingUtil.parseNumericValue(value, rowNumber, fieldName);

        // Then
        assertEquals(9999L, result);
    }

    @Test
    void parseNumericValue_WithNullValue_ShouldThrowException() {
        // Given
        String value = null;
        long rowNumber = 5;
        String fieldName = "views";

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.parseNumericValue(value, rowNumber, fieldName)
        );
        assertTrue(exception.getMessage().contains("views cannot be null"));
        assertTrue(exception.getMessage().contains("row 5"));
    }

    @Test
    void parseNumericValue_WithEmptyString_ShouldThrowException() {
        // Given
        String value = "   ";
        long rowNumber = 10;
        String fieldName = "likes";

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.parseNumericValue(value, rowNumber, fieldName)
        );
        assertTrue(exception.getMessage().contains("likes cannot be empty"));
        assertTrue(exception.getMessage().contains("row 10"));
    }

    @Test
    void parseNumericValue_WithInvalidNumber_ShouldThrowException() {
        // Given
        String value = "abc123";
        long rowNumber = 15;
        String fieldName = "views";

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.parseNumericValue(value, rowNumber, fieldName)
        );
        assertTrue(exception.getMessage().contains("views must be a valid number"));
        assertTrue(exception.getMessage().contains("abc123"));
        assertTrue(exception.getMessage().contains("row 15"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof NumberFormatException);
    }

    @Test
    void parseNumericValue_WithNegativeNumber_ShouldReturnNegativeLong() {
        // Given
        String value = "-500";
        long rowNumber = 20;
        String fieldName = "score";

        // When
        long result = FileParsingUtil.parseNumericValue(value, rowNumber, fieldName);

        // Then
        assertEquals(-500L, result);
    }

    @Test
    void parseNumericValue_WithZero_ShouldReturnZero() {
        // Given
        String value = "0";
        long rowNumber = 25;
        String fieldName = "count";

        // When
        long result = FileParsingUtil.parseNumericValue(value, rowNumber, fieldName);

        // Then
        assertEquals(0L, result);
    }

    @Test
    void parseDateSafely_WithValidDate_ShouldReturnLocalDateTime() {
        // Given
        String dateString = "January 2020";
        long rowNumber = 1;

        // When
        LocalDateTime result = FileParsingUtil.parseDateSafely(dateString, rowNumber);

        // Then
        assertNotNull(result);
        assertEquals(2020, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
    }

    @Test
    void parseDateSafely_WithValidDateAndWhitespace_ShouldReturnLocalDateTime() {
        // Given
        String dateString = "  March 2021  ";
        long rowNumber = 2;

        // When
        LocalDateTime result = FileParsingUtil.parseDateSafely(dateString, rowNumber);

        // Then
        assertNotNull(result);
        assertEquals(2021, result.getYear());
        assertEquals(3, result.getMonthValue());
    }

    @Test
    void parseDateSafely_WithEmptyString_ShouldThrowException() {
        // Given
        String dateString = "   ";
        long rowNumber = 5;

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.parseDateSafely(dateString, rowNumber)
        );
        assertTrue(exception.getMessage().contains("Date cannot be empty"));
        assertTrue(exception.getMessage().contains("row 5"));
    }

    @Test
    void parseDateSafely_WithInvalidFormat_ShouldThrowException() {
        // Given
        String dateString = "2020-01-01";
        long rowNumber = 10;

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.parseDateSafely(dateString, rowNumber)
        );
        assertTrue(exception.getMessage().contains("Invalid date format"));
        assertTrue(exception.getMessage().contains("2020-01-01"));
        assertTrue(exception.getMessage().contains("MMMM yyyy"));
        assertTrue(exception.getMessage().contains("row 10"));
        assertNotNull(exception.getCause());
    }

    @Test
    void parseDateSafely_WithInvalidMonth_ShouldThrowException() {
        // Given
        String dateString = "InvalidMonth 2020";
        long rowNumber = 15;

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.parseDateSafely(dateString, rowNumber)
        );
        assertTrue(exception.getMessage().contains("Invalid date format"));
        assertNotNull(exception.getCause());
    }

    @Test
    void getSafeStringValue_WithValidString_ShouldReturnTrimmedValue() {
        // Given
        String value = "  Test Value  ";
        long rowNumber = 1;
        String fieldName = "title";

        // When
        String result = FileParsingUtil.getSafeStringValue(value, rowNumber, fieldName);

        // Then
        assertEquals("Test Value", result);
    }

    @Test
    void getSafeStringValue_WithNullValue_ShouldThrowException() {
        // Given
        String value = null;
        long rowNumber = 5;
        String fieldName = "author";

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.getSafeStringValue(value, rowNumber, fieldName)
        );
        assertTrue(exception.getMessage().contains("author cannot be empty"));
        assertTrue(exception.getMessage().contains("row 5"));
    }

    @Test
    void getSafeStringValue_WithEmptyString_ShouldThrowException() {
        // Given
        String value = "   ";
        long rowNumber = 10;
        String fieldName = "description";

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.getSafeStringValue(value, rowNumber, fieldName)
        );
        assertTrue(exception.getMessage().contains("description cannot be empty"));
        assertTrue(exception.getMessage().contains("row 10"));
    }

    @Test
    void getSafeStringValue_WithEmptyStringNoSpaces_ShouldThrowException() {
        // Given
        String value = "";
        long rowNumber = 15;
        String fieldName = "title";

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.getSafeStringValue(value, rowNumber, fieldName)
        );
        assertTrue(exception.getMessage().contains("title cannot be empty"));
    }

    @Test
    void parseDateSafely_WithFullYearRange_ShouldParseCorrectly() {
        // Given & When & Then - Testing various years
        LocalDateTime result1 = FileParsingUtil.parseDateSafely("January 2020", 1);
        assertEquals(2020, result1.getYear());
        assertEquals(1, result1.getMonthValue());

        LocalDateTime result2 = FileParsingUtil.parseDateSafely("December 2025", 2);
        assertEquals(2025, result2.getYear());
        assertEquals(12, result2.getMonthValue());
    }

    @Test
    void parseNumericValue_WithMaxLongValue_ShouldParse() {
        // Given
        String value = String.valueOf(Long.MAX_VALUE);
        long rowNumber = 1;
        String fieldName = "views";

        // When
        long result = FileParsingUtil.parseNumericValue(value, rowNumber, fieldName);

        // Then
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    void parseNumericValue_WithMinLongValue_ShouldParse() {
        // Given
        String value = String.valueOf(Long.MIN_VALUE);
        long rowNumber = 1;
        String fieldName = "score";

        // When
        long result = FileParsingUtil.parseNumericValue(value, rowNumber, fieldName);

        // Then
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    void createCsvParser_WithSpecialCharacters_ShouldHandle() throws IOException {
        // Given
        String csvContent = "title,author,date\n\"Talk, with comma\",\"Author's name\",January 2020\n";
        BufferedReader reader = new BufferedReader(new StringReader(csvContent));

        // When
        CSVParser parser = FileParsingUtil.createCsvParser(reader);

        // Then
        assertNotNull(parser);
        assertEquals(3, parser.getHeaderMap().size());
        parser.close();
    }

    @Test
    void closeQuietly_WithMultipleNullCloseables_ShouldNotThrow() {
        // When & Then
        assertDoesNotThrow(() -> FileParsingUtil.closeQuietly(null, null, null));
    }

    @Test
    void closeQuietly_WithMixedNullAndValid_ShouldCloseValid() {
        // Given
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);

        // When & Then
        assertDoesNotThrow(() -> FileParsingUtil.closeQuietly(null, stream, null));
    }

    @Test
    void appendDataBuffer_WithLargeBuffer_ShouldAppendAll() {
        // Given
        StringBuilder sb = new StringBuilder();
        String largeContent = "A".repeat(10000);
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = factory.wrap(largeContent.getBytes(StandardCharsets.UTF_8));

        // When
        StringBuilder result = FileParsingUtil.appendDataBuffer(sb, dataBuffer);

        // Then
        assertEquals(largeContent, result.toString());
        assertEquals(10000, result.length());
    }

    @Test
    void parseNumericValue_WithLeadingZeros_ShouldParse() {
        // Given
        String value = "00001234";
        long rowNumber = 1;
        String fieldName = "views";

        // When
        long result = FileParsingUtil.parseNumericValue(value, rowNumber, fieldName);

        // Then
        assertEquals(1234L, result);
    }

    @Test
    void parseDateSafely_WithAllMonths_ShouldParseCorrectly() {
        // Given & When & Then
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};

        for (int i = 0; i < months.length; i++) {
            LocalDateTime result = FileParsingUtil.parseDateSafely(months[i] + " 2023", i + 1);
            assertEquals(2023, result.getYear());
            assertEquals(i + 1, result.getMonthValue());
        }
    }

    @Test
    void getSafeStringValue_WithOnlyWhitespace_ShouldThrowException() {
        // Given
        String value = "\t\n\r ";
        long rowNumber = 1;
        String fieldName = "title";

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.getSafeStringValue(value, rowNumber, fieldName)
        );
        assertTrue(exception.getMessage().contains("title cannot be empty"));
    }

    @Test
    void parseNumericValue_WithScientificNotation_ShouldThrowException() {
        // Given
        String value = "1e10";
        long rowNumber = 1;
        String fieldName = "views";

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.parseNumericValue(value, rowNumber, fieldName)
        );
        assertTrue(exception.getMessage().contains("must be a valid number"));
    }

    @Test
    void parseDateSafely_WithLowercaseMonth_ShouldThrowException() {
        // Given
        String value = "january 2020";
        long rowNumber = 1;

        // When & Then
        InvalidCsvDataException exception = assertThrows(
            InvalidCsvDataException.class,
            () -> FileParsingUtil.parseDateSafely(value, rowNumber)
        );
        assertTrue(exception.getMessage().contains("Invalid date format"));
    }

    @Test
    void createCsvParser_WithMultilineContent_ShouldParseAll() throws IOException {
        // Given
        String csvContent = "col1,col2,col3\n" +
                           "val1,val2,val3\n" +
                           "val4,val5,val6\n" +
                           "val7,val8,val9\n";
        BufferedReader reader = new BufferedReader(new StringReader(csvContent));

        // When
        CSVParser parser = FileParsingUtil.createCsvParser(reader);

        // Then
        assertNotNull(parser);
        assertEquals(3, parser.getHeaderMap().size());
        parser.close();
    }
}

