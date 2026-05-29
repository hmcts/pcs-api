package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;

@Service
@AllArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimGroundService claimGroundService;
    private final PossessionAlternativesService possessionAlternativesService;
    private final AsbProhibitedConductService asbProhibitedConductService;
    private final RentArrearsService rentArrearsService;
    private final NoticeOfPossessionService noticeOfPossessionService;
    private final StatementOfTruthService statementOfTruthService;

    public ClaimEntity createMainClaimEntity(PCSCase pcsCase) {
        ClaimEntity claimEntity = buildClaimEntity(pcsCase);

        List<ClaimGroundEntity> claimGrounds = claimGroundService.createClaimGroundEntities(pcsCase);
        claimEntity.addClaimGrounds(claimGrounds);

        claimEntity.setPossessionAlternativesEntity(
            possessionAlternativesService.createPossessionAlternativesEntity(pcsCase));

        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            claimEntity
                .setAsbProhibitedConductEntity(asbProhibitedConductService.createAsbProhibitedConductEntity(pcsCase));

            WalesDocuments walesDocuments = pcsCase.getRequiredDocumentsWales();
            if (walesDocuments != null) {
                VerticalYesNo hasEnergyPerformanceCertificate = walesDocuments.getHasEnergyPerformanceCertificate();
                claimEntity.setEnergyPerformanceCertificateProvided(hasEnergyPerformanceCertificate);
                if (hasEnergyPerformanceCertificate == VerticalYesNo.NO) {
                    claimEntity.setNoEnergyPerformanceCertificateReason(
                        walesDocuments.getNoEnergyPerformanceCertificateReason()
                    );
                }

                VerticalYesNo hasGasSafetyReport = walesDocuments.getHasGasSafetyReport();
                claimEntity.setGasSafetyReportProvided(hasGasSafetyReport);
                if (hasGasSafetyReport == VerticalYesNo.NO) {
                    claimEntity.setNoGasSafetyReportReason(walesDocuments.getNoGasSafetyReportReason());
                }

                VerticalYesNo hasElectricalInstallationConditionReport =
                    walesDocuments.getHasElectricalInstallationConditionReport();
                claimEntity.setElectricalInstallationConditionProvided(hasElectricalInstallationConditionReport);
                if (hasElectricalInstallationConditionReport == VerticalYesNo.NO) {
                    claimEntity.setNoElectricalInstallationConditionReason(
                        walesDocuments.getNoElectricalInstallationConditionReportReason()
                    );
                }
            }
        }

        claimEntity.setRentArrears(rentArrearsService.createRentArrearsEntity(pcsCase));
        claimEntity.setNoticeOfPossession(noticeOfPossessionService.createNoticeOfPossessionEntity(pcsCase));
        claimEntity.setStatementOfTruth(statementOfTruthService.createStatementOfTruthEntity(pcsCase));

        claimRepository.save(claimEntity);

        return claimEntity;
    }


    private ClaimEntity buildClaimEntity(PCSCase pcsCase) {
        AdditionalReasons additionalReasons = pcsCase.getAdditionalReasonsForPossession();

        ClaimantCircumstances claimantCircumstances = pcsCase.getClaimantCircumstances();
        DefendantCircumstances defendantCircumstances = pcsCase.getDefendantCircumstances();

        return ClaimEntity.builder()
            .claimantType(pcsCase.getClaimantType() != null
                              ? ClaimantType.fromName(pcsCase.getClaimantType().getValueCode()) : null)
            .againstTrespassers(pcsCase.getClaimAgainstTrespassers())
            .dueToRentArrears(pcsCase.getClaimDueToRentArrears())
            .preActionProtocolFollowed(pcsCase.getPreActionProtocolCompleted())
            .preActionProtocolIncompleteExplanation(pcsCase.getPreActionProtocolIncompleteExplanation())
            .mediationAttempted(pcsCase.getMediationAttempted())
            .settlementAttempted(pcsCase.getSettlementAttempted())
            .claimantCircumstancesProvided(claimantCircumstances != null
                                               ? claimantCircumstances.getClaimantCircumstancesSelect() : null)
            .claimantCircumstances(claimantCircumstances != null
                                       ? claimantCircumstances.getClaimantCircumstancesDetails() : null)
            .additionalDefendants(pcsCase.getAddAnotherDefendant())
            .defendantCircumstancesProvided(defendantCircumstances != null
                                                ? defendantCircumstances.getHasDefendantCircumstancesInfo() : null)
            .defendantCircumstances(defendantCircumstances != null
                                        ? defendantCircumstances.getDefendantCircumstancesInfo() : null)
            .additionalReasonsProvided(additionalReasons != null
                                           ? additionalReasons.getHasReasons() : null)
            .additionalReasons(additionalReasons != null
                                   ? additionalReasons.getReasons() : null)
            .underlesseeOrMortgagee(pcsCase.getHasUnderlesseeOrMortgagee())
            .additionalUnderlesseesOrMortgagees(pcsCase.getAddAdditionalUnderlesseeOrMortgagee())
            .additionalDocsProvided(pcsCase.getWantToUploadDocuments())
            .genAppExpected(pcsCase.getApplicationWithClaim())
            .languageUsed(pcsCase.getLanguageUsed())
            .isExemptLandlord(pcsCase.getIsExemptLandlord())
            .build();
    }

}
