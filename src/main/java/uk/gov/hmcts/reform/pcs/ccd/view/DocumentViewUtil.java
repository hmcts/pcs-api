package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentViewUtil {

    /**
     * The upload timestamp shown in Case File View. As a decentralised service PCS owns this value,
     * so it is set on every Document we return to CCD rather than being populated by ccd-data-store.
     */
    public static LocalDateTime uploadTimestamp(DocumentEntity documentEntity) {
        if (documentEntity.getSubmittedDate() == null) {
            return null;
        }

        return documentEntity.getSubmittedDate().atZone(ZoneOffset.UTC).toLocalDateTime();
    }
}
