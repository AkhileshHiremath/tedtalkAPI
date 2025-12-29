package org.tedtalk.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.tedtalk.api.controller.dtos.TedTalkDataVO;
import org.tedtalk.api.controller.response.CSVImportResponse;
import org.tedtalk.api.controller.response.StatsResponse;
import org.tedtalk.api.model.TedTalk;
import org.tedtalk.api.services.InfluenceSpeakerService;
import org.tedtalk.api.services.interfaces.CsvImportService;
import org.tedtalk.api.services.interfaces.SpeakerInfluenceService;
import org.tedtalk.api.services.interfaces.TedTalkDataService;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TedTalkControllerTest {

    @Mock
    private TedTalkDataService tedTalkDataService;

    @Mock
    private CsvImportService csvImportService;

    @Mock
    private SpeakerInfluenceService speakerInfluenceService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        TedTalkController controller = new TedTalkController(
                tedTalkDataService,
                csvImportService,
                speakerInfluenceService
        );
        this.webTestClient = WebTestClient
                .bindToController(controller)
                .configureClient()
                .build();
    }

    @Test
    void importCsv_ValidFile_ShouldReturnAccepted() {
        
        when(csvImportService.isValidCsvFile(any())).thenReturn(true);
        when(csvImportService.importData(any())).thenReturn(Mono.empty());
        when(csvImportService.getSkippedCount()).thenReturn(0);
        when(csvImportService.getImportWarnings()).thenReturn(List.of());
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(10L));

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "dummy content".getBytes())
                .filename("csvtest.csv")
                .contentType(MediaType.TEXT_PLAIN);

        
        webTestClient
                .post()
                .uri("/api/v1/talks/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CSVImportResponse.class)
                .value(response -> {
                    assertEquals("CSV import accepted and processed successfully", response.getMessage());
                    assertEquals(10L, response.getRecordsImported());
                });
    }

    @Test
    void importCsv_InvalidFileExtension_ShouldReturnBadRequest() {
        
        when(csvImportService.isValidCsvFile(any())).thenReturn(false);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "dummy content".getBytes())
                .filename("csvtest.txt")
                .contentType(MediaType.TEXT_PLAIN);

        
        webTestClient
                .post()
                .uri("/api/v1/talks/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(CSVImportResponse.class)
                .isEqualTo(new CSVImportResponse("Invalid file format. Only CSV files are accepted", 0L));
    }

    @Test
    void getById_ExistingId_ShouldReturnTedTalk() {
        
        LocalDateTime date = LocalDateTime.of(2023, 1, 1, 10, 0);
        TedTalk tedTalk = new TedTalk(1L, "Title", "Author", date, 1000L, 100L, "link");
        when(tedTalkDataService.findById(1L)).thenReturn(Mono.just(Optional.of(tedTalk)));

        
        webTestClient
                .get()
                .uri("/api/v1/talks/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(TedTalkDataVO.class)
                .isEqualTo(TedTalkDataVO.fromEntity(tedTalk));
    }

    @Test
    void getById_NonExistingId_ShouldReturnNotFound() {
        
        lenient().when(tedTalkDataService.findById(1L)).thenReturn(Mono.just(Optional.empty()));

        
        webTestClient
                .get()
                .uri("/api/v1/talks/1")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getByYear_ExistingYear_ShouldReturnList() {
        
        LocalDateTime date = LocalDateTime.of(2023, 1, 1, 10, 0);
        TedTalk tedTalk = new TedTalk(1L, "Title", "Author", date, 1000L, 100L, "link");
        when(tedTalkDataService.fetchTedtalkByYear(2023)).thenReturn(Mono.just(List.of(tedTalk)));

        
        webTestClient
                .get()
                .uri("/api/v1/talks/year/2023")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TedTalkDataVO.class)
                .hasSize(1);
    }

    @Test
    void getByYear_NonExistingYear_ShouldReturnNotFound() {
        
        lenient().when(tedTalkDataService.fetchTedtalkByYear(2023)).thenReturn(Mono.just(List.of()));

        
        webTestClient
                .get()
                .uri("/api/v1/talks/year/2023")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getMostInfluentialSpeakers_ShouldReturnList() {
        
        InfluenceSpeakerService.InfluentialSpeaker speaker =
            new InfluenceSpeakerService.InfluentialSpeaker("Author", 1, 1000L, 100L, 1100.0);
        when(speakerInfluenceService.findMostInfluential(5)).thenReturn(Mono.just(List.of(speaker)));

        
        webTestClient
                .get()
                .uri("/api/v1/talks/influence/speakers?limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InfluenceSpeakerService.InfluentialSpeaker.class)
                .hasSize(1);
    }

    @Test
    void getAll_ShouldReturnPagedResponse() {
        
        TedTalk tedTalk = new TedTalk(1L, "Title", "Author", LocalDateTime.now(), 1000L, 100L, "link");
        when(tedTalkDataService.retreiveAllTedTalkData()).thenReturn(Mono.just(List.of(tedTalk)));
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(1L));

        
        webTestClient
                .get()
                .uri("/api/v1/talks?page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].title").isEqualTo("Title")
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(20);
    }

    @Test
    void getStats_ShouldReturnStats() {
        
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(100L));

        
        webTestClient
                .get()
                .uri("/api/v1/talks/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(StatsResponse.class)
                .isEqualTo(new StatsResponse(100L));
    }

    @Test
    void getAll_WithCustomPageSize_ShouldReturnPagedResponse() {
        // Given
        TedTalk tedTalk = new TedTalk(1L, "Title", "Author", LocalDateTime.now(), 1000L, 100L, "link");
        when(tedTalkDataService.retreiveAllTedTalkData()).thenReturn(Mono.just(List.of(tedTalk)));
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(1L));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks?page=1&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page").isEqualTo(1)
                .jsonPath("$.size").isEqualTo(10);
    }

    @Test
    void getAll_WithEmptyResults_ShouldReturnEmptyPage() {
        // Given
        when(tedTalkDataService.retreiveAllTedTalkData()).thenReturn(Mono.just(List.of()));
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(0L));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks?page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isArray()
                .jsonPath("$.data").isEmpty()
                .jsonPath("$.totalElements").isEqualTo(0);
    }

    @Test
    void getMostInfluentialSpeakers_WithCustomLimit_ShouldReturnList() {
        // Given
        InfluenceSpeakerService.InfluentialSpeaker speaker1 =
            new InfluenceSpeakerService.InfluentialSpeaker("Author1", 2, 2000L, 200L, 2200.0);
        InfluenceSpeakerService.InfluentialSpeaker speaker2 =
            new InfluenceSpeakerService.InfluentialSpeaker("Author2", 1, 1000L, 100L, 1100.0);
        when(speakerInfluenceService.findMostInfluential(10)).thenReturn(Mono.just(List.of(speaker1, speaker2)));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/influence/speakers?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InfluenceSpeakerService.InfluentialSpeaker.class)
                .hasSize(2);
    }

    @Test
    void getMostInfluentialSpeakers_WithDefaultLimit_ShouldReturnList() {
        // Given
        InfluenceSpeakerService.InfluentialSpeaker speaker =
            new InfluenceSpeakerService.InfluentialSpeaker("Author", 1, 1000L, 100L, 1100.0);
        when(speakerInfluenceService.findMostInfluential(5)).thenReturn(Mono.just(List.of(speaker)));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/influence/speakers")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InfluenceSpeakerService.InfluentialSpeaker.class)
                .hasSize(1);
    }

    @Test
    void getMostInfluentialSpeakers_WithMaxLimit_ShouldReturnList() {
        // Given - Test max limit boundary
        List<InfluenceSpeakerService.InfluentialSpeaker> speakers = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            speakers.add(new InfluenceSpeakerService.InfluentialSpeaker("Author" + i, 1, 1000L, 100L, 1100.0));
        }
        when(speakerInfluenceService.findMostInfluential(100)).thenReturn(Mono.just(speakers));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/influence/speakers?limit=100")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InfluenceSpeakerService.InfluentialSpeaker.class)
                .hasSize(100);
    }

    @Test
    void getMostInfluentialSpeakers_WithDatabaseError_ShouldReturnInternalServerError() {
        // Given
        when(speakerInfluenceService.findMostInfluential(5))
            .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/influence/speakers")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void importCsv_WithImportError_ShouldHandleGracefully() {
        // Given
        when(csvImportService.isValidCsvFile(any())).thenReturn(true);
        when(csvImportService.importData(any())).thenReturn(Mono.error(new RuntimeException("Import failed")));
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(0L));

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "dummy content".getBytes())
                .filename("csvtest.csv")
                .contentType(MediaType.TEXT_PLAIN);

        // When & Then
        webTestClient
                .post()
                .uri("/api/v1/talks/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getByYear_WithMultipleTalks_ShouldReturnAll() {
        // Given
        LocalDateTime date1 = LocalDateTime.of(2023, 1, 1, 10, 0);
        LocalDateTime date2 = LocalDateTime.of(2023, 6, 15, 14, 30);
        TedTalk tedTalk1 = new TedTalk(1L, "Title 1", "Author 1", date1, 1000L, 100L, "link1");
        TedTalk tedTalk2 = new TedTalk(2L, "Title 2", "Author 2", date2, 2000L, 200L, "link2");
        when(tedTalkDataService.fetchTedtalkByYear(2023)).thenReturn(Mono.just(List.of(tedTalk1, tedTalk2)));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/year/2023")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TedTalkDataVO.class)
                .hasSize(2);
    }

    @Test
    void getById_WithDatabaseError_ShouldReturnInternalServerError() {
        // Given
        when(tedTalkDataService.findById(1L)).thenReturn(Mono.error(new RuntimeException("Database connection failed")));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/1")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getByYear_WithDatabaseError_ShouldReturnInternalServerError() {
        // Given
        when(tedTalkDataService.fetchTedtalkByYear(2023)).thenReturn(Mono.error(new RuntimeException("Database error")));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/year/2023")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getMostInfluentialSpeakers_WithError_ShouldReturnInternalServerError() {
        // Given
        when(speakerInfluenceService.findMostInfluential(5)).thenReturn(Mono.error(new RuntimeException("Calculation error")));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/influence/speakers?limit=5")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getAll_WithDatabaseError_ShouldReturnInternalServerError() {
        // Given
        when(tedTalkDataService.retreiveAllTedTalkData()).thenReturn(Mono.error(new RuntimeException("Database error")));
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(0L));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks?page=0&size=20")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getStats_WithDatabaseError_ShouldReturnInternalServerError() {
        // Given
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.error(new RuntimeException("Database error")));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/stats")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void importCsv_WithSuccessfulImportAndcount_ShouldReturnAcceptedWithcount() {
        // Given
        when(csvImportService.isValidCsvFile(any())).thenReturn(true);
        when(csvImportService.importData(any())).thenReturn(Mono.empty());
        when(csvImportService.getSkippedCount()).thenReturn(0);
        when(csvImportService.getImportWarnings()).thenReturn(List.of());
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(42L));

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "title,author,date,views,likes,link\nTalk,Speaker,January 2020,1000,100,link".getBytes())
                .filename("data.csv")
                .contentType(MediaType.TEXT_PLAIN);

        // When & Then
        webTestClient
                .post()
                .uri("/api/v1/talks/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CSVImportResponse.class)
                .value(response -> {
                    assertEquals("CSV import accepted and processed successfully", response.getMessage());
                    assertEquals(42L, response.getRecordsImported());
                });
    }

    @Test
    void importCsv_WithSkippedRecords_ShouldReturnMessageWithSkipcount() {
        // Given - Import with some records skipped
        when(csvImportService.isValidCsvFile(any())).thenReturn(true);
        when(csvImportService.importData(any())).thenReturn(Mono.empty());
        when(csvImportService.getSkippedCount()).thenReturn(5);
        when(csvImportService.getImportWarnings()).thenReturn(List.of("Row 1 skipped", "Row 5 skipped"));
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(95L));

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "mixed-data.csv".getBytes())
                .filename("data.csv")
                .contentType(MediaType.TEXT_PLAIN);

        // When & Then
        webTestClient
                .post()
                .uri("/api/v1/talks/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CSVImportResponse.class)
                .value(response -> {
                    assertTrue(response.getMessage().contains("5 records skipped"));
                    assertEquals(95L, response.getRecordsImported());
                    assertEquals(5L, response.getRecordsSkipped());
                    assertEquals(2, response.getWarnings().size());
                });
    }

    @Test
    void getAll_WithLargePageSize_ShouldRespectMaxLimit() {
        // Given
        TedTalk tedTalk = new TedTalk(1L, "Title", "Author", LocalDateTime.now(), 1000L, 100L, "link");
        when(tedTalkDataService.retreiveAllTedTalkData()).thenReturn(Mono.just(List.of(tedTalk)));
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(1L));

        // When & Then - Using max page size of 100
        webTestClient
                .get()
                .uri("/api/v1/talks?page=0&size=100")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.size").isEqualTo(100)
                .jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void getMostInfluentialSpeakers_WithMaxLimit_ShouldReturn() {
        // Given
        InfluenceSpeakerService.InfluentialSpeaker speaker =
            new InfluenceSpeakerService.InfluentialSpeaker("Author", 1, 1000L, 100L, 1100.0);
        when(speakerInfluenceService.findMostInfluential(100)).thenReturn(Mono.just(List.of(speaker)));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/influence/speakers?limit=100")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InfluenceSpeakerService.InfluentialSpeaker.class)
                .hasSize(1);
    }

    @Test
    void getByYear_WithMinYear_ShouldReturnTalks() {
        // Given - Testing with minimum year 1984
        LocalDateTime date = LocalDateTime.of(1984, 1, 1, 10, 0);
        TedTalk tedTalk = new TedTalk(1L, "First TED Talk", "Author", date, 1000L, 100L, "link");
        when(tedTalkDataService.fetchTedtalkByYear(1984)).thenReturn(Mono.just(List.of(tedTalk)));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/year/1984")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TedTalkDataVO.class)
                .hasSize(1);
    }

    @Test
    void getAll_WithZeroPage_ShouldReturnFirstPage() {
        // Given
        TedTalk tedTalk = new TedTalk(1L, "Title", "Author", LocalDateTime.now(), 1000L, 100L, "link");
        when(tedTalkDataService.retreiveAllTedTalkData()).thenReturn(Mono.just(List.of(tedTalk)));
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(1L));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks?page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.totalPages").isEqualTo(1);
    }

    @Test
    void getAll_WithMultiplePages_ShouldCalculateTotalPages() {
        // Given
        TedTalk tedTalk = new TedTalk(1L, "Title", "Author", LocalDateTime.now(), 1000L, 100L, "link");
        when(tedTalkDataService.retreiveAllTedTalkData()).thenReturn(Mono.just(List.of(tedTalk)));
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(50L)); // 50 items with size 20 = 3 pages

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks?page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalPages").isEqualTo(3)
                .jsonPath("$.totalElements").isEqualTo(50);
    }

    @Test
    void importCsv_WithZeroSkipped_ShouldNotContainSkipMessage() {
        // Testing the FALSE branch of skipped > 0
        when(csvImportService.isValidCsvFile(any())).thenReturn(true);
        when(csvImportService.importData(any())).thenReturn(Mono.empty());
        when(csvImportService.getSkippedCount()).thenReturn(0);
        when(csvImportService.getImportWarnings()).thenReturn(List.of());
        when(tedTalkDataService.fetchTotalTedTalkCount()).thenReturn(Mono.just(100L));

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "clean-data.csv".getBytes())
                .filename("data.csv")
                .contentType(MediaType.TEXT_PLAIN);

        // When & Then
        webTestClient
                .post()
                .uri("/api/v1/talks/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CSVImportResponse.class)
                .value(response -> {
                    assertFalse(response.getMessage().contains("skipped"));
                    assertEquals("CSV import accepted and processed successfully", response.getMessage());
                    assertEquals(100L, response.getRecordsImported());
                    assertEquals(0L, response.getRecordsSkipped());
                });
    }

    @Test
    void getByYear_WithEmptyList_ShouldReturn404() {
        // Testing the isEmpty() == true branch
        when(tedTalkDataService.fetchTedtalkByYear(1980)).thenReturn(Mono.just(List.of()));

        // When & Then
        webTestClient
                .get()
                .uri("/api/v1/talks/year/1980")
                .exchange()
                .expectStatus().isNotFound();
    }
}

