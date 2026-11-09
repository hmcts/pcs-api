package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.awaitility.Awaitility;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;

@Service
@RequiredArgsConstructor
public class CaseDocumentService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void waitForDocumentsOfType(long caseReference,
                                       DocumentType documentType,
                                       Instant createdSince,
                                       int minCount) {

        Awaitility.await("At least %d documents of type %s created since %s for case %d"
                             .formatted(minCount, documentType, createdSince, caseReference))
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofMillis(1000))
            .ignoreExceptions()
            .until(() -> countCaseDocuments(caseReference, documentType, createdSince) >= minCount);
    }

    /**
     * The dm-store ids of a case's documents of one type, once there are {@code count} of them.
     */
    public List<UUID> waitForDocumentIds(long caseReference, DocumentType documentType, int count) {
        return Awaitility.await("%d documents of type %s exist for case".formatted(count, documentType))
            .atMost(Duration.ofSeconds(180))
            .pollInterval(Duration.ofMillis(1000))
            .until(() -> documentIds(caseReference, documentType), hasSize(count));
    }

    private List<UUID> documentIds(long caseReference, DocumentType documentType) {
        SqlParameterSource namedParameters = new MapSqlParameterSource()
            .addValue("caseReference", caseReference)
            .addValue("documentType", documentType.name());

        return jdbcTemplate.queryForList(
            """
                SELECT d.document_id FROM document d
                JOIN pcs_case pcs ON pcs.id = d.case_id
                WHERE pcs.case_reference = :caseReference
                AND d.type = :documentType
                AND d.removed = false
                """,
            namedParameters,
            UUID.class
        );
    }

    private int countCaseDocuments(long caseReference, DocumentType documentType, Instant createdSince) {

        SqlParameterSource namedParameters = new MapSqlParameterSource()
            .addValue("caseReference", caseReference)
            .addValue("documentType", documentType.name())
            .addValue("createdSince", Timestamp.from(createdSince));

        return jdbcTemplate.queryForObject(
            """
                    SELECT count(*) FROM document d
                    JOIN pcs_case pcs ON pcs.id = d.case_id
                    WHERE pcs.case_reference = :caseReference
                    AND d.type = :documentType
                    AND d.submitted_date > :createdSince
                """,
            namedParameters,
            Integer.class
        );
    }

}
