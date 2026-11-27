package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.SubmittedDraft;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.submitDraftOrder;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * A solicitor files the order a party they represent proposes. The organisation a solicitor belongs to comes from
 * Professional Reference Data, which the test answers; which parties it represents is pcs-api's own record.
 */
@DisplayName("Submit a draft order")
@PcsCcdEventTest
class SubmitDraftOrderIT extends AbstractPostgresContainerIT {

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private OrganisationRepository organisations;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoSpyBean
    private OrganisationService organisationService;
    @MockitoSpyBean
    private FeatureToggleService featureToggleService;

    private long caseReference;
    private String organisationId;
    private Actor solicitor;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        organisationId = "ORG-" + caseReference;
        solicitor = events.registerActor("Sam", "Solicitor", "caseworker-pcs-solicitor");
        doReturn(organisationId).when(organisationService).getOrganisationIdForCurrentUser();
    }

    @Test
    @DisplayName("files the wording for the party the solicitor represents, cleaned of anything but its formatting")
    void filesTheWordingForTheRepresentedParty() {
        UUID claimant = represent(IssuedCases.CLAIMANT).get(IssuedCases.CLAIMANT);

        var accepted = submit("<ol><li>The defendant must pay <strong>£100</strong>.</li>"
            + "<li>Costs reserved.<script>alert(1)</script></li></ol>", null).submitExpectingSuccess();

        assertThat(accepted.confirmationBody()).contains("Draft order submitted");
        assertThat(filings()).containsExactly(Map.of(
            "party_id", claimant,
            "wording", "<ol><li>The defendant must pay <strong>£100</strong>.</li><li>Costs reserved.</li></ol>",
            "submitted_by", solicitor.uid()));
    }

    @Test
    @DisplayName("gives a judge making an order every party's draft, newest first")
    void givesAJudgeTheDraftsNewestFirst() {
        Map<String, UUID> parties = represent(IssuedCases.CLAIMANT, IssuedCases.DEFENDANT);
        submit("<p>1. Possession in 14 days.</p>", parties.get(IssuedCases.CLAIMANT)).submitExpectingSuccess();
        submit("<p>1. The claim is adjourned.</p>", parties.get(IssuedCases.DEFENDANT)).submitExpectingSuccess();
        Actor judge = events.registerActor("A", "Judge", "caseworker-pcs");

        List<SubmittedDraft> drafts = events.external(caseReference, MAKE_ORDER).as(judge).start()
            .caseContext().drafts();

        assertThat(drafts).extracting(SubmittedDraft::party, SubmittedDraft::wording).containsExactly(
            tuple(IssuedCases.DEFENDANT + " (Defendant 1)", "<p>1. The claim is adjourned.</p>"),
            tuple(IssuedCases.CLAIMANT + " (Claimant 1)", "<p>1. Possession in 14 days.</p>"));
    }

    @Test
    @DisplayName("refuses wording that is empty once cleaned")
    void refusesEmptyWording() {
        represent(IssuedCases.CLAIMANT);

        var rejected = submit("<p>&nbsp;</p><script>alert(1)</script>", null).submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(SubmitDraftOrder.NO_WORDING);
        assertThat(filings()).isEmpty();
    }

    @Test
    @DisplayName("refuses a solicitor whose organisation represents no party on the case")
    void refusesAnUnrepresentedParty() {
        var rejected = submit("<p>1. Possession.</p>", null).submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(SubmitDraftOrder.NOT_REPRESENTED);
        assertThat(filings()).isEmpty();
    }

    @Test
    @DisplayName("asks a solicitor representing several parties which one proposes the order, and files it for them")
    void filesForTheChosenParty() {
        Map<UUID, String> parties = invert(represent(IssuedCases.CLAIMANT, IssuedCases.DEFENDANT));

        PCSCase started = events.start(caseReference, submitDraftOrder.name()).as(solicitor).start().caseData();

        assertThat(started.getMultipleRepresentedParties()).isEqualTo(VerticalYesNo.YES);
        assertThat(started.getRepresentedPartyNames().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactlyInAnyOrderElementsOf(parties.values());

        UUID defendant = invert(parties).get(IssuedCases.DEFENDANT);
        submit("<p>1. Adjourned.</p>", defendant).submitExpectingSuccess();

        assertThat(filings()).extracting(row -> row.get("party_id")).containsExactly(defendant);
    }

    @Test
    @DisplayName("refuses a choice of a party the solicitor represents on another case")
    void refusesAPartyOnAnotherCase() {
        represent(IssuedCases.CLAIMANT, IssuedCases.DEFENDANT);
        long thisCase = caseReference;
        caseReference = cases.issue();
        UUID otherCasesDefendant = represent(IssuedCases.DEFENDANT).get(IssuedCases.DEFENDANT);
        caseReference = thisCase;

        var rejected = submit("<p>1. Adjourned.</p>", otherCasesDefendant).submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(SubmitDraftOrder.NOT_REPRESENTED);
        assertThat(filings()).isEmpty();
    }

    @Test
    @DisplayName("refuses a party that has been removed from the case")
    void refusesARemovedParty() {
        UUID defendant = represent(IssuedCases.DEFENDANT).get(IssuedCases.DEFENDANT);
        cases.update(caseReference, pcsCase -> pcsCase.getParties().stream()
            .filter(party -> party.getId().equals(defendant))
            .forEach(party -> party.setRemoved(true)));

        var rejected = submit("<p>1. Adjourned.</p>", defendant).submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(SubmitDraftOrder.NOT_REPRESENTED);
        assertThat(filings()).isEmpty();
    }

    @Test
    @DisplayName("does not start for a solicitor whose organisation represents no party on the case")
    void doesNotStartWithoutAPartyRepresented() {
        assertThatThrownBy(() -> events.start(caseReference, submitDraftOrder.name()).as(solicitor).start())
            .hasMessageContaining(SubmitDraftOrder.NOT_REPRESENTED);
    }

    @Test
    @DisplayName("refuses a filing while making orders is off")
    void refusesWhileMakingOrdersIsOff() {
        represent(IssuedCases.CLAIMANT);
        doReturn(false).when(featureToggleService).isEnabled(FeatureFlag.MAKE_ORDER);

        var rejected = submit("<p>1. Possession.</p>", null).submitExpectingErrors();

        assertThat(rejected.errors()).containsExactly(SubmitDraftOrder.NOT_AVAILABLE);
        assertThat(filings()).isEmpty();
    }

    private CcdEventTestSupport<PCSCase, State>.EventSubmission submit(String wording, UUID chosenParty) {
        PCSCase.PCSCaseBuilder data = PCSCase.builder().proposedOrderWording(wording);
        if (chosenParty != null) {
            data.representedPartyNames(DynamicList.builder()
                .value(DynamicListElement.builder().code(chosenParty).build())
                .listItems(List.of())
                .build());
        }
        return events.event(caseReference, submitDraftOrder.name(), data.build()).as(solicitor);
    }

    /** Makes the solicitor's organisation act for the case's parties of these names, giving each one's id by name. */
    private Map<String, UUID> represent(String... names) {
        Map<String, UUID> ids = new HashMap<>();
        cases.update(caseReference, pcsCase -> {
            OrganisationEntity organisation = organisations.findByOrganisationId(organisationId)
                .orElseGet(() -> OrganisationEntity.builder()
                    .organisationId(organisationId)
                    .organisationName("Solicitors LLP")
                    .organisationProfileId("SOLICITOR_PROFILE")
                    .createdDate(LocalDateTime.now())
                    .lastModifiedDate(LocalDateTime.now())
                    .build());
            for (String name : names) {
                PartyEntity party = pcsCase.getParties().stream()
                    .filter(candidate -> name.equals(candidate.getOrgName())
                        || name.equals(candidate.getFirstName() + " " + candidate.getLastName()))
                    .findFirst()
                    .orElseThrow();
                // A claimant's organisation is the party's own; a defendant's solicitors are linked to it.
                if (IssuedCases.CLAIMANT.equals(name)) {
                    party.setOrganisationId(organisationId);
                } else {
                    organisation.addParty(party);
                }
                ids.put(name, party.getId());
            }
            organisations.save(organisation);
        });
        return ids;
    }

    private static <K, V> Map<V, K> invert(Map<K, V> map) {
        Map<V, K> inverted = new HashMap<>();
        map.forEach((key, value) -> inverted.put(value, key));
        return inverted;
    }

    private List<Map<String, Object>> filings() {
        return jdbc.queryForList("""
            SELECT d.party_id, d.wording, d.submitted_by
            FROM submitted_draft_order d
            JOIN party p ON p.id = d.party_id
            JOIN pcs_case c ON c.id = p.case_id
            WHERE c.case_reference = ?
            """, caseReference);
    }
}
