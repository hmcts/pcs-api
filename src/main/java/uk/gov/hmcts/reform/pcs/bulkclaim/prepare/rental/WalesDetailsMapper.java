package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ConductAnswerType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.DocumentProvidedType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ProhibitedConductType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.WalesType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;

/**
 * Maps the Wales-only part of a rental claim: exempt landlord, prohibited conduct standard contract, antisocial
 * behaviour questions and the energy performance certificate, gas safety report and EICR. A document that is
 * provided is recorded as provided without a file; one that is not provided needs its reason.
 */
@Component
public class WalesDetailsMapper {

    private static final String ASB_DETAILS_LABEL = "Details of the actual or threatened antisocial behaviour";
    private static final String ILLEGAL_PURPOSES_DETAILS_LABEL =
        "Details of the actual or threatened use of the premises for illegal purposes";
    private static final String OTHER_CONDUCT_DETAILS_LABEL = "Details of other prohibited conduct";
    private static final String PROHIBITED_CONDUCT_DETAILS_LABEL = "Why you are making the prohibited conduct claim";
    private static final String PERIODIC_TERMS_DETAILS_LABEL = "Details of the terms agreed";

    /**
     * Maps the Wales block onto the case.
     *
     * @param asbApplies whether the grounds claimed make the antisocial behaviour questions apply
     * @param path path of the Wales element relative to the claim
     */
    public void apply(WalesType wales,
                      boolean asbApplies,
                      PCSCase target,
                      String path,
                      BulkErrorCollector errors) {
        if (wales == null) {
            errors.add(path, BulkErrorCollector.REQUIRED_CODE, "Wales is required for a claim in Wales");
            return;
        }

        if (errors.required(path + "/IsExemptLandlord", wales.isIsExemptLandlord(), "IsExemptLandlord")) {
            target.setIsExemptLandlord(VerticalYesNo.from(wales.isIsExemptLandlord()));
        }
        prohibitedConduct(wales.getProhibitedConduct(), target, path + "/ProhibitedConduct", errors);
        antisocialBehaviour(wales, asbApplies, target, path, errors);
        target.setRequiredDocumentsWales(documents(wales, path, errors));
    }

    private void prohibitedConduct(ProhibitedConductType conduct,
                                   PCSCase target,
                                   String path,
                                   BulkErrorCollector errors) {
        if (!errors.required(path, conduct, "ProhibitedConduct")) {
            return;
        }
        if (!conduct.isClaim()) {
            target.setProhibitedConductWalesClaim(VerticalYesNo.NO);
            return;
        }

        target.setProhibitedConductWalesClaim(VerticalYesNo.YES);
        errors.requiredWithMaxLength(path + "/Details", conduct.getDetails(), BulkLimits.SHORT_TEXT,
                                     PROHIBITED_CONDUCT_DETAILS_LABEL);
        target.setProhibitedConductWalesClaimDetails(conduct.getDetails());

        Boolean agreedTerms = conduct.isAgreedTermsOfPeriodicContract();
        PeriodicContractTermsWales.PeriodicContractTermsWalesBuilder terms = PeriodicContractTermsWales.builder();
        if (errors.required(path + "/AgreedTermsOfPeriodicContract", agreedTerms,
                            "AgreedTermsOfPeriodicContract")) {
            terms.agreedTermsOfPeriodicContract(VerticalYesNo.from(agreedTerms));
            if (Boolean.TRUE.equals(agreedTerms)) {
                errors.requiredWithMaxLength(path + "/DetailsOfTerms", conduct.getDetailsOfTerms(),
                                             BulkLimits.SHORT_TEXT, PERIODIC_TERMS_DETAILS_LABEL);
                terms.detailsOfTerms(conduct.getDetailsOfTerms());
            }
        }
        target.setPeriodicContractTermsWales(terms.build());
    }

    private void antisocialBehaviour(WalesType wales,
                                     boolean asbApplies,
                                     PCSCase target,
                                     String path,
                                     BulkErrorCollector errors) {
        if (!asbApplies) {
            rejectConductAnswer(wales.getAntisocialBehaviour(), path + "/AntisocialBehaviour", errors);
            rejectConductAnswer(wales.getIllegalPurposesUse(), path + "/IllegalPurposesUse", errors);
            rejectConductAnswer(wales.getOtherProhibitedConduct(), path + "/OtherProhibitedConduct", errors);
            return;
        }

        ASBQuestionsDetailsWales.ASBQuestionsDetailsWalesBuilder asb = ASBQuestionsDetailsWales.builder();
        conductAnswer(wales.getAntisocialBehaviour(), path + "/AntisocialBehaviour", ASB_DETAILS_LABEL, errors,
                      (answer, details) -> asb.antisocialBehaviour(answer).antisocialBehaviourDetails(details));
        conductAnswer(wales.getIllegalPurposesUse(), path + "/IllegalPurposesUse", ILLEGAL_PURPOSES_DETAILS_LABEL,
                      errors, (answer, details) -> asb.illegalPurposesUse(answer).illegalPurposesUseDetails(details));
        conductAnswer(wales.getOtherProhibitedConduct(), path + "/OtherProhibitedConduct",
                      OTHER_CONDUCT_DETAILS_LABEL, errors,
                      (answer, details) -> asb.otherProhibitedConduct(answer).otherProhibitedConductDetails(details));
        target.setAsbQuestionsWales(asb.build());
    }

    private interface AnswerSetter {
        void set(VerticalYesNo answer, String details);
    }

    private void conductAnswer(ConductAnswerType conduct,
                               String path,
                               String detailsLabel,
                               BulkErrorCollector errors,
                               AnswerSetter setter) {
        if (!errors.required(path, conduct, path.substring(path.lastIndexOf('/') + 1)
            + " is required when the antisocial behaviour ground is claimed")) {
            return;
        }
        if (!conduct.isAnswer()) {
            setter.set(VerticalYesNo.NO, null);
            return;
        }
        errors.requiredWithMaxLength(path + "/Details", conduct.getDetails(), BulkLimits.MEDIUM_TEXT, detailsLabel);
        setter.set(VerticalYesNo.YES, conduct.getDetails());
    }

    private void rejectConductAnswer(ConductAnswerType conduct, String path, BulkErrorCollector errors) {
        if (conduct != null) {
            errors.add(path, BulkErrorCollector.NOT_ALLOWED,
                       "Only applies when the antisocial behaviour ground is claimed");
        }
    }

    private WalesDocuments documents(WalesType wales, String path, BulkErrorCollector errors) {
        WalesDocuments.WalesDocumentsBuilder documents = WalesDocuments.builder();
        document(wales.getEnergyPerformanceCertificate(), path + "/EnergyPerformanceCertificate",
                 WalesDocuments.NO_ENERGY_PERFORMANCE_CERTIFICATE_REASON_LABEL, errors,
                 (provided, reason) -> documents.hasEnergyPerformanceCertificate(provided).noEpcReason(reason));
        document(wales.getGasSafetyReport(), path + "/GasSafetyReport",
                 WalesDocuments.NO_GAS_SAFETY_REPORT_REASON_LABEL, errors,
                 (provided, reason) -> documents.hasGasSafetyReport(provided).noGasReportReason(reason));
        document(wales.getElectricalInstallationConditionReport(), path + "/ElectricalInstallationConditionReport",
                 WalesDocuments.NO_ELECTRICAL_INSTALLATION_CONDITION_REPORT_REASON_LABEL, errors,
                 (provided, reason) ->
                     documents.hasElectricalInstallationConditionReport(provided).noEicrReason(reason));
        return documents.build();
    }

    private void document(DocumentProvidedType document,
                          String path,
                          String reasonLabel,
                          BulkErrorCollector errors,
                          AnswerSetter setter) {
        if (!errors.required(path, document, path.substring(path.lastIndexOf('/') + 1))) {
            return;
        }
        if (document.isProvided()) {
            setter.set(VerticalYesNo.YES, null);
            return;
        }
        errors.requiredWithMaxLength(path + "/NotProvidedReason", document.getNotProvidedReason(),
                                     BulkLimits.MEDIUM_TEXT, reasonLabel);
        setter.set(VerticalYesNo.NO, document.getNotProvidedReason());
    }
}
