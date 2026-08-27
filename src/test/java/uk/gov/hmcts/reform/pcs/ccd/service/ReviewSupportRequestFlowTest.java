package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
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
import uk.gov.hmcts.reform.pcs.config.MapperConfig;

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

    private final ModelMapper modelMapper = new MapperConfig().modelMapper();

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
        assertThat(entry.getValue().getSupportFlags().getVisibility()).isEqualTo(FlagVisibility.INTERNAL);

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

    /**
     * The shape the citizen Your Support journey leaves on a real case: requested adjustments stored as
     * internal, alongside other support already made active. Only the requested rows are reviewable.
     */
    @Test
    @DisplayName("offers requested support stored as internal, as the citizen journey persists it")
    void offersStoredInternalRequestedFlagForReview() {
        defendant.setDefendantFlags(new ArrayList<>(List.of(
            supportFlag("RA0034", "Cool room", "Requested", FlagVisibility.INTERNAL),
            supportFlag("RA0042", "Sign language interpreter", "Requested", FlagVisibility.INTERNAL),
            supportFlag("RA0022", "Accessible toilet", "Active", FlagVisibility.INTERNAL))));

        List<ListValue<PartySupport>> reviewFlags =
            supportReviewService.buildRequestedSupport(viewMappedCase());

        assertThat(reviewFlags).hasSize(1);

        ListValue<PartySupport> entry = reviewFlags.getFirst();
        assertThat(entry.getId()).isEqualTo(defendant.getId().toString());
        assertThat(entry.getValue().getSupportFlags().getGroupId()).isEqualTo(defendant.getId());
        assertThat(entry.getValue().getSupportFlags().getRoleOnCase()).isEqualTo("Defendant");
        assertThat(entry.getValue().getSupportFlags().getDetails())
            .extracting(detail -> detail.getValue().getFlagCode())
            .containsExactlyInAnyOrder("RA0034", "RA0042");
    }

    @Test
    @DisplayName("offers internal and external requested support for one party together, without repeats")
    void offersRequestedSupportUnderBothVisibilitiesForTheSameParty() {
        defendant.setDefendantFlags(new ArrayList<>(List.of(
            supportFlag("RA0034", "Cool room", "Requested", FlagVisibility.INTERNAL),
            supportFlag("RA0024", "A different type of chair", "Requested", FlagVisibility.EXTERNAL))));

        List<ListValue<PartySupport>> reviewFlags =
            supportReviewService.buildRequestedSupport(viewMappedCase());

        assertThat(reviewFlags).hasSize(1);

        List<ListValue<FlagDetail>> details = reviewFlags.getFirst().getValue().getSupportFlags().getDetails();
        assertThat(details)
            .extracting(detail -> detail.getValue().getFlagCode())
            .containsExactlyInAnyOrder("RA0034", "RA0024");
        assertThat(details).extracting(ListValue::getId).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("excludes flags that are not awaiting review, whatever their visibility")
    void excludesNonRequestedFlagsUnderEitherVisibility() {
        defendant.setDefendantFlags(new ArrayList<>(List.of(
            supportFlag("RA0022", "Accessible toilet", "Active", FlagVisibility.EXTERNAL),
            supportFlag("RA0028", "Reader", "Active", FlagVisibility.INTERNAL),
            supportFlag("RA0021", "Parking space close to the venue", "Inactive", FlagVisibility.EXTERNAL),
            supportFlag("RA0030", "Break in proceedings", "Inactive", FlagVisibility.INTERNAL),
            supportFlag("PF0015", "Language Interpreter", "Not approved", FlagVisibility.EXTERNAL),
            supportFlag("RA0036", "Larger font", "Not approved", FlagVisibility.INTERNAL))));

        assertThat(supportReviewService.buildRequestedSupport(viewMappedCase())).isEmpty();
    }

    @Test
    @DisplayName("returns nothing to review when no support is stored")
    void returnsNothingWhenNoSupportStored() {
        assertThat(supportReviewService.buildRequestedSupport(viewMappedCase())).isEmpty();
    }

    /**
     * Pins the step the internal review depends on: the projected party has to carry its identity, or the
     * flag view cannot match it back to its entity and leaves the party flag collections empty, which is
     * what made internally stored support unreachable.
     */
    @Test
    @DisplayName("projects party identity so the view populates the party flag collections")
    void projectsPartyIdentityOntoTheProjectedParties() {
        defendant.setDefendantFlags(new ArrayList<>(List.of(
            supportFlag("RA0034", "Cool room", "Requested", FlagVisibility.INTERNAL))));

        PCSCase pcsCase = viewMappedCase();

        ListValue<Party> defendantValue = pcsCase.getParties().stream()
            .filter(value -> defendant.getId().toString().equals(value.getId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("projected parties carry no party identity"));

        assertThat(defendantValue.getValue().getId()).isEqualTo(defendant.getId().toString());
        assertThat(defendantValue.getValue().getDefendantFlags().getDetails())
            .extracting(detail -> detail.getValue().getFlagCode())
            .containsExactly("RA0034");
    }

    /**
     * Builds the case data the way {@code PCSCaseView} does: the party collection is projected through the
     * configured {@code ModelMapper} first, so party identity reaches the projected parties, and the flag
     * view then populates the party flag collections against it.
     */
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
        pcsCase.setParties(mapAndWrapParties(parties));
        caseFlagsView.setCaseFields(pcsCase, pcsCaseEntity);
        return pcsCase;
    }

    private List<ListValue<Party>> mapAndWrapParties(Set<PartyEntity> partyEntities) {
        return partyEntities.stream()
            .map(entity -> modelMapper.map(entity, Party.class))
            .map(party -> ListValue.<Party>builder().id(party.getId()).value(party).build())
            .toList();
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
