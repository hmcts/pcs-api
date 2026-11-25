package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

public final class CitizenPossessionClaimFixtures {

    public static final String COMPLETE_CLAIM_JSON = "/citizenclaim/complete-citizen-possession-claim.json";

    private CitizenPossessionClaimFixtures() {
    }

    public static ObjectMapper draftObjectMapper() {
        return new JacksonConfiguration().draftCaseDataObjectMapper();
    }

    public static CitizenPossessionClaim completeClaim() {
        try (InputStream json = CitizenPossessionClaimFixtures.class.getResourceAsStream(COMPLETE_CLAIM_JSON)) {
            return draftObjectMapper().readValue(json, PCSCase.class).getCitizenPossessionClaim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The complete claim for a property in Wales: the occupation contract, the Wales grounds (section 157 rent
     * arrears, section 160 estate management, section 199 and Others) and the required documents.
     */
    public static CitizenPossessionClaim completeWalesClaim() {
        CitizenPossessionClaim claim = completeClaim();
        claim.getEligibility().setLegislativeCountry(LegislativeCountry.WALES);

        CitizenClaimTenancy tenancy = claim.getTenancy();
        tenancy.setTenancyType(null);
        tenancy.setOccupationContractType(OccupationLicenceTypeWales.STANDARD_CONTRACT);
        tenancy.setHasEnergyPerformanceCertificate(VerticalYesNo.YES);
        tenancy.setEnergyPerformanceDocuments(List.of(document("epc.pdf")));
        tenancy.setHasGasSafetyReport(VerticalYesNo.NO);
        tenancy.setNoGasSafetyReportReason("The property has no gas supply");
        tenancy.setHasElectricalInstallationConditionReport(VerticalYesNo.YES);
        tenancy.setElectricalInstallationDocuments(List.of(document("eicr.pdf")));

        claim.setGrounds(CitizenClaimGrounds.builder()
            .hasRentArrearsGrounds(VerticalYesNo.YES)
            .walesDiscretionaryGrounds(Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157,
                                              DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160))
            .walesMandatoryGrounds(Set.of(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199))
            .otherGrounds(Set.of(PrivateLandlordAssuredGround.OTHER))
            .otherGroundsDetails("Another reason")
            .walesGroundReasons(List.of(
                walesReason(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160, "Building works planned"),
                walesReason(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199, "Break clause used")))
            .build());
        return claim;
    }

    private static ListValue<Document> document(String filename) {
        return ListValue.<Document>builder()
            .value(Document.builder()
                       .url("http://dm-store/documents/" + filename)
                       .binaryUrl("http://dm-store/documents/" + filename + "/binary")
                       .filename(filename)
                       .build())
            .build();
    }

    private static ListValue<CitizenClaimWalesGroundReason> walesReason(Enum<?> ground, String reason) {
        return ListValue.<CitizenClaimWalesGroundReason>builder()
            .value(CitizenClaimWalesGroundReason.builder().ground(ground.name()).reason(reason).build())
            .build();
    }

}
