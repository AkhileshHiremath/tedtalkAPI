package org.tedtalk.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class representing a TedTalk.
 */
@Entity
@Table(name = "ted_talk")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TedTalk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private Long views;

    @Column(nullable = false)
    private Long likes;

    @Column(nullable = false, length = 500)
    private String link;

}