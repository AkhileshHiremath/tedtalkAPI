package org.tedtalk.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.tedtalk.api.exceptions.CsvImportException;
import org.tedtalk.api.exceptions.InvalidCsvFormatException;
import org.tedtalk.api.exceptions.MissingCsvColumnException;
import org.tedtalk.api.model.TedTalk;
import org.tedtalk.api.repository.TedTalkRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TedTalkCSVImporterServiceTest {

    @Mock
    private TedTalkRepository tedTalkRepository;

    @Mock
    private org.tedtalk.api.utils.validators.FileValidator fileValidator;

    @Mock
    private FilePart filePart;

    @InjectMocks
    private TedTalkCSVImporterService importerService;

    @BeforeEach
    void setUp() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(1000L);
        lenient().when(filePart.headers()).thenReturn(headers);
        lenient().when(filePart.filename()).thenReturn("test.csv");
    }

    @Test
    void importCsv_ValidCsv_ShouldSaveTedTalks() {
        
        // Using correct date format: "MMMM yyyy" (e.g., "January 2023")
        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,January 2023,1000,100,http://link1\n" +
                "Talk2,Author2,February 2023,2000,200,http://link2";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(2, savedTalks.size());
        assertEquals("Talk1", savedTalks.get(0).getTitle());
        assertEquals("Author1", savedTalks.get(0).getAuthor());
        assertEquals(1000L, savedTalks.get(0).getViews());
        assertEquals("Talk2", savedTalks.get(1).getTitle());
    }

    @Test
    void importCsv_ReadFileError_ShouldThrowCsvImportException() {

        when(filePart.content()).thenReturn(Flux.error(new RuntimeException("Read error")));

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert - Read errors propagate as RuntimeException
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        verify(tedTalkRepository, never()).saveAll(anyList());
    }

    @Test
    void importCsv_MissingRequiredColumn_ShouldThrowMissingCsvColumnException() {
        // Given - CSV without 'title' column
        String csvContent = "author,date,views,likes,link\n" +
                "Author1,January 2023,1000,100,http://link1";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert
        StepVerifier.create(result)
                .expectError(MissingCsvColumnException.class)
                .verify();
        verify(tedTalkRepository, never()).saveAll(anyList());
    }

    @Test
    void importCsv_InvalidDateFormat_ShouldSkipRowAndContinue() {
        // Given - Invalid date format
        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,2023-01-01,1000,100,http://link1\n" +
                "Talk2,Author2,January 2023,2000,200,http://link2";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert - Should skip invalid row and import valid one
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(1, savedTalks.size());
        assertEquals("Talk2", savedTalks.get(0).getTitle());
    }

    @Test
    void importCsv_InvalidNumericValue_ShouldSkipRowAndContinue() {
        // Given - Invalid numeric value for views
        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,January 2023,invalid,100,http://link1\n" +
                "Talk2,Author2,February 2023,2000,200,http://link2";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert - Should skip invalid row and import valid one
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(1, savedTalks.size());
        assertEquals("Talk2", savedTalks.get(0).getTitle());
        // Note: skippedCount is cleared in doFinally, so we can't check it here
    }

    @Test
    void importCsv_EmptyRequiredField_ShouldSkipRowAndContinue() {
        // Given - Empty title field
        String csvContent = "title,author,date,views,likes,link\n" +
                ",Author1,January 2023,1000,100,http://link1\n" +
                "Talk2,Author2,February 2023,2000,200,http://link2";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert - Should skip invalid row and import valid one
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(1, savedTalks.size());
        assertEquals("Talk2", savedTalks.get(0).getTitle());
    }

    @Test
    void isValidCsvFile_WithCsvExtension_ShouldReturnTrue() {
        // Given
        String filename = "test.csv";
        when(fileValidator.isValidCsvFile(filename)).thenReturn(true);

        // When
        boolean result = importerService.isValidCsvFile(filename);

        // Then
        assertEquals(true, result);
    }

    @Test
    void isValidCsvFile_WithUpperCaseExtension_ShouldReturnTrue() {
        // Given
        String filename = "test.CSV";
        when(fileValidator.isValidCsvFile(filename)).thenReturn(true);

        // When
        boolean result = importerService.isValidCsvFile(filename);

        // Then
        assertEquals(true, result);
    }

    @Test
    void isValidCsvFile_WithNonCsvExtension_ShouldReturnFalse() {
        // Given
        String filename = "test.txt";
        when(fileValidator.isValidCsvFile(filename)).thenReturn(false);

        // When
        boolean result = importerService.isValidCsvFile(filename);

        // Then
        assertEquals(false, result);
    }

    @Test
    void isValidCsvFile_WithNoExtension_ShouldReturnFalse() {
        // Given
        String filename = "test";
        when(fileValidator.isValidCsvFile(filename)).thenReturn(false);

        // When
        boolean result = importerService.isValidCsvFile(filename);

        // Then
        assertEquals(false, result);
    }

    @Test
    void importCsv_WithLargeValidData_ShouldSaveAll() {
        // Given
        StringBuilder csvContent = new StringBuilder("title,author,date,views,likes,link\n");
        for (int i = 1; i <= 100; i++) {
            csvContent.append(String.format("Talk%d,Author%d,January 2023,%d,%d,http://link%d\n",
                i, i, i * 1000, i * 100, i));
        }
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.toString().getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(100, savedTalks.size());
    }

    @Test
    void importCsv_WithFileSizeTooLarge_ShouldThrowException() {
        // Given - File larger than 10MB
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(11 * 1024 * 1024L); // 11 MB
        when(filePart.headers()).thenReturn(headers);

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert
        StepVerifier.create(result)
                .expectError(org.tedtalk.api.exceptions.CsvImportException.class)
                .verify();
        verify(tedTalkRepository, never()).saveAll(anyList());
    }

    @Test
    void importCsv_WithExactMaxFileSize_ShouldProcess() {
        // Given - File exactly at 10MB limit
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(10 * 1024 * 1024L); // Exactly 10 MB
        when(filePart.headers()).thenReturn(headers);

        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,January 2023,1000,100,http://link1";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void importCsv_WithWhitespaceInFields_ShouldTrimAndSave() {
        // Given - CSV with whitespace
        String csvContent = "title,author,date,views,likes,link\n" +
                "  Talk1  ,  Author1  ,January 2023,  1000  ,  100  ,  http://link1  ";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(1, savedTalks.size());
        assertEquals("Talk1", savedTalks.get(0).getTitle());
        assertEquals("Author1", savedTalks.get(0).getAuthor());
    }

    @Test
    void importCsv_WithDifferentMonths_ShouldParseCorrectly() {
        // Given - CSV with different month formats
        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,January 2023,1000,100,http://link1\n" +
                "Talk2,Author2,February 2023,2000,200,http://link2\n" +
                "Talk3,Author3,December 2023,3000,300,http://link3";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(3, savedTalks.size());
    }

    @Test
    void importCsv_WithZeroViewsAndLikes_ShouldSave() {
        // Given - CSV with zero values
        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,January 2023,0,0,http://link1";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(1, savedTalks.size());
        assertEquals(0L, savedTalks.get(0).getViews());
        assertEquals(0L, savedTalks.get(0).getLikes());
    }

    @Test
    void importCsv_WithVeryLargeNumbers_ShouldSave() {
        // Given - CSV with very large numbers
        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,January 2023,999999999,888888888,http://link1";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(1, savedTalks.size());
        assertEquals(999999999L, savedTalks.get(0).getViews());
        assertEquals(888888888L, savedTalks.get(0).getLikes());
    }

    @Test
    void importCsv_WithOnlyHeaders_ShouldThrowException() {
        // Given - CSV with only headers, no data
        String csvContent = "title,author,date,views,likes,link\n";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert - Should throw exception when no valid records
        StepVerifier.create(result)
                .expectError(org.tedtalk.api.exceptions.CsvImportException.class)
                .verify();
        verify(tedTalkRepository, never()).saveAll(anyList());
    }

    @Test
    void importData_ShouldCallImportCsvData() {
        // Given
        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,January 2023,1000,100,http://link1";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importData(filePart);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void importCsv_AllRowsInvalid_ShouldThrowException() {
        // Given - All rows have invalid data
        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,InvalidDate,invalid,100,http://link1\n" +
                "Talk2,Author2,BadDate,abc,200,http://link2";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert - Should throw exception when no valid records
        StepVerifier.create(result)
                .expectError(CsvImportException.class)
                .verify();
        verify(tedTalkRepository, never()).saveAll(anyList());
    }

    @Test
    void importCsv_MixedValidAndInvalid_ShouldImportValidOnly() {
        // Given - Mix of valid and invalid rows
        String csvContent = "title,author,date,views,likes,link\n" +
                "Talk1,Author1,January 2023,1000,100,http://link1\n" +
                "Talk2,Author2,InvalidDate,2000,200,http://link2\n" +
                "Talk3,Author3,March 2023,abc,300,http://link3\n" +
                "Talk4,Author4,April 2023,4000,400,http://link4";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert - Should import 2 valid rows and skip 2 invalid
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(2, savedTalks.size());
        assertEquals("Talk1", savedTalks.get(0).getTitle());
        assertEquals("Talk4", savedTalks.get(1).getTitle());
    }

    @Test
    void importCsv_WithAbcdInViewsField_ShouldSkipRowAndContinue() {
        // Given - CSV with 'abcd' in views field (simulating row 140 issue)
        String csvContent = "title,author,date,views,likes,link\n" +
                "How one of the most profitable companies in history rose to power,Adam Clulow,December 2021,abcd,19000,http://link1\n" +
                "Valid Talk,Author2,January 2023,1000,100,http://link2";
        when(filePart.content()).thenReturn(Flux.just(
                new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8))
        ));
        when(tedTalkRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        Mono<Void> result = importerService.importCsvData(filePart);

        // Assert - Should skip row with 'abcd' and import valid row
        StepVerifier.create(result)
                .verifyComplete();
        ArgumentCaptor<List<TedTalk>> captor = ArgumentCaptor.forClass(List.class);
        verify(tedTalkRepository).saveAll(captor.capture());
        List<TedTalk> savedTalks = captor.getValue();
        assertEquals(1, savedTalks.size());
        assertEquals("Valid Talk", savedTalks.get(0).getTitle());
        assertEquals("Author2", savedTalks.get(0).getAuthor());
    }
}


