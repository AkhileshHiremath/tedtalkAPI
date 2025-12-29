package org.tedtalk.api.utils;

import jakarta.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.tedtalk.api.exceptions.InvalidCsvDataException;
import org.springframework.core.io.buffer.DataBuffer;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for CSV file parsing and data validation operations.
 * Provides reusable methods for numeric parsing, date parsing, and resource management.
 */
@Slf4j
@UtilityClass
public class FileParsingUtil {

    private static final String DATE_PATTERN = "MMMM yyyy";

    /**
     * Appends DataBuffer content to StringBuilder using UTF-8 charset.
     * @param sb the StringBuilder to append to
     * @param dataBuffer the DataBuffer to read from
     * @return the StringBuilder with appended content
     */
    @Nonnull
    public static StringBuilder appendDataBuffer(StringBuilder sb, DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        return sb.append(new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Creates a CSV parser with standard format and header row configuration.
     * Treats the first record as header names automatically.
     * Note: withFirstRecordAsHeader() is deprecated in Apache Commons CSV but remains the standard approach
     * for treating the first record as column headers.
     * @param reader the BufferedReader containing CSV content
     * @return CSVParser configured for parsing with headers
     * @throws IOException if parser creation fails
     */
    @Nonnull
    public static CSVParser createCsvParser(BufferedReader reader) throws IOException {
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);
    }

    /**
     * Closes resources quietly, suppressing any exceptions.
     * @param closeables array of Closeable resources to close
     */
    public static void closeQuietly(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) c.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Parses a numeric value with proper error handling.
     *
     * Java 21 Enhancement: Uses pattern matching for switch expressions.
     *
     * @param value the string value to parse
     * @param rowNumber the row number for error reporting
     * @param fieldName the field name for error messages
     * @return parsed long value
     * @throws InvalidCsvDataException if value is invalid or empty
     */
    public static long parseNumericValue(String value, long rowNumber, String fieldName) {
        try {
            // Java 21: Pattern matching with switch - cleaner than if-else chains
            return switch (value) {
                case null -> {
                    log.warn("Field '{}' is null at row {}", fieldName, rowNumber);
                    throw new InvalidCsvDataException(fieldName + " cannot be null", (int) rowNumber);
                }
                case String s when s.isBlank() -> {
                    log.warn("Field '{}' is empty at row {}", fieldName, rowNumber);
                    throw new InvalidCsvDataException(fieldName + " cannot be empty", (int) rowNumber);
                }
                case String s -> {
                    long result = Long.parseLong(s.trim());
                    log.debug("Field '{}' at row {} parsed to: {}", fieldName, rowNumber, result);
                    yield result;
                }
            };
        } catch (NumberFormatException e) {
            log.error("Invalid numeric value '{}' for field '{}' at row {}", value, fieldName, rowNumber);
            throw new InvalidCsvDataException(fieldName + " must be a valid number, got: " + value, (int) rowNumber, e);
        }
    }

    /**
     * Parses a date string safely, returning a LocalDateTime.
     *
     * @param dateString the date string to parse
     * @param rowNumber  the row number for error reporting
     * @return the parsed LocalDateTime
     * @throws InvalidCsvDataException if the date string is invalid
     */
    @Nonnull
    public static LocalDateTime parseDateSafely(@Nonnull String dateString, long rowNumber) {
        String trimmed = dateString.trim();
        if (trimmed.isEmpty()) {
            log.warn("Date string is empty at row {}", rowNumber);
            throw new InvalidCsvDataException("Date cannot be empty", (int) rowNumber);
        }
        try {
            YearMonth yearMonth = YearMonth.parse(trimmed, DateTimeFormatter.ofPattern(DATE_PATTERN));
            LocalDate date = yearMonth.atDay(1);
            LocalDateTime result = date.atStartOfDay();
            log.debug("Date at row {} parsed successfully: {}", rowNumber, dateString);
            return result;
        } catch (DateTimeParseException e) {
            log.error("Invalid date format '{}' at row {}, expected {}", dateString, rowNumber, DATE_PATTERN);
            throw new InvalidCsvDataException(
                "Invalid date format '" + dateString + "', expected " + DATE_PATTERN,
                (int) rowNumber, e);
        }
    }

    /**
     * Safely retrieves and validates a string value.
     * @param value the string value to validate
     * @param rowNumber the row number for error reporting
     * @param fieldName the field name for error messages
     * @return trimmed string value
     * @throws InvalidCsvDataException if value is empty
     */
    @Nonnull
    public static String getSafeStringValue(String value, long rowNumber, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            log.warn("Required field '{}' is empty at row {}", fieldName, rowNumber);
            throw new InvalidCsvDataException(fieldName + " cannot be empty", (int) rowNumber);
        }
        String trimmed = value.trim();
        log.debug("Field '{}' at row {} has value: {}", fieldName, rowNumber, trimmed);
        return trimmed;
    }
}
