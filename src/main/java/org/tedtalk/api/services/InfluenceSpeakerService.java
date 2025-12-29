package org.tedtalk.api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tedtalk.api.model.TedTalk;
import org.tedtalk.api.repository.TedTalkRepository;
import org.tedtalk.api.services.interfaces.SpeakerInfluenceService;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for calculating speaker influence metrics.
 *
 * "Influence" here means: how much does this speaker engage audiences?
 * We measure it by total views + likes across all their talks, then average it.
 *
 * Why average? Because someone with 10 mediocre talks shouldn't rank higher than
 * someone with 2 blockbuster talks. Average engagement is fairer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InfluenceSpeakerService implements SpeakerInfluenceService {

    private final TedTalkRepository repository;

    @Override
    public Mono<List<InfluentialSpeaker>> findMostInfluential(int limit) {
        return findMostInfluentialSpeakers(limit);
    }

    /**
     * Calculate and rank the most influential speakers.
     *
     * Algorithm:
     * 1. Group all talks by speaker
     * 2. For each speaker, calculate total views + likes
     * 3. Divide by number of talks to get average engagement
     * 4. Sort by average (highest first)
     * 5. Return top N
     *
     * Java 21 Enhancement: Uses Sequenced Collections for better list operations
     */
    public Mono<List<InfluentialSpeaker>> findMostInfluentialSpeakers(int limit) {
        log.debug("Calculating top {} influential speakers", limit);

        // Handle invalid limit
        if (limit <= 0) {
            log.debug("Invalid limit {}, returning empty list", limit);
            return Mono.just(List.of());
        }

        return Mono.fromCallable(() -> {
            // Load all talks - yes, this loads everything into memory
            // For a real production system with millions of talks, we'd do this in the database
            List<TedTalk> allTalks = repository.findAll();
            log.debug("Analyzing {} talks to find influential speakers", allTalks.size());

            // Step 1: Group talks by speaker name
            Map<String, List<TedTalk>> talksByAuthor = groupTalksByAuthor(allTalks);

            // Step 2-4: Calculate metrics and sort
            // Java 21: Using toList() which returns an unmodifiable list (more efficient)
            List<InfluentialSpeaker> speakers = talksByAuthor.entrySet().stream()
                    .map(this::calculateInfluentialSpeaker)
                    .sorted(Comparator.comparingDouble(InfluentialSpeaker::averageEngagement).reversed())
                    .limit(limit)
                    .toList(); // Java 21: toList() instead of collect(Collectors.toList())

            log.debug("Found top {} influential speakers", speakers.size());
            return speakers;
        })
        .doOnError(error -> log.error("Failed to calculate influential speakers", error));
    }

    /**
     * Simple grouping by author name.
     * We could use Collectors.groupingBy inline, but extracting it makes the code clearer.
     */
    private Map<String, List<TedTalk>> groupTalksByAuthor(List<TedTalk> talks) {
        return talks.stream()
                .collect(Collectors.groupingBy(TedTalk::getAuthor));
    }

    /**
     * Calculate all metrics for a single speaker.
     * This is where the "influence" magic happens!
     */
    private InfluentialSpeaker calculateInfluentialSpeaker(Map.Entry<String, List<TedTalk>> entry) {
        String author = entry.getKey();
        List<TedTalk> talks = entry.getValue();

        int talkCount = talks.size();
        long totalViews = calculateTotalViews(talks);
        long totalLikes = calculateTotalLikes(talks);

        // Average engagement = total engagement divided by number of talks
        // This rewards consistency over quantity
        double averageEngagement = calculateAverageEngagement(totalViews, totalLikes, talkCount);

        return new InfluentialSpeaker(author, talkCount, totalViews, totalLikes, averageEngagement);
    }

    /**
     * Sum up all views across a speaker's talks.
     */
    private long calculateTotalViews(List<TedTalk> talks) {
        return talks.stream()
                .mapToLong(TedTalk::getViews)
                .sum();
    }

    /**
     * Sum up all likes across a speaker's talks.
     */
    private long calculateTotalLikes(List<TedTalk> talks) {
        return talks.stream()
                .mapToLong(TedTalk::getLikes)
                .sum();
    }

    /**
     * Calculate average engagement per talk.
     * Formula: (total views + total likes) / number of talks
     *
     * Edge case: If somehow a speaker has 0 talks, return 0 (shouldn't happen in practice).
     */
    private double calculateAverageEngagement(long totalViews, long totalLikes, int talkCount) {
        if (talkCount == 0) {
            log.warn("Found speaker with 0 talks - this shouldn't happen!");
            return 0.0;
        }
        return (double) (totalViews + totalLikes) / talkCount;
    }

    /**
     * Data class representing an influential speaker and their metrics.
     *
     * Using a record here (Java 17+) because:
     * - It's immutable by default (thread-safe)
     * - Automatically generates equals/hashCode/toString
     * - Makes the code more concise and readable
     */
    public record InfluentialSpeaker(
            String author,
            int talkCount,
            long totalViews,
            long totalLikes,
            double averageEngagement
    ) {
        // Records are perfect for DTOs like this - no boilerplate needed!
    }
}

