package uk.gov.hmcts.reform.pcs;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PendingDisposal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.rse.ccd.lib.Database;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DraftRetentionPolicyTest extends CftlibTest {

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private IdamClient idamClient;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    @Qualifier("retainAndDisposeTask")
    private Runnable retainAndDisposeTask;

    private String solicitorToken;
    private String systemUserToken;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        systemUserToken = idamClient.getAccessToken("pcs-system-user@localhost", "password");
    }

    @Test
    void deletesDraftsAfterMoreThanThirtyDaysOfInactivity() throws Exception {
        long inactiveDraft = createDraft();
        long thirtyDayOldDraft = createDraft();

        setLastModified(inactiveDraft, 31);
        setLastModified(thirtyDayOldDraft, 30);

        retainAndDisposeTask.run();

        assertThat(ccdClient.getCaseDetails(inactiveDraft, systemUserToken).getState())
            .isEqualTo(PendingDisposal.name());
        assertThat(ccdClient.getCaseDetails(thirtyDayOldDraft, solicitorToken).getState())
            .isEqualTo(AWAITING_SUBMISSION_TO_HMCTS.name());
        assertThat(localCaseValue(
            "select resolved_ttl = current_date from ccd.case_data where reference = :reference",
            inactiveDraft,
            Boolean.class
        )).isTrue();
        assertThat(localCaseValue(
            "select resolved_ttl is null from ccd.case_data where reference = :reference",
            thirtyDayOldDraft,
            Boolean.class
        )).isTrue();

        expireLocalTtl(inactiveDraft);
        deleteCentralCasePointer(inactiveDraft);

        retainAndDisposeTask.run();

        assertThat(localCaseCount("ccd.case_data", "reference", inactiveDraft)).isZero();
        assertThat(localCaseCount("pcs_case", "case_reference", inactiveDraft)).isZero();
    }

    private long createDraft() {
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                .addressLine1("123 Baker Street")
                .postTown("London")
                .postCode("NW1 6XE")
                .build())
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        CaseDetails caseDetails = ccdClient.createCase(caseData, solicitorToken);
        assertThat(caseDetails.getState()).isEqualTo(AWAITING_SUBMISSION_TO_HMCTS.name());
        return caseDetails.getId();
    }

    private void setLastModified(long caseReference, int daysAgo) {
        int updated = jdbc.update(
            """
                update ccd.case_data
                set last_modified = current_date - :daysAgo
                where reference = :reference
                """,
            Map.of("reference", caseReference, "daysAgo", daysAgo)
        );
        assertThat(updated).isOne();
    }

    private void expireLocalTtl(long caseReference) {
        int updated = jdbc.update(
            "update ccd.case_data set resolved_ttl = current_date - 1 where reference = :reference",
            Map.of("reference", caseReference)
        );
        assertThat(updated).isOne();
    }

    private void deleteCentralCasePointer(long caseReference) throws Exception {
        try (Connection connection = cftlib().getConnection(Database.Datastore);
             PreparedStatement statement = connection.prepareStatement("delete from case_data where reference = ?")) {
            statement.setLong(1, caseReference);
            assertThat(statement.executeUpdate()).isOne();
        }
    }

    private int localCaseCount(String table, String referenceColumn, long caseReference) {
        return localCaseValue(
            "select count(*) from " + table + " where " + referenceColumn + " = :reference",
            caseReference,
            Integer.class
        );
    }

    private <T> T localCaseValue(String sql, long caseReference, Class<T> resultType) {
        return jdbc.queryForObject(sql, Map.of("reference", caseReference), resultType);
    }
}
