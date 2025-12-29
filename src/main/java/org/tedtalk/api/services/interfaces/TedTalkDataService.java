package org.tedtalk.api.services.interfaces;

import org.tedtalk.api.model.TedTalk;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface TedTalkDataService {

    /**
     * Retrieves all TED talks.
     *
     * @return Mono containing list of all TED talks
     */
    Mono<List<TedTalk>> retreiveAllTedTalkData();

    /**
     * Retrieves a TED talk by its ID.
     *
     * @param id the talk ID
     * @return Mono containing Optional of TedTalk
     */
    Mono<Optional<TedTalk>> findById(Long id);

    /**
     * Retrieves TED talks by year.
     *
     * @param year the year to filter by
     * @return Mono containing list of TED talks
     */
    Mono<List<TedTalk>> fetchTedtalkByYear(int year);

    /**
     * Gets the total count of TED talks.
     *
     * @return Mono containing the count
     */
    Mono<Long> fetchTotalTedTalkCount();
}

