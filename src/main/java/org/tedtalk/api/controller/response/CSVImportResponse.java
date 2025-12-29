package org.tedtalk.api.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CSVImportResponse {
    private String message;
    private long recordsImported;
    private long recordsSkipped;
    private List<String> warnings;

    public CSVImportResponse(String message, long recordsImported) {
        this.message = message;
        this.recordsImported = recordsImported;
        this.recordsSkipped = 0;
        this.warnings = new ArrayList<>();
    }
}
