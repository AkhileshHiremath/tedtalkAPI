package org.tedtalk.api.services.interfaces;

import org.tedtalk.api.services.InfluenceSpeakerService.InfluentialSpeaker;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SpeakerInfluenceService {

    /**
     * Finds the most influential speakers based on engagement metrics.
     *
     * @param limit maximum number of speakers to return
     * @return Mono containing list of influential speakers
     */
    Mono<List<InfluentialSpeaker>> findMostInfluential(int limit);
}

