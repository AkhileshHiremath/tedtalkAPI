package org.tedtalk.api.controller.dtos;

import java.time.LocalDateTime;

public record TedTalkDataVO(
        Long id,
        String title,
        String author,
        LocalDateTime date,
        Long views,
        Long likes,
        String link
) {
    // Factory method to create DTO from entity
    public static TedTalkDataVO fromEntity(org.tedtalk.api.model.TedTalk entity) {
        return new TedTalkDataVO(
                entity.getId(),
                entity.getTitle(),
                entity.getAuthor(),
                entity.getDate(),
                entity.getViews(),
                entity.getLikes(),
                entity.getLink()
        );
    }
}
