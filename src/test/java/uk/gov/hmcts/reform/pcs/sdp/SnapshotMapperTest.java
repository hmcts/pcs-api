package uk.gov.hmcts.reform.pcs.sdp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotMilestones;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;

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
            "123"
        );

        assertThat(result.getClaimType()).isEqualTo("AAA3-SLD");
        assertThat(result.getClaimSubType()).isEqualTo("Social_housing");
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
            "45"
        );

        assertThat(result.getClaimType()).isEqualTo("AAA3-TCM");
    }

    @Test
    void shouldMapDefaultClaimType() {
        SnapshotDimensions result = claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD",
            "NO",
            "ENGLISH",
            "ASSURED_TENANCY",
            "45"
        );

        assertThat(result.getClaimType()).isEqualTo("AAA3-PLD");
    }

    @Test
    void shouldDefaultLocationCodeWhenMissing() {
        SnapshotDimensions result = claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD",
            "NO",
            "ENGLISH",
            "ASSURED_TENANCY",
            null
        );

        assertThat(result.getCaseManagementLocationCode()).isEqualTo("MRD-CRT-0000");
    }

    @Test
    void shouldMapMilestonesWithPostalNotice() {
        SnapshotMilestones result = claimSnapshotMapper.mapMilestones(
            "FIRST_CLASS_POST",
            "2026-01-10",
            "2026-02-01 14:30:00",
            "2026-02-05 09:00:00"
        );

        assertThat(result.getPostalClaimReceivedByCourtDate()).isEqualTo("2026-01-10");
        assertThat(result.getClaimSubmittedToSystemDate()).isEqualTo("2026-02-01 14:30:00");
        assertThat(result.getClaimFeePaidDate()).isEqualTo("2026-02-01 14:30:00");
        assertThat(result.getIssueClaimDate()).isEqualTo("2026-02-05 09:00:00");
    }

    @Test
    void shouldMapDefendantPartyRole() {
        SnapshotParty result = claimSnapshotMapper.mapParty("party-1", "DEFENDANT");

        assertThat(result.getPartyId()).isEqualTo("party-1");
        assertThat(result.getPartyRole()).isEqualTo("Defendant");
    }

    @Test
    void shouldAssembleFullSnapshotRecord() {
        SnapshotDimensions dimensions = claimSnapshotMapper.mapDimensions(
            "PROVIDER_OF_SOCIAL_HOUSING", "NO", "ENGLISH", "SECURE_TENANCY", "45"
        );
        SnapshotMilestones milestones = claimSnapshotMapper.mapMilestones(
            "EMAIL", null, "2026-03-01 10:00:00", "2026-03-02 11:00:00"
        );
        SnapshotParty party = claimSnapshotMapper.mapParty("party-1", "CLAIMANT");

        ClaimSnapshotRecord result = claimSnapshotMapper.mapRecord(
            "aat",
            "2026-03-09",
            "snapshot-123",
            "2026-03-09T12:00:00Z",
            ClaimSnapshot.builder()
                .claimReference("PCS-1001")
                .snapshotVersion(2)
                .recordedDate("2026-03-01 10:00:00")
                .dimensions(dimensions)
                .milestones(milestones)
                .parties(List.of(party))
                .build()
        );

        assertThat(result.getContractVersion()).isEqualTo("4.1.0");
        assertThat(result.getProducer().getSystem()).isEqualTo("possessions-ccd");
        assertThat(result.getProducer().getEnvironment()).isEqualTo("aat");
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
