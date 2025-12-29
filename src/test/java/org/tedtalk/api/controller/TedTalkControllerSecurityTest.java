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
import org.tedtalk.api.services.interfaces.TedTalkDataService;
import org.tedtalk.api.services.interfaces.CsvImportService;
import org.tedtalk.api.services.interfaces.SpeakerInfluenceService;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TedTalkControllerSecurityTest {

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
        
        when(tedTalkDataService.findById(1L)).thenReturn(Mono.just(Optional.empty()));
        webTestClient
                .get()
                .uri("/api/v1/talks/1")
                .exchange()
                .expectStatus().isNotFound();
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
}

