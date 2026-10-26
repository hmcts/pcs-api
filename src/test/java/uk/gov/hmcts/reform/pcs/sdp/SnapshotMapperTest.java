package uk.gov.hmcts.reform.pcs.sdp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotGround;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotMilestones;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SnapshotMapperTest {

    @InjectMocks
    private SnapshotMapper claimSnapshotMapper;

    @Test
    void shouldMapSocialHousingDimensions() {
        SnapshotDimensions result = claimSnapshotMapper.mapDimensions(
            "PROVIDER_OF_SOCIAL_HOUSING",
            "NO",
            "WELSH",
            "SECURE_TENANCY",
            "123",
            "ENGLAND"
        );

        assertThat(result.getClaimType()).isEqualTo("AAA3-SLD");
        assertThat(result.getClaimSubType()).isEqualTo("Social");
        assertThat(result.getLanguage()).isEqualTo("cym");
        assertThat(result.getTenancyType()).isEqualTo("AAA3-STY");
        assertThat(result.getSubmittedType()).isEqualTo("Digital");
        assertThat(result.getCaseManagementLocationCode()).isEqualTo("MRD-CRT-0123");
    }

    @Test
    void shouldMapTrespassClaimType() {
        SnapshotDimensions result = claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD",
            "YES",
            "ENGLISH",
            "ASSURED_TENANCY",
            "45",
            "ENGLAND"
        );

        assertThat(result.getClaimType()).isEqualTo("AAA3-TCM");
        assertThat(result.getClaimSubType()).isEqualTo("Possession");
    }

    @Test
    void shouldMapWalesPrivateAndAssuredTenancy() {
        SnapshotDimensions result = claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD",
            "NO",
            "ENGLISH",
            "ASSURED_TENANCY",
            "45",
            "WALES"
        );

        assertThat(result.getClaimType()).isEqualTo("AAA3-WPL");
        assertThat(result.getClaimSubType()).isEqualTo("Private");
        assertThat(result.getTenancyType()).isEqualTo("AAA3-ATY");
    }

    @Test
    void shouldMapMortgageClaimType() {
        SnapshotDimensions result = claimSnapshotMapper.mapDimensions(
            "MORTGAGE_LENDER",
            "NO",
            "ENGLISH",
            "OTHER",
            "45",
            "ENGLAND"
        );

        assertThat(result.getClaimType()).isEqualTo("AAA3-MOR");
        assertThat(result.getClaimSubType()).isEqualTo("Mortgage");
        assertThat(result.getTenancyType()).isEqualTo("AAA3-OTH");
    }

    @Test
    void shouldDefaultLocationCodeWhenMissing() {
        SnapshotDimensions result = claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD",
            "NO",
            "ENGLISH",
            "ASSURED_TENANCY",
            null,
            "ENGLAND"
        );

        assertThat(result.getCaseManagementLocationCode()).isEqualTo("MRD-CRT-0000");
    }

    @Test
    void shouldMapMilestonesWithPostalNotice() {
        SnapshotMilestones result = claimSnapshotMapper.mapMilestones(
            "FIRST_CLASS_POST",
            "2026-01-10",
            "2026-02-01T14:30:00Z",
            "2026-02-05T09:00:00Z",
            "2026-02-03T10:00:00Z"
        );

        assertThat(result.getPostalClaimReceivedByCourtDate()).isEqualTo("2026-01-10");
        assertThat(result.getClaimSubmittedToSystemDate()).isEqualTo("2026-02-01T14:30:00Z");
        assertThat(result.getClaimFeePaidDate()).isEqualTo("2026-02-03T10:00:00Z");
        assertThat(result.getIssueClaimDate()).isEqualTo("2026-02-05T09:00:00Z");
    }

    @Test
    void shouldMapDefendantWithoutPartyType() {
        SnapshotParty result = claimSnapshotMapper.mapParty("party-1", "DEFENDANT", "PRIVATE_LANDLORD", false);

        assertThat(result.getPartyId()).isEqualTo("party-1");
        assertThat(result.getPartyRole()).isEqualTo("Defendant");
        assertThat(result.getPartyType()).isNull();
        assertThat(result.getPartyRepresented()).isEqualTo("No");
    }

    @Test
    void shouldMapClaimantPartyTypeAndRepresentation() {
        SnapshotParty result = claimSnapshotMapper.mapParty(
            "party-1", "CLAIMANT", "PROVIDER_OF_SOCIAL_HOUSING", true);

        assertThat(result.getPartyRole()).isEqualTo("Claimant");
        assertThat(result.getPartyType()).isEqualTo("AAA3-SHS");
        assertThat(result.getPartyRepresented()).isEqualTo("Yes");
    }

    @Test
    void shouldMapGroundFromStoredValues() {
        SnapshotGround result = claimSnapshotMapper.mapGround("GROUND_8", "Serious rent arrears", "ASSURED_MANDATORY");

        assertThat(result.getGroundsCode()).isEqualTo("GROUND_8");
        assertThat(result.getGroundsDescription()).isEqualTo("Serious rent arrears");
        assertThat(result.getGroundsSubType()).isEqualTo("Mandatory");
    }

    @Test
    void shouldMapCounterClaimResponse() {
        SnapshotResponse result = claimSnapshotMapper.mapResponse("12", "party-1", "YES", "2026-03-01T10:00:00Z");

        assertThat(result.getResponseId()).isEqualTo("12");
        assertThat(result.getDefendantId()).isEqualTo("party-1");
        assertThat(result.getResponseType()).isEqualTo("Counter Claim");
        assertThat(result.getRespondedDate()).isEqualTo("2026-03-01T10:00:00Z");
    }

    @Test
    void shouldAssembleFullSnapshotRecord() {
        SnapshotDimensions dimensions = claimSnapshotMapper.mapDimensions(
            "PROVIDER_OF_SOCIAL_HOUSING", "NO", "ENGLISH", "SECURE_TENANCY", "45", "ENGLAND"
        );
        SnapshotMilestones milestones = claimSnapshotMapper.mapMilestones(
            "EMAIL", null, "2026-03-01T10:00:00Z", "2026-03-02T11:00:00Z", null
        );
        SnapshotParty party = claimSnapshotMapper.mapParty("party-1", "CLAIMANT", "PROVIDER_OF_SOCIAL_HOUSING", false);

        ClaimSnapshotRecord result = claimSnapshotMapper.mapRecord(
            "aat",
            "2026-03-09",
            "snapshot-123",
            "2026-03-09T12:00:00Z",
            ClaimSnapshot.builder()
                .claimReference("PCS-1001")
                .snapshotVersion(2)
                .recordedDate("2026-03-01T10:00:00Z")
                .dimensions(dimensions)
                .milestones(milestones)
                .parties(List.of(party))
                .build()
        );

        assertThat(result.getContractVersion()).isEqualTo("4.1.0");
        assertThat(result.getProducer().getSystem()).isEqualTo("possessions-ccd");
        assertThat(result.getProducer().getEnvironment()).isEqualTo("test");
        assertThat(result.getExtractDate()).isEqualTo("2026-03-09");
        assertThat(result.getSnapshotId()).isEqualTo("snapshot-123");
        assertThat(result.getPublishedDate()).isEqualTo("2026-03-09T12:00:00Z");
        assertThat(result.getClaim().getClaimReference()).isEqualTo("PCS-1001");
        assertThat(result.getClaim().getSnapshotVersion()).isEqualTo(2);
        assertThat(result.getClaim().getDimensions()).isEqualTo(dimensions);
        assertThat(result.getClaim().getMilestones()).isEqualTo(milestones);
        assertThat(result.getClaim().getParties()).containsExactly(party);
    }
}
