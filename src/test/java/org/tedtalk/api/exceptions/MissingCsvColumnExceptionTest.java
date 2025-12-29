package org.tedtalk.api.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MissingCsvColumnExceptionTest {

    @Test
    void testMissingCsvColumnExceptionWithColumn() {
        String column = "title";
        MissingCsvColumnException exception = new MissingCsvColumnException(column);

        assertEquals("Missing required CSV column: title", exception.getMessage());
    }
}
