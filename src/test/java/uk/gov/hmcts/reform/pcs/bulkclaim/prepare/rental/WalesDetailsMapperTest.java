package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ConductAnswerType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.DocumentProvidedType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ProhibitedConductType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.WalesType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static org.assertj.core.api.Assertions.assertThat;

class WalesDetailsMapperTest {

    private static final String PATH = "ClaimParticulars/Wales";
    private static final String ABSOLUTE = "/RentalClaimSet/RentalClaim[1]/" + PATH;

    private final WalesDetailsMapper mapper = new WalesDetailsMapper();
    private final BulkErrorCollector errors = BulkErrorCollector.forClaimPath("/RentalClaimSet/RentalClaim[1]");
    private final PCSCase target = PCSCase.builder().build();

    private static ProhibitedConductType conduct(boolean claim, Boolean agreed, String terms, String details) {
        ProhibitedConductType conduct = new ProhibitedConductType();
        conduct.setClaim(claim);
        conduct.setAgreedTermsOfPeriodicContract(agreed);
        conduct.setDetailsOfTerms(terms);
        conduct.setDetails(details);
        return conduct;
    }

    private static ConductAnswerType answer(boolean answer, String details) {
        ConductAnswerType type = new ConductAnswerType();
        type.setAnswer(answer);
        type.setDetails(details);
        return type;
    }

    private static DocumentProvidedType document(boolean provided, String reason) {
        DocumentProvidedType type = new DocumentProvidedType();
        type.setProvided(provided);
        type.setNotProvidedReason(reason);
        return type;
    }

    private static WalesType validWales() {
        WalesType wales = new WalesType();
        wales.setIsExemptLandlord(false);
        wales.setProhibitedConduct(conduct(false, null, null, null));
        wales.setEnergyPerformanceCertificate(document(true, null));
        wales.setGasSafetyReport(document(true, null));
        wales.setElectricalInstallationConditionReport(document(true, null));
        return wales;
    }

    private void apply(WalesType wales, boolean asbApplies) {
        mapper.apply(wales, asbApplies, target, PATH, errors);
    }

    private java.util.List<String> errorPaths() {
        return errors.errors().stream().map(error -> error.path()).toList();
    }

    @Test
    void validBlockSetsExemptLandlordConductAndDocuments() {
        apply(validWales(), false);

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getIsExemptLandlord()).isEqualTo(VerticalYesNo.NO);
        assertThat(target.getProhibitedConductWalesClaim()).isEqualTo(VerticalYesNo.NO);
        assertThat(target.getProhibitedConductWalesClaimDetails()).isNull();
        assertThat(target.getPeriodicContractTermsWales()).isNull();
        assertThat(target.getAsbQuestionsWales()).isNull();
        assertThat(target.getRequiredDocumentsWales().getHasEnergyPerformanceCertificate())
            .isEqualTo(VerticalYesNo.YES);
        assertThat(target.getRequiredDocumentsWales().getEnergyPerformance()).isNull();
        assertThat(target.getRequiredDocumentsWales().getNoEpcReason()).isNull();
        assertThat(target.getRequiredDocumentsWales().getHasGasSafetyReport()).isEqualTo(VerticalYesNo.YES);
        assertThat(target.getRequiredDocumentsWales().getHasElectricalInstallationConditionReport())
            .isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void exemptLandlordIsRecorded() {
        WalesType wales = validWales();
        wales.setIsExemptLandlord(true);

        apply(wales, false);

        assertThat(target.getIsExemptLandlord()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void missingBlockIsRejected() {
        apply(null, false);

        assertThat(errorPaths()).containsExactly(ABSOLUTE);
        assertThat(target.getRequiredDocumentsWales()).isNull();
    }

    @Test
    void emptyBlockReportsEveryRequiredPart() {
        apply(new WalesType(), false);

        assertThat(errorPaths()).containsExactly(
            ABSOLUTE + "/IsExemptLandlord",
            ABSOLUTE + "/ProhibitedConduct",
            ABSOLUTE + "/EnergyPerformanceCertificate",
            ABSOLUTE + "/GasSafetyReport",
            ABSOLUTE + "/ElectricalInstallationConditionReport");
    }

    @Test
    void prohibitedConductClaimWithAgreedTermsRecordsEverything() {
        WalesType wales = validWales();
        wales.setProhibitedConduct(conduct(true, true, "Pets not allowed.", "Repeated harassment."));

        apply(wales, false);

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getProhibitedConductWalesClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(target.getProhibitedConductWalesClaimDetails()).isEqualTo("Repeated harassment.");
        assertThat(target.getPeriodicContractTermsWales().getAgreedTermsOfPeriodicContract())
            .isEqualTo(VerticalYesNo.YES);
        assertThat(target.getPeriodicContractTermsWales().getDetailsOfTerms()).isEqualTo("Pets not allowed.");
    }

    @Test
    void prohibitedConductClaimWithoutAgreedTermsOmitsTheTermsDetails() {
        WalesType wales = validWales();
        wales.setProhibitedConduct(conduct(true, false, "ignored", "Reason."));

        apply(wales, false);

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getPeriodicContractTermsWales().getAgreedTermsOfPeriodicContract())
            .isEqualTo(VerticalYesNo.NO);
        assertThat(target.getPeriodicContractTermsWales().getDetailsOfTerms()).isNull();
    }

    @Test
    void prohibitedConductClaimRequiresItsDetails() {
        WalesType wales = validWales();
        wales.setProhibitedConduct(conduct(true, null, null, null));

        apply(wales, false);

        assertThat(errorPaths()).containsExactly(
            ABSOLUTE + "/ProhibitedConduct/Details", ABSOLUTE + "/ProhibitedConduct/AgreedTermsOfPeriodicContract");
    }

    @Test
    void prohibitedConductAgreedTermsNeedDetailsWithinTheLimit() {
        WalesType wales = validWales();
        wales.setProhibitedConduct(conduct(true, true, null, "x".repeat(251)));
        apply(wales, false);
        wales.setProhibitedConduct(conduct(true, true, "x".repeat(251), "Reason."));
        apply(wales, false);

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/ProhibitedConduct/Details:TOO_LONG",
            ABSOLUTE + "/ProhibitedConduct/DetailsOfTerms:REQUIRED",
            ABSOLUTE + "/ProhibitedConduct/DetailsOfTerms:TOO_LONG");
    }

    @Test
    void asbQuestionsAreMappedWhenTheGroundIsClaimed() {
        WalesType wales = validWales();
        wales.setAntisocialBehaviour(answer(true, "Threats."));
        wales.setIllegalPurposesUse(answer(false, "ignored"));
        wales.setOtherProhibitedConduct(answer(true, "Damage."));

        apply(wales, true);

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getAsbQuestionsWales().getAntisocialBehaviour()).isEqualTo(VerticalYesNo.YES);
        assertThat(target.getAsbQuestionsWales().getAntisocialBehaviourDetails()).isEqualTo("Threats.");
        assertThat(target.getAsbQuestionsWales().getIllegalPurposesUse()).isEqualTo(VerticalYesNo.NO);
        assertThat(target.getAsbQuestionsWales().getIllegalPurposesUseDetails()).isNull();
        assertThat(target.getAsbQuestionsWales().getOtherProhibitedConduct()).isEqualTo(VerticalYesNo.YES);
        assertThat(target.getAsbQuestionsWales().getOtherProhibitedConductDetails()).isEqualTo("Damage.");
    }

    @Test
    void asbQuestionsAreRequiredWhenTheGroundIsClaimed() {
        apply(validWales(), true);

        assertThat(errorPaths()).containsExactly(
            ABSOLUTE + "/AntisocialBehaviour", ABSOLUTE + "/IllegalPurposesUse", ABSOLUTE + "/OtherProhibitedConduct");
    }

    @Test
    void asbYesAnswersNeedDetailsWithinTheLimit() {
        WalesType wales = validWales();
        wales.setAntisocialBehaviour(answer(true, null));
        wales.setIllegalPurposesUse(answer(true, "x".repeat(501)));
        wales.setOtherProhibitedConduct(answer(false, null));

        apply(wales, true);

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/AntisocialBehaviour/Details:REQUIRED",
            ABSOLUTE + "/IllegalPurposesUse/Details:TOO_LONG");
    }

    @Test
    void asbQuestionsAreRejectedWhenTheGroundIsNotClaimed() {
        WalesType wales = validWales();
        wales.setAntisocialBehaviour(answer(false, null));
        wales.setIllegalPurposesUse(answer(false, null));
        wales.setOtherProhibitedConduct(answer(false, null));

        apply(wales, false);

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/AntisocialBehaviour:NOT_ALLOWED",
            ABSOLUTE + "/IllegalPurposesUse:NOT_ALLOWED",
            ABSOLUTE + "/OtherProhibitedConduct:NOT_ALLOWED");
        assertThat(target.getAsbQuestionsWales()).isNull();
    }

    @Test
    void documentsNotProvidedRecordTheirReasons() {
        WalesType wales = validWales();
        wales.setEnergyPerformanceCertificate(document(false, "No EPC."));
        wales.setGasSafetyReport(document(false, "No gas."));
        wales.setElectricalInstallationConditionReport(document(false, "No EICR."));

        apply(wales, false);

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getRequiredDocumentsWales().getHasEnergyPerformanceCertificate())
            .isEqualTo(VerticalYesNo.NO);
        assertThat(target.getRequiredDocumentsWales().getNoEpcReason()).isEqualTo("No EPC.");
        assertThat(target.getRequiredDocumentsWales().getHasGasSafetyReport()).isEqualTo(VerticalYesNo.NO);
        assertThat(target.getRequiredDocumentsWales().getNoGasReportReason()).isEqualTo("No gas.");
        assertThat(target.getRequiredDocumentsWales().getHasElectricalInstallationConditionReport())
            .isEqualTo(VerticalYesNo.NO);
        assertThat(target.getRequiredDocumentsWales().getNoEicrReason()).isEqualTo("No EICR.");
    }

    @Test
    void documentsNotProvidedNeedAReasonWithinTheLimit() {
        WalesType wales = validWales();
        wales.setEnergyPerformanceCertificate(document(false, null));
        wales.setGasSafetyReport(document(false, "x".repeat(501)));
        wales.setElectricalInstallationConditionReport(document(false, " "));

        apply(wales, false);

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/EnergyPerformanceCertificate/NotProvidedReason:REQUIRED",
            ABSOLUTE + "/GasSafetyReport/NotProvidedReason:TOO_LONG",
            ABSOLUTE + "/ElectricalInstallationConditionReport/NotProvidedReason:REQUIRED");
    }

    @Test
    void providedDocumentIgnoresAReason() {
        WalesType wales = validWales();
        wales.setGasSafetyReport(document(true, "ignored"));

        apply(wales, false);

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getRequiredDocumentsWales().getNoGasReportReason()).isNull();
    }
}
