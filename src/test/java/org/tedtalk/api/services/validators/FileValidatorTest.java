package org.tedtalk.api.services.validators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tedtalk.api.utils.validators.FileValidator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileValidator.
 */
@ExtendWith(MockitoExtension.class)
class FileValidatorTest {

    private final FileValidator fileValidator = new FileValidator();

    @Test
    void isValidCsvFile_WithValidCsvExtension_ShouldReturnTrue() {
        
        assertTrue(fileValidator.isValidCsvFile("test.csv"));
        assertTrue(fileValidator.isValidCsvFile("data.CSV"));
        assertTrue(fileValidator.isValidCsvFile("file-name.csv"));
        assertTrue(fileValidator.isValidCsvFile("/path/to/file.csv"));
    }

    @Test
    void isValidCsvFile_WithInvalidExtension_ShouldReturnFalse() {
        
        assertFalse(fileValidator.isValidCsvFile("test.txt"));
        assertFalse(fileValidator.isValidCsvFile("test.xls"));
        assertFalse(fileValidator.isValidCsvFile("test.xlsx"));
        assertFalse(fileValidator.isValidCsvFile("test"));
    }

    @Test
    void isValidCsvFile_WithNullFilename_ShouldReturnFalse() {
        
        assertFalse(fileValidator.isValidCsvFile(null));
    }

    @Test
    void isValidCsvFile_WithEmptyFilename_ShouldReturnFalse() {
        
        assertFalse(fileValidator.isValidCsvFile(""));
        assertFalse(fileValidator.isValidCsvFile("   "));
    }

    @Test
    void getFileExtension_WithValidFilename_ShouldReturnExtension() {
        
        assertEquals(".csv", fileValidator.getFileExtension("test.csv"));
        assertEquals(".txt", fileValidator.getFileExtension("test.txt"));
        assertEquals(".xlsx", fileValidator.getFileExtension("data.xlsx"));
    }

    @Test
    void getFileExtension_WithNoExtension_ShouldReturnEmpty() {
        
        assertEquals("", fileValidator.getFileExtension("testfile"));
        assertEquals("", fileValidator.getFileExtension("test."));
    }

    @Test
    void getFileExtension_WithNullFilename_ShouldReturnEmpty() {
        
        assertEquals("", fileValidator.getFileExtension(null));
    }

    @Test
    void getFileExtension_WithEmptyFilename_ShouldReturnEmpty() {
        
        assertEquals("", fileValidator.getFileExtension(""));
        assertEquals("", fileValidator.getFileExtension("   "));
    }

    @Test
    void getFileExtension_WithMultipleDots_ShouldReturnLastExtension() {
        
        assertEquals(".csv", fileValidator.getFileExtension("file.name.csv"));
        assertEquals(".gz", fileValidator.getFileExtension("archive.tar.gz"));
    }

    @Test
    void isValidCsvFile_WithBlankFilename_ShouldReturnFalse() {
        // Testing the isBlank() branch specifically
        assertFalse(fileValidator.isValidCsvFile(" "));
        assertFalse(fileValidator.isValidCsvFile("\t"));
        assertFalse(fileValidator.isValidCsvFile("\n"));
    }

    @Test
    void getFileExtension_WithBlankSpaces_ShouldReturnEmpty() {
        // Testing trim and blank branches
        assertEquals("", fileValidator.getFileExtension(" "));
        assertEquals("", fileValidator.getFileExtension("\t\t"));
    }

    @Test
    void isValidCsvFile_WithUppercaseCsv_ShouldReturnTrue() {
        // Ensure both .csv and .CSV work
        assertTrue(fileValidator.isValidCsvFile("TEST.CSV"));
        assertTrue(fileValidator.isValidCsvFile("test.csv"));
    }

    @Test
    void getFileExtension_WithDotAtEnd_ShouldReturnEmpty() {
        // Edge case: filename ending with dot
        assertEquals("", fileValidator.getFileExtension("filename."));
        assertEquals("", fileValidator.getFileExtension("test.file."));
    }

    @Test
    void getFileExtension_WithOnlyDot_ShouldReturnEmpty() {
        // Edge case: only a dot
        assertEquals("", fileValidator.getFileExtension("."));
    }

    @Test
    void isValidCsvFile_WithHiddenFile_ShouldReturnTrue() {
        // Hidden files on Unix systems (starting with dot)
        assertTrue(fileValidator.isValidCsvFile(".hidden.csv"));
    }
}

