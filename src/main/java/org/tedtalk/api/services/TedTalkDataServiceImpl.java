package org.tedtalk.api.services;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tedtalk.api.model.TedTalk;
import org.tedtalk.api.repository.TedTalkRepository;
import org.tedtalk.api.services.interfaces.TedTalkDataService;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Service for retrieving TED Talk data.
 *
 * This is our data access layer - all database operations for TED talks go through here.
 * We keep it simple and focused: just CRUD operations, no business logic.
 *
 * Why reactive (Mono)? Because our API is reactive (WebFlux), so we need to return
 * reactive types to avoid blocking threads. Think of Mono as a "promise" of future data.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TedTalkDataServiceImpl implements TedTalkDataService {

    private final TedTalkRepository tedTalkRepository;

    /**
     * Get all talks from the database.
     * Use with caution - this loads everything into memory!
     */
    @Nonnull
    @Override
    public Mono<List<TedTalk>> retreiveAllTedTalkData() {
        log.debug("Retrieving all TED talks from database");
        return Mono.fromCallable(tedTalkRepository::findAll)
                .doOnSuccess(talks -> log.debug("Retrieved {} TED talks", talks.size()))
                .doOnError(error -> log.error("Failed to retrieve TED talks", error));
    }

    /**
     * Look up a single talk by its ID.
     * Returns empty Optional if not found - that's cleaner than throwing exceptions.
     */
    @Nonnull
    @Override
    public Mono<Optional<TedTalk>> findById(Long id) {
        log.debug("Looking up TED talk with ID: {}", id);
        return Mono.fromCallable(() -> tedTalkRepository.findById(id))
                .doOnSuccess(result -> {
                    if (result.isPresent()) {
                        log.debug("Found TED talk: {}", result.get().getTitle());
                    } else {
                        log.debug("No TED talk found with ID: {}", id);
                    }
                })
                .doOnError(error -> log.error("Database error while fetching talk ID: {}", id, error));
    }

    /**
     * Find all talks from a specific year.
     * Years are extracted from the date field at query time.
     */
    @Nonnull
    @Override
    public Mono<List<TedTalk>> fetchTedtalkByYear(int year) {
        log.debug("Searching for TED talks from year: {}", year);
        return Mono.fromCallable(() -> tedTalkRepository.findByDateYear(year))
                .doOnSuccess(talks -> log.debug("Found {} talks from {}", talks.size(), year))
                .doOnError(error -> log.error("Failed to fetch talks for year: {}", year, error));
    }

    /**
     * Count total talks in the database.
     * This is cheap - just a COUNT(*) query, no data transfer.
     */
    @Nonnull
    @Override
    public Mono<Long> fetchTotalTedTalkCount() {
        log.debug("Counting total TED talks");
        return Mono.fromCallable(tedTalkRepository::count)
                .doOnSuccess(count -> log.debug("Total talks in database: {}", count))
                .doOnError(error -> log.error("Failed to count talks", error));
    }
}
