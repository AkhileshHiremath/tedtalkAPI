package org.tedtalk.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tedtalk.api.model.TedTalk;
import org.tedtalk.api.repository.TedTalkRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for TedTalkDataServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class TedTalkDataServiceImplTest {

    @Mock
    private TedTalkRepository tedTalkRepository;

    @InjectMocks
    private TedTalkDataServiceImpl TedTalkDataServiceImpl;

    @Test
    void findAll_WithMultipleTalks_ShouldReturnAllTalks() {
        
        TedTalk talk1 = new TedTalk(1L, "Title 1", "Author 1", LocalDateTime.now(), 1000L, 100L, "link1");
        TedTalk talk2 = new TedTalk(2L, "Title 2", "Author 2", LocalDateTime.now(), 2000L, 200L, "link2");
        List<TedTalk> talks = Arrays.asList(talk1, talk2);
        when(tedTalkRepository.findAll()).thenReturn(talks);

        
        StepVerifier.create(TedTalkDataServiceImpl.retreiveAllTedTalkData())
                .expectNextMatches(result -> result.size() == 2)
                .verifyComplete();

        verify(tedTalkRepository).findAll();
    }

    @Test
    void findAll_WithEmptyList_ShouldReturnEmptyList() {
        
        when(tedTalkRepository.findAll()).thenReturn(Collections.emptyList());

        
        StepVerifier.create(TedTalkDataServiceImpl.retreiveAllTedTalkData())
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

        verify(tedTalkRepository).findAll();
    }

    @Test
    void findAll_WithError_ShouldPropagateError() {
        
        when(tedTalkRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        
        StepVerifier.create(TedTalkDataServiceImpl.retreiveAllTedTalkData())
                .expectError(RuntimeException.class)
                .verify();

        verify(tedTalkRepository).findAll();
    }

    @Test
    void findById_WithExistingId_ShouldReturnTalk() {
        
        TedTalk talk = new TedTalk(1L, "Title", "Author", LocalDateTime.now(), 1000L, 100L, "link");
        when(tedTalkRepository.findById(1L)).thenReturn(Optional.of(talk));

        
        StepVerifier.create(TedTalkDataServiceImpl.findById(1L))
                .expectNextMatches(Optional::isPresent)
                .verifyComplete();

        verify(tedTalkRepository).findById(1L);
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        
        when(tedTalkRepository.findById(999L)).thenReturn(Optional.empty());

        
        StepVerifier.create(TedTalkDataServiceImpl.findById(999L))
                .expectNextMatches(Optional::isEmpty)
                .verifyComplete();

        verify(tedTalkRepository).findById(999L);
    }

    @Test
    void findById_WithError_ShouldPropagateError() {
        
        when(tedTalkRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        
        StepVerifier.create(TedTalkDataServiceImpl.findById(1L))
                .expectError(RuntimeException.class)
                .verify();

        verify(tedTalkRepository).findById(1L);
    }

    @Test
    void findByYear_WithExistingYear_ShouldReturnTalks() {
        
        TedTalk talk = new TedTalk(1L, "Title", "Author", LocalDateTime.of(2023, 1, 1, 0, 0), 1000L, 100L, "link");
        when(tedTalkRepository.findByDateYear(2023)).thenReturn(List.of(talk));

        
        StepVerifier.create(TedTalkDataServiceImpl.fetchTedtalkByYear(2023))
                .expectNextMatches(result -> result.size() == 1)
                .verifyComplete();

        verify(tedTalkRepository).findByDateYear(2023);
    }

    @Test
    void findByYear_WithNonExistingYear_ShouldReturnEmptyList() {
        
        when(tedTalkRepository.findByDateYear(1999)).thenReturn(Collections.emptyList());

        
        StepVerifier.create(TedTalkDataServiceImpl.fetchTedtalkByYear(1999))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

        verify(tedTalkRepository).findByDateYear(1999);
    }

    @Test
    void findByYear_WithError_ShouldPropagateError() {
        
        when(tedTalkRepository.findByDateYear(2023)).thenThrow(new RuntimeException("Database error"));

        
        StepVerifier.create(TedTalkDataServiceImpl.fetchTedtalkByYear(2023))
                .expectError(RuntimeException.class)
                .verify();

        verify(tedTalkRepository).findByDateYear(2023);
    }

    @Test
    void count_WithTalks_ShouldReturnCount() {
        
        when(tedTalkRepository.count()).thenReturn(42L);

        
        StepVerifier.create(TedTalkDataServiceImpl.fetchTotalTedTalkCount())
                .expectNext(42L)
                .verifyComplete();

        verify(tedTalkRepository).count();
    }

    @Test
    void count_WithZeroTalks_ShouldReturnZero() {
        
        when(tedTalkRepository.count()).thenReturn(0L);

        
        StepVerifier.create(TedTalkDataServiceImpl.fetchTotalTedTalkCount())
                .expectNext(0L)
                .verifyComplete();

        verify(tedTalkRepository).count();
    }

    @Test
    void count_WithError_ShouldPropagateError() {
        
        when(tedTalkRepository.count()).thenThrow(new RuntimeException("Database error"));

        
        StepVerifier.create(TedTalkDataServiceImpl.fetchTotalTedTalkCount())
                .expectError(RuntimeException.class)
                .verify();

        verify(tedTalkRepository).count();
    }
}

