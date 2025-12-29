package org.tedtalk.api.repository;
import org.tedtalk.api.model.TedTalk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TedTalkRepository extends JpaRepository<TedTalk, Long> {

    List<TedTalk> findByAuthor(String author);

    @Query("SELECT t FROM TedTalk t WHERE YEAR(t.date) = :year")
    List<TedTalk> findByDateYear(int year);
}
