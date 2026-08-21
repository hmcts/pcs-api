package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyId;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the join between the view layer and Review Support Request. The service is exercised on the
 * case data the view actually produces, rather than on a hand-built collection, because the defect this
 * pins was that the service read a collection the view never populates with external party flags.
 */
@DisplayName("Review Support Request event start over view-mapped case data")
class ReviewSupportRequestFlowTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 8, 21, 9, 47, 4);

    private CaseFlagsView caseFlagsView;
    private SupportReviewService supportReviewService;

    private PartyEntity claimant;
    private PartyEntity defendant;

    @BeforeEach
    void setUp() {
        caseFlagsView = new CaseFlagsView();
        supportReviewService = new SupportReviewService();
        claimant = party("Possession Claims Solicitor Org", null, null);
        defendant = party(null, "testing", "CR TEST1");
    }

    @Test
    @DisplayName("offers a stored external Requested flag for review")
    void offersStoredExternalRequestedFlagForReview() {
        defendant.setDefendantFlags(new ArrayList<>(List.of(
            supportFlag("RA0024", "A different type of chair", "Requested", FlagVisibility.EXTERNAL))));

        PCSCase pcsCase = viewMappedCase();
        List<ListValue<PartySupport>> reviewFlags = supportReviewService.buildRequestedSupport(pcsCase);

        assertThat(reviewFlags).hasSize(1);

        ListValue<PartySupport> entry = reviewFlags.getFirst();
        assertThat(entry.getId()).isEqualTo(defendant.getId().toString());
        assertThat(entry.getValue().getSupportFlags().getGroupId()).isEqualTo(defendant.getId());
        assertThat(entry.getValue().getSupportFlags().getRoleOnCase()).isEqualTo("Defendant");
        assertThat(entry.getValue().getSupportFlags().getVisibility()).isEqualTo(FlagVisibility.EXTERNAL);

        List<ListValue<FlagDetail>> details = entry.getValue().getSupportFlags().getDetails();
        assertThat(details).hasSize(1);
        assertThat(details.getFirst().getId())
            .isEqualTo(defendant.getDefendantFlags().getFirst().getId().toString());
        assertThat(details.getFirst().getValue().getFlagCode()).isEqualTo("RA0024");
        assertThat(details.getFirst().getValue().getName()).isEqualTo("A different type of chair");
        assertThat(details.getFirst().getValue().getStatus()).isEqualTo("Requested");
    }

    @Test
    @DisplayName("offers requested support raised by the claimant as well as the defendant")
    void offersRequestedSupportForEveryPartyRole() {
        claimant.setDefendantFlags(new ArrayList<>(List.of(
            supportFlag("SM0004", "Evidence given in private", "Requested", FlagVisibility.EXTERNAL))));
        defendant.setDefendantFlags(new ArrayList<>(List.of(
            supportFlag("RA0029", "Therapy animal", "Requested", FlagVisibility.EXTERNAL))));

        List<ListValue<PartySupport>> reviewFlags =
            supportReviewService.buildRequestedSupport(viewMappedCase());

        assertThat(reviewFlags)
            .extracting(entry -> entry.getValue().getSupportFlags().getRoleOnCase())
            .containsExactlyInAnyOrder("Claimant", "Defendant");
    }

    @Test
    @DisplayName("excludes flags that are not awaiting review and internal flags")
    void excludesNonRequestedAndInternalFlags() {
        defendant.setDefendantFlags(new ArrayList<>(List.of(
            supportFlag("RA0022", "Accessible toilet", "Active", FlagVisibility.EXTERNAL),
            supportFlag("RA0021", "Parking space close to the venue", "Inactive", FlagVisibility.EXTERNAL),
            supportFlag("PF0015", "Language Interpreter", "Requested", FlagVisibility.INTERNAL))));

        assertThat(supportReviewService.buildRequestedSupport(viewMappedCase())).isEmpty();
    }

    @Test
    @DisplayName("returns nothing to review when no support is stored")
    void returnsNothingWhenNoSupportStored() {
        assertThat(supportReviewService.buildRequestedSupport(viewMappedCase())).isEmpty();
    }

    private PCSCase viewMappedCase() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        Set<PartyEntity> parties = new LinkedHashSet<>(List.of(claimant, defendant));
        pcsCaseEntity.setParties(parties);

        UUID claimId = UUID.randomUUID();
        ClaimPartyEntity claimantLink = claimParty(claimant, PartyRole.CLAIMANT, claimId);
        ClaimPartyEntity defendantLink = claimParty(defendant, PartyRole.DEFENDANT, claimId);
        pcsCaseEntity.setClaims(List.of(ClaimEntity.builder()
            .id(claimId)
            .claimParties(List.of(claimantLink, defendantLink))
            .build()));

        PCSCase pcsCase = PCSCase.builder().build();
        caseFlagsView.setCaseFields(pcsCase, pcsCaseEntity);
        return pcsCase;
    }

    private ClaimPartyEntity claimParty(PartyEntity partyEntity, PartyRole role, UUID claimId) {
        ClaimPartyId id = new ClaimPartyId();
        id.setPartyId(partyEntity.getId());
        id.setClaimId(claimId);

        return ClaimPartyEntity.builder().id(id).party(partyEntity).role(role).build();
    }

    private PartyEntity party(String orgName, String firstName, String lastName) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .orgName(orgName)
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }

    private CasePartyFlagEntity supportFlag(String flagCode, String flagName, String status,
                                            FlagVisibility visibility) {
        CasePartyFlagEntity flag = new CasePartyFlagEntity();
        flag.setId(UUID.randomUUID());
        flag.setFlagRefData(FlagRefDataEntity.builder().flagCode(flagCode).flagName(flagName).build());
        flag.setDefaultStatus(status);
        flag.setVisibility(visibility == FlagVisibility.EXTERNAL ? "External" : "Internal");
        flag.setDateTimeCreated(CREATED);
        flag.setPaths(UUID.randomUUID() + ":Party");
        return flag;
    }
}
