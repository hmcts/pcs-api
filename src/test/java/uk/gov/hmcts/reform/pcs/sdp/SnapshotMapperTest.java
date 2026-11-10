package uk.gov.hmcts.reform.pcs.sdp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotFeeParty;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotGround;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotMilestones;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotPropertyAddress;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotRequestedFee;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

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
        assertThat(result.getLanguage()).isEqualTo("eng");
    }

    @Test
    void shouldMapEnglishAndWelshLanguage() {
        SnapshotDimensions result = claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD",
            "NO",
            "ENGLISH_AND_WELSH",
            "ASSURED_TENANCY",
            "45",
            "ENGLAND"
        );

        assertThat(result.getLanguage()).isEqualTo("engAndCym");
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
    void shouldMapCommunityLandlordClaimType() {
        SnapshotDimensions result = claimSnapshotMapper.mapDimensions(
            "COMMUNITY_LANDLORD",
            "NO",
            "ENGLISH",
            "SECURE_CONTRACT",
            "45",
            "WALES"
        );

        assertThat(result.getClaimType()).isEqualTo("AAA3-WSL");
        assertThat(result.getClaimSubType()).isEqualTo("Social");
        assertThat(result.getTenancyType()).isEqualTo("AAA3-SEC");
    }

    @Test
    void shouldMapRemainingTenancyTypes() {
        assertThat(claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD", "NO", "ENGLISH", "INTRODUCTORY_TENANCY", "45", "ENGLAND"
        ).getTenancyType()).isEqualTo("AAA3-ITY");
        assertThat(claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD", "NO", "ENGLISH", "FLEXIBLE_TENANCY", "45", "ENGLAND"
        ).getTenancyType()).isEqualTo("AAA3-FTY");
        assertThat(claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD", "NO", "ENGLISH", "DEMOTED_TENANCY", "45", "ENGLAND"
        ).getTenancyType()).isEqualTo("AAA3-DTY");
        assertThat(claimSnapshotMapper.mapDimensions(
            "PRIVATE_LANDLORD", "NO", "ENGLISH", "STANDARD_CONTRACT", "45", "WALES"
        ).getTenancyType()).isEqualTo("AAA3-STN");
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
    void shouldMapPropertyAddressWithoutPostcodeInAddressLine() {
        List<SnapshotPropertyAddress> result = claimSnapshotMapper.mapPropertyAddress(
            "1 High Street",
            "Flat 2",
            null,
            "Cambridge",
            "Cambridgeshire",
            "England",
            "cb5 8bl"
        );

        assertThat(result).singleElement().satisfies(address -> {
            assertThat(address.getAddress())
                .isEqualTo("1 High Street, Flat 2, Cambridge, Cambridgeshire, England");
            assertThat(address.getPostcode()).isEqualTo("CB5 8BL");
        });
    }

    @Test
    void shouldMapEmptyPropertyAddressWhenNoLinesOrPostcode() {
        assertThat(claimSnapshotMapper.mapPropertyAddress(
            null, " ", null, null, null, null, "  "
        )).isEmpty();
    }

    @Test
    void shouldMapPropertyAddressWhenPostcodeIsNull() {
        List<SnapshotPropertyAddress> result = claimSnapshotMapper.mapPropertyAddress(
            "1 High Street",
            null,
            null,
            "Cambridge",
            null,
            null,
            null
        );

        assertThat(result).singleElement().satisfies(address -> {
            assertThat(address.getAddress()).isEqualTo("1 High Street, Cambridge");
            assertThat(address.getPostcode()).isEmpty();
        });
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
        assertThat(result.getClaimIssuedToDefendantDate()).isEqualTo("2026-02-05T09:00:00Z");
    }

    @Test
    void shouldMapDefendantWithoutPartyType() {
        SnapshotParty result = claimSnapshotMapper.mapParty(
            "party-1", "DEFENDANT", "PRIVATE_LANDLORD", false, "ENGLISH");

        assertThat(result.getPartyId()).isEqualTo("party-1");
        assertThat(result.getPartyRole()).isEqualTo("Defendant");
        assertThat(result.getPartyType()).isNull();
        assertThat(result.getPartyRepresented()).isEqualTo("No");
        assertThat(result.getPartyLanguage()).isEqualTo("eng");
    }

    @Test
    void shouldMapClaimantPartyTypeAndRepresentation() {
        SnapshotParty result = claimSnapshotMapper.mapParty(
            "party-1", "CLAIMANT", "PROVIDER_OF_SOCIAL_HOUSING", true, "WELSH");

        assertThat(result.getPartyRole()).isEqualTo("Claimant");
        assertThat(result.getPartyType()).isEqualTo("AAA3-SHS");
        assertThat(result.getPartyRepresented()).isEqualTo("Yes");
        assertThat(result.getPartyLanguage()).isEqualTo("cym");
    }

    @Test
    void shouldMapRemainingClaimantPartyTypesAndOtherRole() {
        assertThat(claimSnapshotMapper.mapParty("p", "CLAIMANT", "MORTGAGE_LENDER", false, "ENGLISH").getPartyType())
            .isEqualTo("AAA3-MPL");
        assertThat(claimSnapshotMapper.mapParty("p", "CLAIMANT", "COMMUNITY_LANDLORD", false, "ENGLISH").getPartyType())
            .isEqualTo("AAA3-RCL");
        assertThat(claimSnapshotMapper.mapParty("p", "CLAIMANT", "OTHER", false, "ENGLISH").getPartyType())
            .isEqualTo("AAA3-OTH");
        assertThat(claimSnapshotMapper.mapParty("p", "CLAIMANT", "PRIVATE_LANDLORD", false, "ENGLISH").getPartyType())
            .isEqualTo("AAA3-PLD");
        assertThat(claimSnapshotMapper.mapParty("p", "UNDERLESSEE_OR_MORTGAGEE", "PRIVATE_LANDLORD", false, "ENGLISH")
            .getPartyRole()).isEqualTo("UNDERLESSEE_OR_MORTGAGEE");
    }

    @Test
    void shouldMapGroundFromStoredValues() {
        SnapshotGround result = claimSnapshotMapper.mapGround("GROUND_8", "Serious rent arrears", "ASSURED_MANDATORY");

        assertThat(result.getGroundsCode()).isEqualTo("GROUND_8");
        assertThat(result.getGroundsDescription()).isEqualTo("Serious rent arrears");
        assertThat(result.getGroundsSubType()).isEqualTo("Mandatory");
    }

    @Test
    void shouldMapDiscretionaryAndUnmappedGrounds() {
        assertThat(claimSnapshotMapper.mapGround("10", "Rent arrears", "ASSURED_DISCRETIONARY")
            .getGroundsSubType()).isEqualTo("Discretionary");
        assertThat(claimSnapshotMapper.mapGround("OTHER", "Other", "ASSURED_OTHER")
            .getGroundsSubType()).isEqualTo("ASSURED_OTHER");
        assertThat(claimSnapshotMapper.mapGround("OTHER", "Other", null).getGroundsSubType()).isNull();
    }

    @Test
    void shouldMapRequestedFeeFromPayment() {
        UUID partyId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        SdpClaimSnapshotPayment payment = new SdpClaimSnapshotPayment(
            UUID.randomUUID(),
            10,
            partyId,
            "2026-123456",
            "RC-1234",
            new BigDecimal("355.00"),
            "PAID",
            "CLAIM",
            null
        );

        SnapshotRequestedFee result = claimSnapshotMapper.mapRequestedFee(payment, "2026-02-03T10:00:00Z");

        assertThat(result.getFeeId()).isEqualTo(
            UUID.nameUUIDFromBytes("fee-payment-10".getBytes(StandardCharsets.UTF_8)).toString());
        assertThat(result.getFeeReference()).isEqualTo("2026-123456");
        assertThat(result.getFeeTypeId()).isEqualTo(
            UUID.nameUUIDFromBytes("fee-type-CLAIM".getBytes(StandardCharsets.UTF_8)).toString());
        assertThat(result.getFeeTypeDescription()).isEqualTo("CLAIM");
        assertThat(result.getFeeAmount()).isEqualByComparingTo("355.00");
        assertThat(result.getFeeStatus()).isEqualTo("Fully Paid");
        assertThat(result.getFeeRequestedOfTheseParties()).singleElement()
            .extracting(SnapshotFeeParty::getPartyId).isEqualTo(partyId.toString());
        assertThat(result.getFeeRequestedDate()).isEqualTo("2026-02-03T10:00:00Z");
        assertThat(result.getResponsiblePartyId()).isEqualTo(partyId.toString());
    }

    @Test
    void shouldMapFeeStatusAndFallbackReference() {
        SdpClaimSnapshotPayment unpaid = new SdpClaimSnapshotPayment(
            UUID.randomUUID(), 11, null, " ", "RC-9", null, "NOT_PAID", "CLAIM", null);
        assertThat(claimSnapshotMapper.mapRequestedFee(unpaid, null).getFeeStatus())
            .isEqualTo("Requested Not Paid");
        assertThat(claimSnapshotMapper.mapRequestedFee(unpaid, null).getFeeReference())
            .isEqualTo("RC-9");
        assertThat(claimSnapshotMapper.mapRequestedFee(unpaid, null).getFeeRequestedOfTheseParties())
            .isEmpty();

        SdpClaimSnapshotPayment partial = new SdpClaimSnapshotPayment(
            UUID.randomUUID(), 12, null, null, null, null, "PARTIALLY_PAID", "CLAIM", null);
        assertThat(claimSnapshotMapper.mapRequestedFee(partial, null).getFeeStatus())
            .isEqualTo("Partially Paid");
        assertThat(claimSnapshotMapper.mapRequestedFee(partial, null).getFeeReference()).isNull();
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
    void shouldMapDefenceResponse() {
        assertThat(claimSnapshotMapper.mapResponse("12", "party-1", "NO", null).getResponseType())
            .isEqualTo("Defence");
    }

    @Test
    void shouldMapEnvironments() {
        ClaimSnapshot claim = ClaimSnapshot.builder().claimReference("PCS-1001").build();

        assertThat(claimSnapshotMapper.mapRecord("prod", "2026-03-09", "s", "2026-03-09T12:00:00Z", claim)
            .getProducer().getEnvironment()).isEqualTo("prod");
        assertThat(claimSnapshotMapper.mapRecord("preview", "2026-03-09", "s", "2026-03-09T12:00:00Z", claim)
            .getProducer().getEnvironment()).isEqualTo("dev");
        assertThat(claimSnapshotMapper.mapRecord("demo", "2026-03-09", "s", "2026-03-09T12:00:00Z", claim)
            .getProducer().getEnvironment()).isEqualTo("demo");
    }

    @Test
    void shouldAssembleFullSnapshotRecord() {
        SnapshotDimensions dimensions = claimSnapshotMapper.mapDimensions(
            "PROVIDER_OF_SOCIAL_HOUSING", "NO", "ENGLISH", "SECURE_TENANCY", "45", "ENGLAND"
        );
        SnapshotMilestones milestones = claimSnapshotMapper.mapMilestones(
            "EMAIL", null, "2026-03-01T10:00:00Z", "2026-03-02T11:00:00Z", null
        );
        SnapshotParty party = claimSnapshotMapper.mapParty(
            "party-1", "CLAIMANT", "PROVIDER_OF_SOCIAL_HOUSING", false, "ENGLISH");

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

        assertThat(result.getContractVersion()).isEqualTo("4.2.0");
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
