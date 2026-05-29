package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClaimView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (!pcsCaseEntity.getClaims().isEmpty()) {
            ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
            mapBasicClaimFields(pcsCase, mainClaim);
            mapComplexClaimFields(pcsCase, mainClaim, pcsCaseEntity);
        }
    }

    private void mapBasicClaimFields(PCSCase pcsCase, ClaimEntity claim) {
        pcsCase.setClaimAgainstTrespassers(claim.getAgainstTrespassers());
        pcsCase.setClaimDueToRentArrears(claim.getDueToRentArrears());
        pcsCase.setPreActionProtocolCompleted(claim.getPreActionProtocolFollowed());
        pcsCase.setPreActionProtocolIncompleteExplanation(claim.getPreActionProtocolIncompleteExplanation());
        pcsCase.setMediationAttempted(claim.getMediationAttempted());
        pcsCase.setSettlementAttempted(claim.getSettlementAttempted());
        pcsCase.setAddAnotherDefendant(claim.getAdditionalDefendants());
        pcsCase.setHasUnderlesseeOrMortgagee(claim.getUnderlesseeOrMortgagee());
        pcsCase.setAddAdditionalUnderlesseeOrMortgagee(claim.getAdditionalUnderlesseesOrMortgagees());
        pcsCase.setApplicationWithClaim(claim.getGenAppExpected());
        pcsCase.setLanguageUsed(claim.getLanguageUsed());
        pcsCase.setWantToUploadDocuments(claim.getAdditionalDocsProvided());
        pcsCase.setIsExemptLandlord(claim.getIsExemptLandlord());
    }

    private void mapComplexClaimFields(PCSCase pcsCase, ClaimEntity claim, PcsCaseEntity pcsCaseEntity) {
        pcsCase.setClaimantCircumstances(
            ClaimantCircumstances.builder()
                .claimantCircumstancesSelect(claim.getClaimantCircumstancesProvided())
                .claimantCircumstancesDetails(claim.getClaimantCircumstances())
                .build()
        );

        pcsCase.setDefendantCircumstances(
            DefendantCircumstances.builder()
                .hasDefendantCircumstancesInfo(claim.getDefendantCircumstancesProvided())
                .defendantCircumstancesInfo(claim.getDefendantCircumstances())
                .build()
        );

        pcsCase.setAdditionalReasonsForPossession(
            AdditionalReasons.builder()
                .hasReasons(claim.getAdditionalReasonsProvided())
                .reasons(claim.getAdditionalReasons())
                .build()
        );

        pcsCase.setRequiredDocumentsWales(
            WalesDocuments.builder()
                .hasEnergyPerformanceCertificate(claim.getEnergyPerformanceCertificateProvided())
                .hasGasSafetyReport(claim.getGasSafetyReportProvided())
                .hasElectricalInstallationConditionReport(claim.getElectricalInstallationConditionProvided())
                .noEpcReason(claim.getNoEnergyPerformanceCertificateReason())
                .noGasReportReason(claim.getNoGasSafetyReportReason())
                .noEicrReason(claim.getNoElectricalInstallationConditionReason())
                .energyPerformance(getEnergyPerformanceCertificate(pcsCaseEntity))
                .gasSafetyReport(getGasSafetyReport(pcsCaseEntity))
                .electricalInstallation(getElectricalInstallationCondition(pcsCaseEntity))
                .build()
        );

        if (claim.getClaimantType() != null) {
            pcsCase.setClaimantType(DynamicStringList.builder()
                                        .value(DynamicStringListElement.builder().code(claim.getClaimantType().name())
                                                   .label(claim.getClaimantType().getLabel())
                                                   .build())
                                        .build());
        }

    }

    private static List<ListValue<Document>> getEnergyPerformanceCertificate(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return new ArrayList<>();
        }

        return pcsCaseEntity.getDocuments().stream()
            .filter(ClaimView::isEnergyPerformanceCertificate)
            .map(ClaimView::toDocument)
            .toList();
    }

    private static List<ListValue<Document>> getGasSafetyReport(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return new ArrayList<>();
        }

        return pcsCaseEntity.getDocuments().stream()
            .filter(ClaimView::isGasSafetyReport)
            .map(ClaimView::toDocument)
            .toList();
    }

    private static List<ListValue<Document>> getElectricalInstallationCondition(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return new ArrayList<>();
        }

        return pcsCaseEntity.getDocuments().stream()
            .filter(ClaimView::isElectricalInstallationCondition)
            .map(ClaimView::toDocument)
            .toList();
    }

    private static boolean isEnergyPerformanceCertificate(DocumentEntity documentEntity) {
        return documentEntity.getType() == DocumentType.ENERGY_PERFORMANCE_CERTIFICATE;
    }

    private static boolean isGasSafetyReport(DocumentEntity documentEntity) {
        return documentEntity.getType() == DocumentType.GAS_SAFETY_REPORT;
    }

    private static boolean isElectricalInstallationCondition(DocumentEntity documentEntity) {
        return documentEntity.getType() == DocumentType.ELECTRICAL_INSTALLATION_CONDITION;
    }

    private static ListValue<Document> toDocument(DocumentEntity documentEntity) {
        return ListValue.<Document>builder()
            .value(
                Document.builder()
                    .url(documentEntity.getUrl())
                    .filename(documentEntity.getFileName())
                    .binaryUrl(documentEntity.getBinaryUrl())
                    .categoryId(documentEntity.getCategoryId())
                    .build()
            ).build();
    }

}
