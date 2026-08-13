package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SupportReviewServiceTest {

    private static final String PARTY_ID = UUID.randomUUID().toString();

    private SupportReviewService underTest;

    @BeforeEach
    void setUp() {
        underTest = new SupportReviewService();
    }

    @Test
    void shouldIncludeOnlyRequestedFlags() {
        PCSCase pcsCase = caseWithExternalFlags(List.of(
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
        PCSCase pcsCase = caseWithExternalFlags(List.of(flag("REQUESTED", "Lip speaker")));

        assertThat(underTest.buildRequestedSupport(pcsCase)).hasSize(1);
    }

    @Test
    void shouldOmitPartiesWithNoRequestedFlags() {
        PCSCase pcsCase = caseWithExternalFlags(List.of(flag("Active", "Video hearing")));

        assertThat(underTest.buildRequestedSupport(pcsCase)).isEmpty();
    }

    @Test
    void shouldPreservePartyIdentityAndFlagGroupingForDisplay() {
        PCSCase pcsCase = caseWithExternalFlags(List.of(flag("Requested", "Intermediary")));

        List<ListValue<PartySupport>> result = underTest.buildRequestedSupport(pcsCase);

        assertThat(result.getFirst().getId()).isEqualTo(PARTY_ID);
        Flags supportFlags = result.getFirst().getValue().getSupportFlags();
        assertThat(supportFlags.getPartyName()).isEqualTo("Test Party");
        assertThat(supportFlags.getRoleOnCase()).isEqualTo("Defendant");
    }

    @Test
    void shouldNotIncludeInternalPartyFlags() {
        Party party = Party.builder()
            .id(PARTY_ID)
            .defendantFlags(Flags.builder()
                .details(List.of(flag("Requested", "Internal only flag")))
                .build())
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(ListValue.<Party>builder().id(PARTY_ID).value(party).build()))
            .build();

        assertThat(underTest.buildRequestedSupport(pcsCase)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenThereAreNoParties() {
        assertThat(underTest.buildRequestedSupport(PCSCase.builder().build())).isEmpty();
    }

    @Test
    void shouldSkipNullPartyEntry() {
        PCSCase pcsCase = PCSCase.builder().allDefendants(
            List.of(ListValue.<Party>builder().id(PARTY_ID).value(null).build()))
            .build();

        assertThat(underTest.buildRequestedSupport(pcsCase)).isEmpty();
    }

    @Test
    void shouldSkipNullPartyEntryWhenWithMultipleParties() {
        // Given
        Party party = Party.builder().id(PARTY_ID).defendantFlags(Flags.builder()
            .details(List.of(flag("Requested", "Internal only flag"))).build()).build();
        Party withExternalFlags = Party.builder().id(UUID.randomUUID().toString())
            .partyFlagsExternal(Flags.builder().partyName("Test Party").roleOnCase("Defendant")
                                    .details(List.of(flag("REQUESTED", "Lip speaker"))).build()).build();

        PCSCase pcsCase = PCSCase.builder().allDefendants(
                List.of(ListValue.<Party>builder().id(PARTY_ID).value(null).value(party)
                            .value(withExternalFlags).build())).build();

        // When
        List<ListValue<PartySupport>> listValues = underTest.buildRequestedSupport(pcsCase);

        // Then
        assertThat(listValues).hasSize(1);
    }

    private PCSCase caseWithExternalFlags(List<ListValue<FlagDetail>> details) {
        Party party = Party.builder()
            .id(PARTY_ID)
            .partyFlagsExternal(Flags.builder()
                .partyName("Test Party")
                .roleOnCase("Defendant")
                .details(details)
                .build())
            .build();

        return PCSCase.builder()
            .allDefendants(List.of(ListValue.<Party>builder().id(PARTY_ID).value(party).build()))
            .build();
    }

    private ListValue<FlagDetail> flag(String status, String name) {
        return ListValue.<FlagDetail>builder()
            .id(UUID.randomUUID().toString())
            .value(FlagDetail.builder().name(name).status(status).build())
            .build();
    }
}
