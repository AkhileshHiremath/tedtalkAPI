package org.tedtalk.api.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TedTalkTest {

    @Test
    void testTedTalkConstructor() {
        LocalDateTime date = LocalDateTime.now();
        TedTalk tedTalk = new TedTalk(1L, "Test Title", "Test Author", date, 1000L, 100L, "http://test.com");

        assertEquals(1L, tedTalk.getId());
        assertEquals("Test Title", tedTalk.getTitle());
        assertEquals("Test Author", tedTalk.getAuthor());
        assertEquals(date, tedTalk.getDate());
        assertEquals(1000L, tedTalk.getViews());
        assertEquals(100L, tedTalk.getLikes());
        assertEquals("http://test.com", tedTalk.getLink());
    }

    @Test
    void testTedTalkEqualsAndHashCode() {
        LocalDateTime date = LocalDateTime.now();
        TedTalk tedTalk1 = new TedTalk(1L, "Test Title", "Test Author", date, 1000L, 100L, "http://test.com");
        TedTalk tedTalk2 = new TedTalk(1L, "Test Title", "Test Author", date, 1000L, 100L, "http://test.com");
        TedTalk tedTalk3 = new TedTalk(2L, "Different Title", "Test Author", date, 1000L, 100L, "http://test.com");

        assertEquals(tedTalk1, tedTalk2);
        assertNotEquals(tedTalk1, tedTalk3);
        assertEquals(tedTalk1.hashCode(), tedTalk2.hashCode());
        assertNotEquals(tedTalk1.hashCode(), tedTalk3.hashCode());
    }

    @Test
    void testTedTalkToString() {
        LocalDateTime date = LocalDateTime.of(2023, 1, 1, 10, 0);
        TedTalk tedTalk = new TedTalk(1L, "Test Title", "Test Author", date, 1000L, 100L, "http://test.com");

        String expected = "TedTalk(id=1, title=Test Title, author=Test Author, date=2023-01-01T10:00, views=1000, likes=100, link=http://test.com)";
        assertEquals(expected, tedTalk.toString());
    }
}
