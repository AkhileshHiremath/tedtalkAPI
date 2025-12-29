package org.tedtalk.api.utils.validators;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;


@Component
public class FileValidator {

    // We accept both .csv and .CSV because who knows what OS the user is on
    private static final List<String> ALLOWED_CSV_EXTENSIONS = Arrays.asList(".csv", ".CSV");

    /**
     * Check if a file is a valid CSV file.
     *
     * We're just doing a simple extension check here - good enough for our use case.
     * A more paranoid check would also look at file content/MIME type.
     *
     * @param filename the name of the file to check
     * @return true if it ends with .csv or .CSV, false otherwise
     */
    public boolean isValidCsvFile(String filename) {
        // Handle the edge cases first - null or empty strings are obviously not valid
        if (filename == null || filename.isBlank()) {
            return false;
        }

        // Check if the filename ends with any of our allowed extensions
        return ALLOWED_CSV_EXTENSIONS.stream()
                .anyMatch(filename::endsWith);
    }

    /**
     * Extract the file extension from a filename.
     *
     * For example: "data.csv" returns ".csv", "archive.tar.gz" returns ".gz"
     *
     * @param filename the filename to parse
     * @return the extension (including the dot), or empty string if no extension
     */
    public String getFileExtension(String filename) {
        // Garbage in, garbage out
        if (filename == null || filename.isBlank()) {
            return "";
        }

        // Find the last dot in the filename
        int lastDotIndex = filename.lastIndexOf('.');

        // No dot found, or dot is the last character? No extension then.
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        // Return everything from the dot onwards
        return filename.substring(lastDotIndex);
    }
}

