package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference;
import uk.gov.hmcts.reform.pcs.ccd.domain.SimpleYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementLegalRep;
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
            statementOfTruth(warrantDetails, warrantEntity);
        }
        return warrantEntity;
    }

    private void controlFlags(WarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setShowPeopleWhoWillBeEvictedPage(
            convertYesOrNoToVerticalYesNo(warrantDetails.getShowPeopleWhoWillBeEvictedPage()));
        warrantEntity.setShowPeopleYouWantToEvictPage(
            convertYesOrNoToVerticalYesNo(warrantDetails.getShowPeopleYouWantToEvictPage()));
    }

    private void statementOfTruth(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getStatementOfTruth() != null) {
            StatementOfTruthDetailsEnforcement statementOfTruth = warrantDetails.getStatementOfTruth();
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
        List<StatementOfTruthAgreementLegalRep> agreementLegalRep = statementOfTruth.getAgreementLegalRep();
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

    private void suspendTheOrder(WarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setIsSuspendedOrder(warrantDetails.getIsSuspendedOrder());
    }

    private void peopleToEvict(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getPeopleToEvict() != null) {
            warrantEntity.setEvictEveryone(warrantDetails.getPeopleToEvict().getEvictEveryone());
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

    private void legalCosts(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        LegalCosts legalCosts = warrantDetails.getLegalCosts();
        if (legalCosts != null) {
            warrantEntity.setAreLegalCostsToBeClaimed(legalCosts.getAreLegalCostsToBeClaimed());
            warrantEntity.setAmountOfLegalCosts(legalCosts.getAmountOfLegalCosts());
        }
    }

    private void landRegistry(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        LandRegistryFees landRegistryFees = warrantDetails.getLandRegistryFees();
        if (landRegistryFees != null) {
            warrantEntity.setHaveLandRegistryFeesBeenPaid(landRegistryFees.getHaveLandRegistryFeesBeenPaid());
            warrantEntity.setAmountOfLandRegistryFees(landRegistryFees.getAmountOfLandRegistryFees());
        }
    }

    private void repayment(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getRepaymentCosts() != null) {
            RepaymentCosts repaymentCosts = warrantDetails.getRepaymentCosts();
            RepaymentPreference repaymentChoice = repaymentCosts.getRepaymentChoice();
            warrantEntity.setRepaymentChoice(repaymentChoice.getLabel());
            warrantEntity.setAmountOfRepaymentCosts(repaymentCosts.getAmountOfRepaymentCosts());
            warrantEntity.setRepaymentSummaryMarkdown(repaymentCosts.getRepaymentSummaryMarkdown());
        }
    }

    private void defendantsDOB(WarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setDefendantsDOBKnown(warrantDetails.getDefendantsDOBKnown());
        if (warrantDetails.getDefendantsDOB() != null) {
            warrantEntity.setDefendantsDOBDetails(
                warrantDetails.getDefendantsDOB().getDefendantsDOBDetails());
        }
    }

    private void moneyOwed(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getMoneyOwedByDefendants() != null) {
            warrantEntity.setAmountOwed(warrantDetails.getMoneyOwedByDefendants().getAmountOwed());
        }
    }

    private SimpleYesNo convertYesOrNoToVerticalYesNo(YesOrNo yesOrNo) {
        return yesOrNo == YesOrNo.YES ? SimpleYesNo.YES : SimpleYesNo.NO;
    }
}
