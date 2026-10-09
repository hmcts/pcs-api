package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.AgreementClaimantLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;

import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class WarrantDetailsMapper {

    public WarrantEntity toEntity(EnforcementOrder enforcementOrder,
                                  EnforcementOrderEntity enforcementOrderEntity) {
        WarrantEntity warrantEntity = WarrantEntity.builder()
            .enforcementOrder(enforcementOrderEntity).build();
        if (enforcementOrder.getWarrantDetails() != null) {
            WarrantDetails warrantDetails = enforcementOrder.getWarrantDetails();
            warrantEntity.setLanguageUsed(warrantDetails.getLanguageUsed());
            additionalInformation(warrantDetails, warrantEntity);
            nameAndAddressForEviction(warrantDetails, warrantEntity);
            propertyAccessDetails(warrantDetails, warrantEntity);
            defendantsDOB(warrantEntity, warrantDetails);
            statementOfTruth(warrantDetails, warrantEntity);
        }
        return warrantEntity;
    }

    private void statementOfTruth(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getStatementOfTruth() != null) {
            StatementOfTruthDetailsEnforcement statementOfTruth = warrantDetails.getStatementOfTruth();
            warrantEntity.setCompletedBy(statementOfTruth.getCompletedBy());
            warrantEntity.setFullNameClaimant(statementOfTruth.getFullNameParty());
            warrantEntity.setPositionClaimant(statementOfTruth.getPositionParty());
            warrantEntity.setFullNameLegalRep(statementOfTruth.getFullNameLegalRep());
            warrantEntity.setFirmNameLegalRep(statementOfTruth.getFirmNameLegalRep());
            warrantEntity.setPositionLegalRep(statementOfTruth.getPositionLegalRep());

            agreementClaimant(warrantEntity, statementOfTruth);
            agreementLegalRep(warrantEntity, statementOfTruth);
            certification(warrantEntity, statementOfTruth);
        }
    }

    private void certification(WarrantEntity warrantEntity,
                               StatementOfTruthDetailsEnforcement statementOfTruth) {
        List<StatementOfTruthAgreement> certification = statementOfTruth.getCertification();
        if (certification != null && !certification.isEmpty()) {
            warrantEntity.setCertification(
                certification.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","))
            );
        }
    }

    private void agreementLegalRep(WarrantEntity warrantEntity,
                                   StatementOfTruthDetailsEnforcement statementOfTruth) {
        List<AgreementClaimantLegalRep> agreementLegalRep = statementOfTruth.getAgreementClaimantLegalRep();
        if (agreementLegalRep != null && !agreementLegalRep.isEmpty()) {
            warrantEntity.setAgreementLegalRep(agreementLegalRep.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","))
            );
        }
    }

    private void agreementClaimant(WarrantEntity warrantEntity,
                                   StatementOfTruthDetailsEnforcement statementOfTruth) {
        List<StatementOfTruthAgreementClaimant> agreementClaimant = statementOfTruth.getAgreementClaimant();
        if (agreementClaimant != null && !agreementClaimant.isEmpty()) {
            warrantEntity.setAgreementClaimant(
                agreementClaimant.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","))
            );
        }
    }

    private void nameAndAddressForEviction(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getNameAndAddressForEviction() != null) {
            warrantEntity.setCorrectNameAndAddress(
                warrantDetails.getNameAndAddressForEviction().getCorrectNameAndAddress());
        }
    }

    private void additionalInformation(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getAdditionalInformation() != null) {
            warrantEntity.setAdditionalInformationSelect(
                warrantDetails.getAdditionalInformation().getAdditionalInformationSelect());
            warrantEntity.setAdditionalInformationDetails(
                warrantDetails.getAdditionalInformation().getAdditionalInformationDetails());
        }
    }

    private void propertyAccessDetails(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getPropertyAccessDetails() != null) {
            warrantEntity.setIsDifficultToAccessProperty(
                warrantDetails.getPropertyAccessDetails().getIsDifficultToAccessProperty());
            warrantEntity.setClarificationOnAccessDifficultyText(
                warrantDetails.getPropertyAccessDetails().getClarificationOnAccessDifficultyText());
        }
    }

    private void defendantsDOB(WarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setDefendantsDOBKnown(warrantDetails.getDefendantsDOBKnown());
        if (warrantDetails.getDefendantsDOB() != null) {
            warrantEntity.setDefendantsDOBDetails(
                warrantDetails.getDefendantsDOB().getDefendantsDOBDetails());
        }
    }

}
