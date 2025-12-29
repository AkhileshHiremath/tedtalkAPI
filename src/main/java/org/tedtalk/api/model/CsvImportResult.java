package org.tedtalk.api.model;

/**
 * Sealed interface for CSV import results using Java 21 sealed classes.
 *
 * Sealed classes give us exhaustive pattern matching - the compiler knows
 * all possible subtypes and can check we've handled them all.
 *
 * This is much safer than traditional inheritance where new subtypes
 * could be added anywhere.
 */
public sealed interface CsvImportResult
    permits CsvImportResult.Success, CsvImportResult.PartialSuccess, CsvImportResult.Failure {

    /**
     * Successful import - all rows processed.
     * Using a record here for immutability and conciseness.
     */
    record Success(long recordsImported, String message) implements CsvImportResult {
        public Success {
            // Compact constructor for validation (Java 21 feature)
            if (recordsImported < 0) {
                throw new IllegalArgumentException("Records imported cannot be negative");
            }
        }
    }

    /**
     * Partial success - some rows failed but others succeeded.
     */
    record PartialSuccess(
        long recordsImported,
        long recordsFailed,
        String message
    ) implements CsvImportResult {
        public PartialSuccess {
            if (recordsImported < 0 || recordsFailed < 0) {
                throw new IllegalArgumentException("Record counts cannot be negative");
            }
        }
    }

    /**
     * Complete failure - nothing was imported.
     */
    record Failure(String errorMessage, Throwable cause) implements CsvImportResult {
        public Failure {
            if (errorMessage == null || errorMessage.isBlank()) {
                throw new IllegalArgumentException("Error message cannot be blank");
            }
        }
    }

    /**
     * Java 21 Pattern Matching: Handle all possible outcomes exhaustively.
     * The compiler ensures we've covered all cases!
     */
    default String getStatusMessage() {
        return switch (this) {
            case Success(var count, var msg) ->
                " Success: Imported %d records. %s".formatted(count, msg);
            case PartialSuccess(var imported, var failed, var msg) ->
                " Partial: Imported %d, Failed %d. %s".formatted(imported, failed, msg);
            case Failure(var error, var cause) ->
                " Failed: %s".formatted(error);
        };
    }

    /**
     * Check if the import was at least partially successful.
     */
    default boolean isSuccessful() {
        return switch (this) {
            case Success s -> true;
            case PartialSuccess ps -> ps.recordsImported() > 0;
            case Failure f -> false;
        };
    }
}

