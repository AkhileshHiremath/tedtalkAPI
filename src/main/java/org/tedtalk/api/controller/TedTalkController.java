package org.tedtalk.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.tedtalk.api.controller.response.CSVImportResponse;
import org.tedtalk.api.controller.response.StatsResponse;
import org.tedtalk.api.controller.dtos.TedTalkDataVO;
import org.tedtalk.api.controller.response.PagedResponse;
import org.tedtalk.api.services.interfaces.CsvImportService;
import org.tedtalk.api.services.interfaces.SpeakerInfluenceService;
import org.tedtalk.api.services.interfaces.TedTalkDataService;
import org.tedtalk.api.services.InfluenceSpeakerService;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST API controller for managing TED Talks.
 *
 * This controller handles all HTTP requests related to TED talk data, including:
 * - Importing talk data from CSV files
 * - Retrieving individual talks or collections
 * - Calculating speaker influence metrics
 * - Providing statistics
 *
 * Security: Most endpoints require authentication. CSV import requires ADMIN role.
 *
 * @author Akhilesh Hiremath
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/talks")
@RequiredArgsConstructor
@Tag(name = "TED Talks", description = "TED Talks management API")
public class TedTalkController {

    // Pagination constraints - keep page sizes reasonable to avoid memory issues
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_NUMBER = 0;

    // TED started in 1984, so that's our minimum year
    private static final int MIN_YEAR = 1984;
    private static final int MAX_YEAR = 9999;


    private static final String INVALID_FILE_FORMAT = "Invalid file format. Only CSV files are accepted";
    private static final String SUCCESSFUL_CSV_PROCESSING = "CSV import accepted and processed successfully";

    private final TedTalkDataService tedTalkDataService;
    private final CsvImportService csvImportService;
    private final SpeakerInfluenceService speakerInfluenceService;

    /**
     * Imports TED talk data from a CSV file.
     *
     * This is an async operation - we accept the file, start processing it in the background,
     * and immediately return a 202 Accepted response. This prevents timeouts on large files.
     *
     * Only admins can import data (enforced by SecurityConfig, not here).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Import TED talks data from CSV file",
               description = "Accepts CSV file and imports TED talks data asynchronously. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "CSV import accepted for processing",
                    content = @Content(schema = @Schema(implementation = CSVImportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file format or bad request"),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
            @ApiResponse(responseCode = "415", description = "Unsupported media type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<ResponseEntity<CSVImportResponse>> importCsv(
            @Parameter(description = "CSV file containing TED talks data", required = true)
            @RequestPart("file") FilePart file) {

        log.info("Received CSV import request for file: {}", file.filename());

        // Quick validation before we start any processing
        if (!csvImportService.isValidCsvFile(file.filename())) {
            log.warn("Rejected invalid file format: {}", file.filename());
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new CSVImportResponse(INVALID_FILE_FORMAT, 0L)));
        }

        // Start the import process and return immediately with 202 Accepted
        return csvImportService.importData(file)
                .then(tedTalkDataService.fetchTotalTedTalkCount())  // Get the new total count
                .map(count -> {
                    // Get import statistics from service
                    long skipped = csvImportService.getSkippedCount();
                    List<String> warnings = csvImportService.getImportWarnings();

                    String message = skipped > 0
                        ? String.format("%s (%d records skipped due to errors)", SUCCESSFUL_CSV_PROCESSING, skipped)
                        : SUCCESSFUL_CSV_PROCESSING;

                    log.info("Successfully imported data. Total: {}, Skipped: {}", count, skipped);
                    return ResponseEntity
                            .status(HttpStatus.ACCEPTED)
                            .body(new CSVImportResponse(message, count, skipped, warnings));
                })
                .onErrorResume(e -> {
                    // Log the full error for debugging, but return a simple message to the user
                    log.error("Failed to import CSV file: {}", file.filename(), e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new CSVImportResponse("Failed to import CSV: " + e.getMessage(), 0L, 0L, List.of())));
                });
    }

    /**
     * Retrieves a single TED talk by its ID.
     * Simple lookup - returns 404 if not found.
     *
     * Java 21: Uses pattern matching with records for cleaner optional handling
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get TED talk by ID", description = "Returns a single TED talk by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "TED talk found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "404", description = "TED talk not found")
    })
    public Mono<ResponseEntity<TedTalkDataVO>> getById(
            @Parameter(description = "TED talk ID", required = true)
            @PathVariable Long id) {

        log.debug("Looking up TED talk with ID: {}", id);

        return tedTalkDataService.findById(id)
                .map(optional -> optional
                        .map(TedTalkDataVO::fromEntity)  // Java 21: Method references are cleaner
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error fetching TED talk with ID: {}", id, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Find all talks from a specific year.
     * Useful for seeing how TED topics evolved over time.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/year/{year}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get TED talks by year", description = "Returns all TED talks from a specific year")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "TED talks retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "404", description = "No talks found for the specified year")
    })
    public Mono<ResponseEntity<?>> getByYear(
            @Parameter(description = "Year (TED started in 1984)")
            @PathVariable @Min(MIN_YEAR) @Max(MAX_YEAR) int year) {

        log.debug("Fetching TED talks for year: {}", year);

        return tedTalkDataService.fetchTedtalkByYear(year)
                .map(talks -> {
                    if (talks.isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }
                    // Convert domain objects to DTOs before sending to client
                    List<TedTalkDataVO> dtos = talks.stream()
                            .map(TedTalkDataVO::fromEntity)
                            .toList();
                    return ResponseEntity.ok(dtos);
                })
                .onErrorResume(e -> {
                    log.error("Error fetching TED talks for year: {}", year, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Get the most influential speakers based on engagement metrics.
     *
     * We calculate influence using total views + likes across all their talks.
     * This helps identify which speakers really resonate with audiences.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/influence/speakers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get most influential speakers",
               description = "Returns the most influential speakers ranked by engagement (views + likes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Speaker influence calculated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    public Mono<ResponseEntity<List<InfluenceSpeakerService.InfluentialSpeaker>>> getMostInfluentialSpeakers(
            @Parameter(description = "Number of speakers to return (max 100)")
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int limit) {

        log.info("Calculating top {} influential speakers", limit);

        return speakerInfluenceService.findMostInfluential(limit)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error calculating speaker influence", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Get all talks with pagination.
     *
     * Important: We use pagination here because the dataset can be large (5000+ talks).
     * Loading everything at once would kill performance and memory.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all TED talks",
               description = "Returns all TED talks with pagination. Use page and size parameters to control results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "TED talks retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    public Mono<ResponseEntity<PagedResponse<TedTalkDataVO>>> getAll(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (max 100 to avoid memory issues)")
            @RequestParam(defaultValue = "20") @Min(DEFAULT_PAGE_NUMBER) @Max(MAX_PAGE_SIZE) int size) {

        log.debug("Fetching page {} of TED talks (size: {})", page, size);

        // Fetch both data and total count in parallel for better performance
        return Mono.zip(
                        tedTalkDataService.retreiveAllTedTalkData(),
                        tedTalkDataService.fetchTotalTedTalkCount()
                )
                .map(tuple -> {
                    List<TedTalkDataVO> dtos = tuple.getT1().stream()
                            .map(TedTalkDataVO::fromEntity)
                            .toList();
                    long totalElements = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) totalElements / size);

                    return ResponseEntity.ok(new PagedResponse<>(
                            dtos,
                            page,
                            size,
                            totalElements,
                            totalPages
                    ));
                })
                .onErrorResume(e -> {
                    log.error("Error fetching TED talks", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Get simple statistics about the database.
     * Currently just returns total count, but could be extended with more metrics.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get TED talks statistics",
               description = "Returns statistics about TED talks in the database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    public Mono<ResponseEntity<StatsResponse>> getStats() {
        log.debug("Fetching TED talks statistics");

        return tedTalkDataService.fetchTotalTedTalkCount()
                .map(count -> ResponseEntity.ok(new StatsResponse(count)))
                .onErrorResume(e -> {
                    log.error("Error fetching statistics", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
