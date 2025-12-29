package org.tedtalk.api.controller.dtos;

/**
 * Data class representing an influential speaker and their metrics.
 */
public record InfluentialSpeaker(
        String author,
        int talkCount,
        long totalViews,
        long totalLikes,
        double averageEngagement
) {}
