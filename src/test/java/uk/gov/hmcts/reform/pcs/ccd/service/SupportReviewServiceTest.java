package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SupportReviewServiceTest {

    private static final UUID PARTY_UUID = UUID.randomUUID();
    private static final String PARTY_ID = PARTY_UUID.toString();

    private SupportReviewService underTest;

    @BeforeEach
    void setUp() {
        underTest = new SupportReviewService();
    }

    @Test
    void shouldIncludeOnlyRequestedFlags() {
        PCSCase pcsCase = caseWithSupport(List.of(
            flag("Requested", "Sign Language Interpreter"),
            flag("Active", "Assistance dog"),
            flag("Inactive", "Regular breaks"),
            flag("Not approved", "Private waiting area")));

        List<ListValue<PartySupport>> result = underTest.buildRequestedSupport(pcsCase);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getValue().getSupportFlags().getDetails())
            .extracting(detail -> detail.getValue().getName())
            .containsExactly("Sign Language Interpreter");
    }

    @Test
    void shouldMatchStatusCaseInsensitively() {
        PCSCase pcsCase = caseWithSupport(List.of(flag("REQUESTED", "Lip speaker")));

        assertThat(underTest.buildRequestedSupport(pcsCase)).hasSize(1);
    }

    @Test
    void shouldOmitPartiesWithNoRequestedFlags() {
        PCSCase pcsCase = caseWithSupport(List.of(flag("Active", "Video hearing")));

        assertThat(underTest.buildRequestedSupport(pcsCase)).isEmpty();
    }

    @Test
    void shouldPreservePartyIdentityAndFlagGroupingForDisplay() {
        PCSCase pcsCase = caseWithSupport(List.of(flag("Requested", "Intermediary")));

        List<ListValue<PartySupport>> result = underTest.buildRequestedSupport(pcsCase);

        assertThat(result.getFirst().getId()).isEqualTo(PARTY_ID);
        Flags supportFlags = result.getFirst().getValue().getSupportFlags();
        assertThat(supportFlags.getPartyName()).isEqualTo("Test Party");
        assertThat(supportFlags.getRoleOnCase()).isEqualTo("Defendant");
        assertThat(supportFlags.getGroupId()).isEqualTo(PARTY_UUID);
        // The reviewed collection is internal-only case data rendered through the internal flag
        // launcher, so it is presented as internal whatever visibilities it was gathered from.
        assertThat(supportFlags.getVisibility()).isEqualTo(FlagVisibility.INTERNAL);
    }

    /**
     * Support a defendant requests through the citizen Your Support journey is stored as internal, and an
     * authorised internal reviewer must still be able to review it. Stored visibility decides who sees
     * support on the Support tab, not whether a requested flag can be reviewed.
     */
    @Test
    void shouldIncludeRequestedSupportStoredAsInternal() {
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(supportEntry(PARTY_ID, FlagVisibility.INTERNAL,
                List.of(flag("Requested", "Internally stored flag")))))
            .build();

        List<ListValue<PartySupport>> result = underTest.buildRequestedSupport(pcsCase);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getValue().getSupportFlags().getDetails())
            .extracting(detail -> detail.getValue().getName())
            .containsExactly("Internally stored flag");
    }

    @Test
    void shouldExcludeNonRequestedSupportWhateverItsVisibility() {
        String internalPartyId = UUID.randomUUID().toString();
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(
                supportEntry(internalPartyId, FlagVisibility.INTERNAL, List.of(
                    flag("Active", "Internal active"),
                    flag("Inactive", "Internal inactive"),
                    flag("Not approved", "Internal not approved"))),
                supportEntry(PARTY_ID, FlagVisibility.EXTERNAL, List.of(
                    flag("Active", "External active"),
                    flag("Inactive", "External inactive"),
                    flag("Not approved", "External not approved")))))
            .build();

        assertThat(underTest.buildRequestedSupport(pcsCase)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenThereIsNoSupportCollection() {
        assertThat(underTest.buildRequestedSupport(PCSCase.builder().build())).isEmpty();
    }

    @Test
    void shouldSkipNullSupportEntry() {
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(ListValue.<PartySupport>builder().id(PARTY_ID).value(null).build()))
            .build();

        assertThat(underTest.buildRequestedSupport(pcsCase)).isEmpty();
    }

    @Test
    void shouldSkipEntryWithNoSupportFlags() {
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(ListValue.<PartySupport>builder()
                .id(PARTY_ID)
                .value(PartySupport.builder().supportFlags(null).build())
                .build()))
            .build();

        assertThat(underTest.buildRequestedSupport(pcsCase)).isEmpty();
    }

    @Test
    void shouldSkipEntryWhenSupportFlagsCarryNoDetails() {
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(supportEntry(PARTY_ID, FlagVisibility.EXTERNAL, null)))
            .build();

        assertThat(underTest.buildRequestedSupport(pcsCase)).isEmpty();
    }

    @Test
    void shouldSkipFlagDetailsWithNoValue() {
        PCSCase pcsCase = caseWithSupport(new ArrayList<>(List.of(
            ListValue.<FlagDetail>builder().id(UUID.randomUUID().toString()).value(null).build(),
            flag("Requested", "Sign Language Interpreter"))));

        List<ListValue<PartySupport>> result = underTest.buildRequestedSupport(pcsCase);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getValue().getSupportFlags().getDetails())
            .extracting(detail -> detail.getValue().getName())
            .containsExactly("Sign Language Interpreter");
    }

    @Test
    void shouldReviewRequestedSupportForEveryPartyRoleNotOnlyDefendants() {
        String claimantId = UUID.randomUUID().toString();
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(
                supportEntry(claimantId, FlagVisibility.EXTERNAL,
                    List.of(flag("Requested", "Hearing loop")), "Claimant"),
                supportEntry(PARTY_ID, FlagVisibility.EXTERNAL, List.of(flag("Requested", "Therapy animal")))))
            .build();

        List<ListValue<PartySupport>> result = underTest.buildRequestedSupport(pcsCase);

        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(entry -> entry.getValue().getSupportFlags().getRoleOnCase())
            .containsExactly("Claimant", "Defendant");
    }

    @Test
    void shouldSkipPartiesWithoutRequestedSupportWhileKeepingTheRest() {
        String otherId = UUID.randomUUID().toString();
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(
                supportEntry(otherId, FlagVisibility.EXTERNAL, List.of(flag("Active", "Video hearing"))),
                supportEntry(PARTY_ID, FlagVisibility.EXTERNAL, List.of(flag("REQUESTED", "Lip speaker")))))
            .build();

        List<ListValue<PartySupport>> result = underTest.buildRequestedSupport(pcsCase);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(PARTY_ID);
    }

    private PCSCase caseWithSupport(List<ListValue<FlagDetail>> details) {
        return PCSCase.builder()
            .partySupport(List.of(supportEntry(PARTY_ID, FlagVisibility.EXTERNAL, details)))
            .build();
    }

    private ListValue<PartySupport> supportEntry(String partyId, FlagVisibility visibility,
                                                 List<ListValue<FlagDetail>> details) {
        return supportEntry(partyId, visibility, details, "Defendant");
    }

    private ListValue<PartySupport> supportEntry(String partyId, FlagVisibility visibility,
                                                 List<ListValue<FlagDetail>> details, String roleOnCase) {
        return ListValue.<PartySupport>builder()
            .id(partyId)
            .value(PartySupport.builder()
                .supportFlags(Flags.builder()
                    .partyName("Test Party")
                    .roleOnCase(roleOnCase)
                    .groupId(UUID.fromString(partyId))
                    .visibility(visibility)
                    .details(details)
                    .build())
                .build())
            .build();
    }

    private ListValue<FlagDetail> flag(String status, String name) {
        return ListValue.<FlagDetail>builder()
            .id(UUID.randomUUID().toString())
            .value(FlagDetail.builder().name(name).status(status).build())
            .build();
    }
}
