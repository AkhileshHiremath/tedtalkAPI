package org.tedtalk.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tedtalk.api.model.TedTalk;
import org.tedtalk.api.repository.TedTalkRepository;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfluenceSpeakerServiceTest {

    @Mock
    private TedTalkRepository repository;

    @InjectMocks
    private InfluenceSpeakerService service;

    @Test
    void findMostInfluentialSpeakers_WithMultipleTalks_ShouldReturnSortedList() {
        
        TedTalk talk1 = new TedTalk(1L, "Talk1", "AuthorA", LocalDateTime.now(), 1000L, 100L, "link1");
        TedTalk talk2 = new TedTalk(2L, "Talk2", "AuthorA", LocalDateTime.now(), 2000L, 200L, "link2");
        TedTalk talk3 = new TedTalk(3L, "Talk3", "AuthorB", LocalDateTime.now(), 1500L, 150L, "link3");
        when(repository.findAll()).thenReturn(List.of(talk1, talk2, talk3));

        
        StepVerifier.create(service.findMostInfluentialSpeakers(2))
                .expectNextMatches(speakers -> {
                    // AuthorA: talkCount=2, totalViews=3000, totalLikes=300, avg=(3000+300)/2=1650
                    // AuthorB: talkCount=1, totalViews=1500, totalLikes=150, avg=(1500+150)/1=1650
                    // But since avg is same, order might vary, but let's check size and content
                    return speakers.size() == 2 &&
                            speakers.stream().anyMatch(s -> s.author().equals("AuthorA") && s.talkCount() == 2 && s.totalViews() == 3000L && s.totalLikes() == 300L && s.averageEngagement() == 1650.0) &&
                            speakers.stream().anyMatch(s -> s.author().equals("AuthorB") && s.talkCount() == 1 && s.totalViews() == 1500L && s.totalLikes() == 150L && s.averageEngagement() == 1650.0);
                })
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithLimitGreaterThanAuthors_ShouldReturnAll() {
        
        TedTalk talk1 = new TedTalk(1L, "Talk1", "AuthorA", LocalDateTime.now(), 1000L, 100L, "link1");
        TedTalk talk2 = new TedTalk(2L, "Talk2", "AuthorB", LocalDateTime.now(), 2000L, 200L, "link2");
        when(repository.findAll()).thenReturn(List.of(talk1, talk2));

        
        StepVerifier.create(service.findMostInfluentialSpeakers(5))
                .expectNextMatches(speakers -> speakers.size() == 2)
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithNoTalks_ShouldReturnEmptyList() {

        when(repository.findAll()).thenReturn(List.of());

        StepVerifier.create(service.findMostInfluentialSpeakers(5))
                .expectNext(List.of())
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithLimitZero_ShouldReturnEmptyList() {
        // Given - No stubbing needed as service returns empty list immediately for limit 0

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(0))
                .expectNext(List.of())
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithSingleTalk_ShouldReturnOneSpeaker() {
        
        TedTalk talk1 = new TedTalk(1L, "Talk1", "AuthorA", LocalDateTime.now(), 1000L, 100L, "link1");
        when(repository.findAll()).thenReturn(List.of(talk1));

        
        StepVerifier.create(service.findMostInfluentialSpeakers(5))
                .expectNextMatches(speakers -> {
                    InfluenceSpeakerService.InfluentialSpeaker speaker = speakers.get(0);
                    return speakers.size() == 1 &&
                            speaker.author().equals("AuthorA") &&
                            speaker.talkCount() == 1 &&
                            speaker.totalViews() == 1000L &&
                            speaker.totalLikes() == 100L &&
                            speaker.averageEngagement() == 1100.0;
                })
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithNegativeLimit_ShouldReturnEmptyList() {
        // Given - No stubbing needed as service returns empty list immediately for negative limit

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(-1))
                .expectNext(List.of())
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithDifferentEngagementScores_ShouldSortCorrectly() {
        // Given
        TedTalk talk1 = new TedTalk(1L, "Talk1", "LowEngagement", LocalDateTime.now(), 100L, 10L, "link1");
        TedTalk talk2 = new TedTalk(2L, "Talk2", "HighEngagement", LocalDateTime.now(), 10000L, 1000L, "link2");
        TedTalk talk3 = new TedTalk(3L, "Talk3", "MediumEngagement", LocalDateTime.now(), 500L, 50L, "link3");
        when(repository.findAll()).thenReturn(List.of(talk1, talk2, talk3));

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(3))
                .expectNextMatches(speakers -> {
                    // HighEngagement: 11000, MediumEngagement: 550, LowEngagement: 110
                    return speakers.size() == 3 &&
                            speakers.get(0).author().equals("HighEngagement") &&
                            speakers.get(0).averageEngagement() == 11000.0 &&
                            speakers.get(1).author().equals("MediumEngagement") &&
                            speakers.get(1).averageEngagement() == 550.0 &&
                            speakers.get(2).author().equals("LowEngagement") &&
                            speakers.get(2).averageEngagement() == 110.0;
                })
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithSameAuthorMultipleTalks_ShouldAggregate() {
        // Given
        TedTalk talk1 = new TedTalk(1L, "Talk1", "Author", LocalDateTime.now(), 1000L, 100L, "link1");
        TedTalk talk2 = new TedTalk(2L, "Talk2", "Author", LocalDateTime.now(), 2000L, 200L, "link2");
        TedTalk talk3 = new TedTalk(3L, "Talk3", "Author", LocalDateTime.now(), 3000L, 300L, "link3");
        when(repository.findAll()).thenReturn(List.of(talk1, talk2, talk3));

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(1))
                .expectNextMatches(speakers -> {
                    InfluenceSpeakerService.InfluentialSpeaker speaker = speakers.get(0);
                    // Total views: 6000, Total likes: 600, Talk count: 3
                    // Average: (6000 + 600) / 3 = 2200
                    return speakers.size() == 1 &&
                            speaker.author().equals("Author") &&
                            speaker.talkCount() == 3 &&
                            speaker.totalViews() == 6000L &&
                            speaker.totalLikes() == 600L &&
                            speaker.averageEngagement() == 2200.0;
                })
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithZeroViewsAndLikes_ShouldCalculateCorrectly() {
        // Given
        TedTalk talk1 = new TedTalk(1L, "Talk1", "Author", LocalDateTime.now(), 0L, 0L, "link1");
        when(repository.findAll()).thenReturn(List.of(talk1));

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(1))
                .expectNextMatches(speakers -> {
                    InfluenceSpeakerService.InfluentialSpeaker speaker = speakers.get(0);
                    return speakers.size() == 1 &&
                            speaker.author().equals("Author") &&
                            speaker.talkCount() == 1 &&
                            speaker.totalViews() == 0L &&
                            speaker.totalLikes() == 0L &&
                            speaker.averageEngagement() == 0.0;
                })
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithVeryLargeNumbers_ShouldCalculateCorrectly() {
        // Given
        TedTalk talk1 = new TedTalk(1L, "Talk1", "ViralAuthor", LocalDateTime.now(), 1000000000L, 50000000L, "link1");
        when(repository.findAll()).thenReturn(List.of(talk1));

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(1))
                .expectNextMatches(speakers -> {
                    InfluenceSpeakerService.InfluentialSpeaker speaker = speakers.get(0);
                    return speakers.size() == 1 &&
                            speaker.author().equals("ViralAuthor") &&
                            speaker.totalViews() == 1000000000L &&
                            speaker.totalLikes() == 50000000L &&
                            speaker.averageEngagement() == 1050000000.0;
                })
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithManyAuthors_ShouldReturnOnlyRequestedLimit() {
        // Given
        List<TedTalk> talks = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            talks.add(new TedTalk((long) i, "Talk" + i, "Author" + i, LocalDateTime.now(),
                                 i * 1000L, i * 100L, "link" + i));
        }
        when(repository.findAll()).thenReturn(talks);

        // When & Then - Request only top 5
        StepVerifier.create(service.findMostInfluentialSpeakers(5))
                .expectNextMatches(speakers -> speakers.size() == 5)
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithOneAuthorManyTalks_ShouldAggregateCorrectly() {
        // Given - One author with 5 talks
        List<TedTalk> talks = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            talks.add(new TedTalk((long) i, "Talk" + i, "ProlificAuthor", LocalDateTime.now(),
                                 1000L, 100L, "link" + i));
        }
        when(repository.findAll()).thenReturn(talks);

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(1))
                .expectNextMatches(speakers -> {
                    InfluenceSpeakerService.InfluentialSpeaker speaker = speakers.get(0);
                    return speakers.size() == 1 &&
                            speaker.author().equals("ProlificAuthor") &&
                            speaker.talkCount() == 5 &&
                            speaker.totalViews() == 5000L &&
                            speaker.totalLikes() == 500L &&
                            speaker.averageEngagement() == 1100.0; // (5000 + 500) / 5
                })
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithTiedScores_ShouldReturnAll() {
        // Given - Multiple authors with same score
        TedTalk talk1 = new TedTalk(1L, "Talk1", "Author1", LocalDateTime.now(), 1000L, 100L, "link1");
        TedTalk talk2 = new TedTalk(2L, "Talk2", "Author2", LocalDateTime.now(), 1000L, 100L, "link2");
        TedTalk talk3 = new TedTalk(3L, "Talk3", "Author3", LocalDateTime.now(), 1000L, 100L, "link3");
        when(repository.findAll()).thenReturn(List.of(talk1, talk2, talk3));

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(3))
                .expectNextMatches(speakers -> {
                    // All should have same score
                    return speakers.size() == 3 &&
                            speakers.stream().allMatch(s -> s.averageEngagement() == 1100.0);
                })
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithLimitOne_ShouldReturnTopAuthor() {
        // Given
        TedTalk talk1 = new TedTalk(1L, "Talk1", "LowEngagement", LocalDateTime.now(), 100L, 10L, "link1");
        TedTalk talk2 = new TedTalk(2L, "Talk2", "HighEngagement", LocalDateTime.now(), 10000L, 1000L, "link2");
        when(repository.findAll()).thenReturn(List.of(talk1, talk2));

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(1))
                .expectNextMatches(speakers -> {
                    return speakers.size() == 1 &&
                            speakers.get(0).author().equals("HighEngagement");
                })
                .verifyComplete();
    }

    @Test
    void findMostInfluentialSpeakers_WithRepositoryError_ShouldPropagateError() {
        // Given
        when(repository.findAll()).thenThrow(new RuntimeException("Database connection lost"));

        // When & Then
        StepVerifier.create(service.findMostInfluentialSpeakers(5))
                .expectError(RuntimeException.class)
                .verify();
    }
}

