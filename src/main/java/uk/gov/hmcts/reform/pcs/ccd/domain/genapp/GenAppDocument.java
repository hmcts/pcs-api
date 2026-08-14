package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenAppDocument {

    private String id;

    private String filename;

    private String url;

    private String binaryUrl;

    private String categoryId;

    private LocalDateTime uploadTimestamp;
}
