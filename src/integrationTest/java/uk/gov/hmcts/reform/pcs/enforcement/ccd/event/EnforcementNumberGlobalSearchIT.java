package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Global search indexes a decentralised case from the case data the SDK records with each event, which it takes
 * from the case view straight after the submit handler. So the event that approves a warrant has to be the one
 * whose recorded data carries the new number, or the warrant cannot be found by it (HDPI-7677).
 */
@PcsCcdEventTest
@DisplayName("Warrant numbers reach the case data global search indexes")
class EnforcementNumberGlobalSearchIT extends AbstractPostgresContainerIT {

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    /**
     * A case view looks up the viewer's case roles to filter what defendants see (DefendantEnforcementView); the
     * SDK test support does not answer it, so the mock gives the viewer no case roles.
     */
    @MockitoBean
    private CaseAssignmentApi caseAssignmentApi;

    private long caseReference;
    private Actor caseworker;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        caseworker = events.registerActor("Hana", "Admin", "caseworker-pcs", "hearing-centre-admin");
    }

    @Test
    @DisplayName("Decide enforcement records each approved warrant's number as an other case reference")
    void approvingAWarrantIndexesItsNumber() {
        givenApplicationAwaitingReview();
        approve();
        String firstNumber = numbersOnTheClaim().getFirst();
        assertThat(otherCaseReferencesInLatestEventData()).containsExactly(firstNumber);

        closeEveryEnforcement();
        givenApplicationAwaitingReview();
        approve();

        List<String> numbers = numbersOnTheClaim();
        assertThat(numbers).hasSize(2).contains(firstNumber);
        assertThat(otherCaseReferencesInLatestEventData()).containsExactlyInAnyOrderElementsOf(numbers);
    }

    private void approve() {
        events.event(caseReference, EventId.decideEnforcement.name(), PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                                      .enforcementDecision(EnforcementDecision.ACCEPT)
                                      .build())
                .build())
            .as(caseworker)
            .submitExpectingSuccess();
    }

    private List<String> otherCaseReferencesInLatestEventData() {
        String data = jdbcTemplate.queryForObject(
            "SELECT ce.data::text FROM ccd.case_event ce JOIN ccd.case_data cd ON cd.id = ce.case_data_id "
                + "WHERE cd.reference = ? ORDER BY ce.id DESC LIMIT 1",
            String.class, caseReference);
        JsonNode references;
        try {
            references = objectMapper.readTree(data).path("SearchCriteria").path("OtherCaseReferences");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        List<String> values = new ArrayList<>();
        references.forEach(reference -> values.add(reference.path("value").asText()));
        return values;
    }

    private void givenApplicationAwaitingReview() {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            EnforcementOrderEntity warrant = new EnforcementOrderEntity();
            warrant.setClaim(claim());
            warrant.setEnforcementOrder(EnforcementOrder.builder()
                .selectEnforcementType(SelectEnforcementType.WARRANT)
                .build());
            warrant.setStatus(EnforcementStatus.AWAITING_REVIEW);
            warrant.setStatusUpdatedAt(Instant.now());
            enforcementOrderRepository.saveAndFlush(warrant);
        });
    }

    private void closeEveryEnforcement() {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored ->
            enforcementOrderRepository.findByClaimId(claim().getId()).forEach(enforcement -> {
                enforcement.setStatus(EnforcementStatus.CLOSED);
                enforcementOrderRepository.saveAndFlush(enforcement);
            }));
    }

    private List<String> numbersOnTheClaim() {
        return new TransactionTemplate(transactionManager).execute(ignored ->
            enforcementOrderRepository.findEnforcementNumbersByCaseReference(caseReference));
    }

    private ClaimEntity claim() {
        return pcsCaseRepository.findByCaseReference(caseReference).orElseThrow().getClaims().getFirst();
    }
}
