package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant;


import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementWarrantEntity;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EnforcementWarrantMapper {

    public EnforcementWarrantEntity toEntity(EnforcementOrder enforcementOrder,
        EnforcementOrderEntity enforcementOrderEntity) {
        EnforcementWarrantEntity warrantEntity = EnforcementWarrantEntity.builder()
            .enforcementOrder(enforcementOrderEntity).build();
        if (enforcementOrder.getWarrantDetails() != null) {
            WarrantDetails warrantDetails = enforcementOrder.getWarrantDetails();
            controlFlags(warrantEntity, warrantDetails);
            suspendTheOrder(warrantEntity, warrantDetails);
            additionalInformation(warrantDetails, warrantEntity);
            nameAndAddressForEviction(warrantDetails, warrantEntity);
            peopleToEvict(warrantDetails, warrantEntity);
            propertyAccessDetails(warrantDetails, warrantEntity);
            legalCosts(warrantDetails, warrantEntity);
            moneyOwed(warrantDetails, warrantEntity);
            landRegistry(warrantDetails, warrantEntity);
            repayment(warrantDetails, warrantEntity);
            defendantsDOB(warrantEntity, warrantDetails);
            riskAssessment(warrantEntity, warrantDetails);
            statementOfTruth(warrantDetails, warrantEntity);
        }
        return warrantEntity;
    }

    private void controlFlags(EnforcementWarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setShowPeopleWhoWillBeEvictedPage(
            convertYesOrNoToVerticalYesNo(warrantDetails.getShowPeopleWhoWillBeEvictedPage()));
        warrantEntity.setShowPeopleYouWantToEvictPage(
            convertYesOrNoToVerticalYesNo(warrantDetails.getShowPeopleYouWantToEvictPage()));
    }

    private void statementOfTruth(WarrantDetails warrantDetails, EnforcementWarrantEntity warrantEntity) {
        if (warrantDetails.getStatementOfTruth() != null) {
            StatementOfTruthDetails statementOfTruth = warrantDetails.getStatementOfTruth();
            warrantEntity.setCompletedBy(statementOfTruth.getCompletedBy());
            warrantEntity.setFullNameClaimant(statementOfTruth.getFullNameClaimant());
            warrantEntity.setPositionClaimant(statementOfTruth.getPositionClaimant());
            warrantEntity.setFullNameLegalRep(statementOfTruth.getFullNameLegalRep());
            warrantEntity.setFirmNameLegalRep(statementOfTruth.getFirmNameLegalRep());
            warrantEntity.setPositionLegalRep(statementOfTruth.getPositionLegalRep());

            agreementClaimant(warrantEntity, statementOfTruth);
            agreementLegalRep(warrantEntity, statementOfTruth);
            certification(warrantEntity, statementOfTruth);
        }
    }

    private void certification(EnforcementWarrantEntity warrantEntity, StatementOfTruthDetails statementOfTruth) {
        List<StatementOfTruthAgreement> certification = statementOfTruth.getCertification();
        if (certification != null && !certification.isEmpty()) {
            warrantEntity.setCertification(
                certification.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","))
            );
        }
    }

    private void agreementLegalRep(EnforcementWarrantEntity warrantEntity, StatementOfTruthDetails statementOfTruth) {
        List<StatementOfTruthAgreementLegalRep> agreementLegalRep = statementOfTruth.getAgreementLegalRep();
        if (agreementLegalRep != null && !agreementLegalRep.isEmpty()) {
            warrantEntity.setAgreementLegalRep(agreementLegalRep.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","))
            );
        }
    }

    private void agreementClaimant(EnforcementWarrantEntity warrantEntity, StatementOfTruthDetails statementOfTruth) {
        List<StatementOfTruthAgreementClaimant> agreementClaimant = statementOfTruth.getAgreementClaimant();
        if (agreementClaimant != null && !agreementClaimant.isEmpty()) {
            warrantEntity.setAgreementClaimant(
                agreementClaimant.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","))
            );
        }
    }

    private void suspendTheOrder(EnforcementWarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setIsSuspendedOrder(warrantDetails.getIsSuspendedOrder());
    }

    private void peopleToEvict(WarrantDetails warrantDetails, EnforcementWarrantEntity warrantEntity) {
        if (warrantDetails.getPeopleToEvict() != null) {
            warrantEntity.setEvictEveryone(warrantDetails.getPeopleToEvict().getEvictEveryone());
        }
    }

    private void nameAndAddressForEviction(WarrantDetails warrantDetails, EnforcementWarrantEntity warrantEntity) {
        if (warrantDetails.getNameAndAddressForEviction() != null) {
            warrantEntity.setCorrectNameAndAddress(
                warrantDetails.getNameAndAddressForEviction().getCorrectNameAndAddress());
        }
    }

    private void additionalInformation(WarrantDetails warrantDetails, EnforcementWarrantEntity warrantEntity) {
        if (warrantDetails.getAdditionalInformation() != null) {
            warrantEntity.setAdditionalInformationSelect(
                warrantDetails.getAdditionalInformation().getAdditionalInformationSelect());
            warrantEntity.setAdditionalInformationDetails(
                warrantDetails.getAdditionalInformation().getAdditionalInformationDetails());
        }
    }

    private void propertyAccessDetails(WarrantDetails warrantDetails, EnforcementWarrantEntity warrantEntity) {
        if (warrantDetails.getPropertyAccessDetails() != null) {
            warrantEntity.setIsDifficultToAccessProperty(
                warrantDetails.getPropertyAccessDetails().getIsDifficultToAccessProperty());
            warrantEntity.setClarificationOnAccessDifficultyText(
                warrantDetails.getPropertyAccessDetails().getClarificationOnAccessDifficultyText());
        }
    }

    private void legalCosts(WarrantDetails warrantDetails, EnforcementWarrantEntity warrantEntity) {
        LegalCosts legalCosts = warrantDetails.getLegalCosts();
        if (legalCosts != null) {
            warrantEntity.setAreLegalCostsToBeClaimed(legalCosts.getAreLegalCostsToBeClaimed());
            warrantEntity.setAmountOfLegalCosts(legalCosts.getAmountOfLegalCosts());
        }
    }

    private void landRegistry(WarrantDetails warrantDetails, EnforcementWarrantEntity warrantEntity) {
        LandRegistryFees landRegistryFees = warrantDetails.getLandRegistryFees();
        if (landRegistryFees != null) {
            warrantEntity.setHaveLandRegistryFeesBeenPaid(landRegistryFees.getHaveLandRegistryFeesBeenPaid());
            warrantEntity.setAmountOfLandRegistryFees(landRegistryFees.getAmountOfLandRegistryFees());
        }
    }

    private void repayment(WarrantDetails warrantDetails, EnforcementWarrantEntity warrantEntity) {
        if (warrantDetails.getRepaymentCosts() != null) {
            RepaymentCosts repaymentCosts = warrantDetails.getRepaymentCosts();
            RepaymentPreference repaymentChoice = repaymentCosts.getRepaymentChoice();
            if (repaymentChoice != null) {
                warrantEntity.setRepaymentChoice(repaymentChoice.getLabel());
            }
            warrantEntity.setAmountOfRepaymentCosts(repaymentCosts.getAmountOfRepaymentCosts());
            warrantEntity.setRepaymentSummaryMarkdown(repaymentCosts.getRepaymentSummaryMarkdown());
        }
    }

    private void riskAssessment(EnforcementWarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        if (warrantDetails.getEnforcementRiskCategories() != null) {
            warrantEntity.setEnforcementRiskCategories(
                warrantDetails.getEnforcementRiskCategories().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","))
            );
        }
    }

    private void defendantsDOB(EnforcementWarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setDefendantsDOBKnown(warrantDetails.getDefendantsDOBKnown());
        if (warrantDetails.getDefendantsDOB() != null) {
            warrantEntity.setDefendantsDOBDetails(
                warrantDetails.getDefendantsDOB().getDefendantsDOBDetails());
        }
    }

    private void moneyOwed(WarrantDetails warrantDetails, EnforcementWarrantEntity warrantEntity) {
        if (warrantDetails.getMoneyOwedByDefendants() != null) {
            warrantEntity.setAmountOwed(warrantDetails.getMoneyOwedByDefendants().getAmountOwed());
        }
    }

    private VerticalYesNo convertYesOrNoToVerticalYesNo(YesOrNo yesOrNo) {
        return yesOrNo == YesOrNo.YES ? VerticalYesNo.YES : VerticalYesNo.NO;
    }
}
