package org.tedtalk.api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.tedtalk.api.exceptions.CsvImportException;
import org.tedtalk.api.exceptions.InvalidCsvDataException;
import org.tedtalk.api.exceptions.MissingCsvColumnException;
import org.tedtalk.api.model.TedTalk;
import org.tedtalk.api.repository.TedTalkRepository;
import org.tedtalk.api.services.interfaces.CsvImportService;
import org.tedtalk.api.utils.FileParsingUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class TedTalkCSVImporterService implements CsvImportService {

    private final TedTalkRepository repository;
    private final org.tedtalk.api.utils.validators.FileValidator fileValidator;

    private static final String[] REQUIRED_COLUMNS = {"title", "author", "date", "views", "likes", "link"};
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private static final ThreadLocal<List<String>> importWarnings = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<AtomicInteger> skippedCount = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    @Override
    public Mono<Void> importData(FilePart file) {
        log.info("Starting CSV import for file: {}", file.filename());
        return importCsvData(file);
    }

    @Override
    public boolean isValidCsvFile(String filename) {
        return fileValidator.isValidCsvFile(filename);
    }

    /**
     * Imports CSV file content and saves TedTalk records to database reactively.
     * @param filePart the uploaded CSV file
     * @return Mono<Void> that completes when import is finished
     */
    public Mono<Void> importCsvData(FilePart filePart) {
        log.info("Starting CSV import for file: {}", filePart.filename());

        importWarnings.get().clear();
        skippedCount.get().set(0);

        return validateFileSize(filePart)
                .then(Mono.defer(() -> readFileContent(filePart)))
                .flatMapMany(this::parseCsvContent)
                .flatMap(record -> Mono.fromCallable(() -> mapToTedTalk(record))
                        .onErrorResume(InvalidCsvDataException.class, e -> {
                            String warning = String.format("Row %d skipped: %s",
                                record.getRecordNumber(), e.getMessage());
                            importWarnings.get().add(warning);
                            skippedCount.get().incrementAndGet();
                            log.warn("Skipping invalid row {}: {}", record.getRecordNumber(), e.getMessage());
                            return Mono.empty();
                        })
                        .onErrorResume(Exception.class, e -> {
                            // Catch any other exceptions (like NumberFormatException wrapped differently)
                            String warning = String.format("Row %d skipped: %s",
                                record.getRecordNumber(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                            importWarnings.get().add(warning);
                            skippedCount.get().incrementAndGet();
                            log.warn("Skipping row {} due to unexpected error: {}", record.getRecordNumber(), e.getMessage(), e);
                            return Mono.empty();
                        }))
                .collectList()
                .flatMap(talks -> {
                    if (talks.isEmpty()) {
                        log.warn("No valid records found in CSV file");
                        return Mono.error(new CsvImportException("No valid records found in CSV file"));
                    }
                    log.info("Importing {} valid records from CSV (skipped: {})",
                        talks.size(), skippedCount.get().get());
                    return Mono.fromCallable(() -> repository.saveAll(talks));
                })
                .then()
                .doOnSuccess(v -> log.info("CSV import completed successfully for file: {}", filePart.filename()))
                .doOnError(e -> log.error("CSV import failed for file: {}", filePart.filename(), e))
                .doFinally(signal -> {
                    try {
                        importWarnings.remove();
                        skippedCount.remove();
                    } catch (Exception e) {
                        log.debug("Error cleaning up ThreadLocal variables", e);
                    }
                });
    }

    public List<String> getImportWarnings() {
        List<String> warnings = importWarnings.get();
        return warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
    }

    public int getSkippedCount() {
        AtomicInteger counter = skippedCount.get();
        return counter != null ? counter.get() : 0;
    }

    private Mono<Void> validateFileSize(FilePart filePart) {
        return Mono.defer(() -> {
            long contentLength = filePart.headers().getContentLength();
            if (contentLength > MAX_FILE_SIZE) {
                log.warn("File size {} exceeds maximum allowed size {}", contentLength, MAX_FILE_SIZE);
                return Mono.error(new CsvImportException(
                        "File size exceeds maximum limit of " + (MAX_FILE_SIZE / 1024 / 1024) + " MB"));
            }
            return Mono.empty();
        });
    }

    private Mono<String> readFileContent(FilePart filePart) {
        return filePart.content()
                .reduce(new StringBuilder(), FileParsingUtil::appendDataBuffer)
                .map(StringBuilder::toString);
    }

    private Flux<CSVRecord> parseCsvContent(String content) {
        return Mono.fromCallable(() -> {
            try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
                CSVParser parser = FileParsingUtil.createCsvParser(reader);
                validateHeaders(parser);
                return parser.getRecords();
            }
        }).flatMapMany(Flux::fromIterable);
    }

    private void validateHeaders(CSVParser parser) {
        var headerMap = parser.getHeaderMap();
        for (String requiredColumn : REQUIRED_COLUMNS) {
            if (!headerMap.containsKey(requiredColumn)) {
                throw new MissingCsvColumnException(requiredColumn);
            }
        }
        log.info("All required CSV columns validated: {}", String.join(", ", REQUIRED_COLUMNS));
    }

    private TedTalk mapToTedTalk(CSVRecord record) {
        try {
            long rowNumber = record.getRecordNumber();

            String title = FileParsingUtil.getSafeStringValue(record.get("title"), rowNumber, "title");
            String author = FileParsingUtil.getSafeStringValue(record.get("author"), rowNumber, "author");
            LocalDateTime date = FileParsingUtil.parseDateSafely(record.get("date"), rowNumber);
            long views = FileParsingUtil.parseNumericValue(record.get("views"), rowNumber, "views");
            long likes = FileParsingUtil.parseNumericValue(record.get("likes"), rowNumber, "likes");
            String link = FileParsingUtil.getSafeStringValue(record.get("link"), rowNumber, "link");

            return new TedTalk(null, title, author, date, views, likes, link);
        } catch (InvalidCsvDataException e) {
            log.error("Invalid CSV data at row {}: {}", record.getRecordNumber(), e.getMessage());
            throw e;
        }
    }
}

