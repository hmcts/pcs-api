package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.awaitility.Awaitility;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CaseDocumentService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void waitForDocumentType(long caseReference, DocumentType documentType) {
        Awaitility.await("Document type %s exists for case".formatted(documentType))
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofMillis(1000))
            .ignoreExceptions()
            .until(() -> caseDocumentExists(caseReference, documentType));
    }

    private boolean caseDocumentExists(long caseReference, DocumentType documentType) {

        SqlParameterSource namedParameters = new MapSqlParameterSource()
            .addValue("caseReference", caseReference)
            .addValue("documentType", documentType.name());

        return jdbcTemplate.queryForObject(
            """
                SELECT EXISTS(
                    SELECT 1 FROM document d
                    JOIN pcs_case pcs ON pcs.id = d.case_id
                    WHERE pcs.case_reference = :caseReference
                    AND d.type = :documentType
                )
                """,
            namedParameters,
            Boolean.class
        );
    }

}
