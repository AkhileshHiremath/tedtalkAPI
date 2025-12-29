package org.tedtalk.api.services.interfaces;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CsvImportService {

    /**
     * Imports TED talk data from a CSV file.
     *
     * @param file the CSV file to import
     * @return Mono that completes when import is done
     */
    Mono<Void> importData(FilePart file);

    /**
     * Validates if a file has a valid CSV extension.
     *
     * @param filename the filename to validate
     * @return true if valid CSV file
     */
    boolean isValidCsvFile(String filename);

    /**
     * Gets warnings from the last import operation.
     *
     * @return list of warning messages
     */
    List<String> getImportWarnings();

    /**
     * Gets the number of skipped records from the last import.
     *
     * @return count of skipped records
     */
    int getSkippedCount();
}

