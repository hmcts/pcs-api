package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CaseView;
import uk.gov.hmcts.ccd.sdk.CaseViewRequest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseTitleService;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
public class PCSCaseView implements CaseView<PCSCase, State> {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private final DraftCaseDataService draftCaseDataService;
    private final CaseTitleService caseTitleService;
    private final DefendantService defendantService;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param request encapsulates the CCD case reference and state
     */
    @Override
    public PCSCase getCase(CaseViewRequest<State> request) {
        long caseReference = request.caseRef();
        State state = request.state();
        PCSCase pcsCase = getSubmittedCase(caseReference);

        boolean hasUnsubmittedCaseData = caseHasUnsubmittedData(caseReference, state);

        setMarkdownFields(pcsCase, hasUnsubmittedCaseData);

        return pcsCase;
    }

    private boolean caseHasUnsubmittedData(long caseReference, State state) {
        if (State.AWAITING_SUBMISSION_TO_HMCTS == state) {
            return draftCaseDataService.hasUnsubmittedCaseData(caseReference, resumePossessionClaim);
        } else {
            return false;
        }
    }

    private PCSCase getSubmittedCase(long caseReference) {
        PcsCaseEntity pcsCaseEntity = loadCaseData(caseReference);
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(convertAddress(pcsCaseEntity.getPropertyAddress()))
            .legislativeCountry(pcsCaseEntity.getLegislativeCountry())
            .caseManagementLocation(pcsCaseEntity.getCaseManagementLocation())
            .noticeServed(pcsCaseEntity.getTenancyLicence() != null
                && pcsCaseEntity.getTenancyLicence().getNoticeServed() != null
                ? YesOrNo.from(pcsCaseEntity.getTenancyLicence().getNoticeServed()) : null)
            .allDefendants(wrapListItems(defendantService.mapToDefendantDetails(pcsCaseEntity.getDefendants())))
            .build();

        setDerivedProperties(pcsCase, pcsCaseEntity);
        setRentDetails(pcsCase, pcsCaseEntity);
        setClaimFields(pcsCase, pcsCaseEntity);

        return pcsCase;
    }

    private void setDerivedProperties(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        boolean pcqIdSet = findPartyForCurrentUser(pcsCaseEntity)
            .map(party -> party.getPcqId() != null)
            .orElse(false);

        pcsCase.setUserPcqIdSet(YesOrNo.from(pcqIdSet));

        pcsCase.setParties(mapAndWrapParties(pcsCaseEntity.getParties()));
    }

    private void setRentDetails(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity.getTenancyLicence() != null) {
            pcsCase.setRentDetails(RentDetails.builder()
                .currentRent(pcsCaseEntity.getTenancyLicence().getRentAmount() != null
                    ? poundsToPence(pcsCaseEntity.getTenancyLicence().getRentAmount()) : null)
                .frequency(pcsCaseEntity.getTenancyLicence().getRentPaymentFrequency())
                .otherFrequency(pcsCaseEntity.getTenancyLicence().getOtherRentFrequency())
                .dailyCharge(pcsCaseEntity.getTenancyLicence().getDailyRentChargeAmount() != null
                    ? poundsToPence(pcsCaseEntity.getTenancyLicence().getDailyRentChargeAmount()) : null)
                .build());
        }
    }

    private void setMarkdownFields(PCSCase pcsCase, boolean hasUnsubmittedCaseData) {
        pcsCase.setCaseTitleMarkdown(caseTitleService.buildCaseTitle(pcsCase));

        if (hasUnsubmittedCaseData) {
            pcsCase.setNextStepsMarkdown("""
                                             <h2 class="govuk-heading-m">Resume claim</h2>
                                             You've already answered some questions about this claim.
                                             <br>
                                             <br>
                                             <a href="/cases/case-details/${[CASE_REFERENCE]}/trigger/%s"
                                                role="button"
                                                class="govuk-button govuk-link govuk-link--no-visited-state">
                                               Continue
                                             </a>
                                             <p class="govuk-body govuk-!-font-size-19">
                                             <span><a class="govuk-link--no-visited-state" href="/cases">Cancel</a></span>
                                             </p>
                                             """.formatted(resumePossessionClaim));
        } else {
            pcsCase.setNextStepsMarkdown("""
                                             <h2 class="govuk-heading-m">Provide more details about your claim</h2>
                                             Your answers will be saved from this point so you can return to your draft
                                             later.
                                             <br>
                                             <br>
                                             <a href="/cases/case-details/${[CASE_REFERENCE]}/trigger/%s"
                                                role="button"
                                                class="govuk-button govuk-link govuk-link--no-visited-state">
                                               Continue
                                             </a>
                                             <p class="govuk-body govuk-!-font-size-19">
                                             <span><a class="govuk-link--no-visited-state" href="/cases">Cancel</a></span>
                                             </p>
                                             """.formatted(resumePossessionClaim));
        }
    }

    private Optional<PartyEntity> findPartyForCurrentUser(PcsCaseEntity pcsCaseEntity) {
        UUID userId = securityContextService.getCurrentUserId();

        if (userId != null) {
            return pcsCaseEntity.getParties().stream()
                .filter(party -> userId.equals(party.getIdamId()))
                .findFirst();
        } else {
            return Optional.empty();
        }
    }

    private AddressUK convertAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }

        return modelMapper.map(address, AddressUK.class);
    }

    private PcsCaseEntity loadCaseData(long caseRef) {
        return pcsCaseRepository.findByCaseReference(caseRef)
            .orElseThrow(() -> new CaseNotFoundException(caseRef));
    }

    private List<ListValue<Party>> mapAndWrapParties(Set<PartyEntity> partyEntities) {
        return partyEntities.stream()
            .map(entity -> modelMapper.map(entity, Party.class))
            .collect(Collectors.collectingAndThen(Collectors.toList(), ListValueUtils::wrapListItems));
    }

    private static String poundsToPence(java.math.BigDecimal pounds) {
        if (pounds == null) {
            return null;
        }
        return pounds.movePointRight(2).toPlainString();
    }

    private void mapBasicClaimFields(PCSCase pcsCase, ClaimEntity claim) {
        pcsCase.setClaimAgainstTrespassers(claim.getAgainstTrespassers());
        pcsCase.setClaimDueToRentArrears(claim.getDueToRentArrears());
        pcsCase.setClaimingCostsWanted(claim.getClaimCosts());
        pcsCase.setPreActionProtocolCompleted(claim.getPreActionProtocolFollowed());
        pcsCase.setMediationAttempted(claim.getMediationAttempted());
        pcsCase.setMediationAttemptedDetails(claim.getMediationDetails());
        pcsCase.setSettlementAttempted(claim.getSettlementAttempted());
        pcsCase.setSettlementAttemptedDetails(claim.getSettlementDetails());
        pcsCase.setAddAnotherDefendant(claim.getAdditionalDefendants());
        pcsCase.setHasUnderlesseeOrMortgagee(claim.getUnderlesseeOrMortgagee());
        pcsCase.setAddAdditionalUnderlesseeOrMortgagee(claim.getAdditionalUnderlesseesOrMortgagees());
        pcsCase.setApplicationWithClaim(claim.getGenAppExpected());
        pcsCase.setLanguageUsed(claim.getLanguageUsed());
    }

    private void mapComplexClaimFields(PCSCase pcsCase, ClaimEntity claim) {
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

        if (claim.getClaimantType() != null) {
            pcsCase.setClaimantType(DynamicStringList.builder()
                .value(DynamicStringListElement.builder().code(claim.getClaimantType().name())
                           .label(claim.getClaimantType().getLabel())
                           .build())
                .build());
        }

    }

    private void setClaimFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (!pcsCaseEntity.getClaims().isEmpty()) {
            ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
            mapBasicClaimFields(pcsCase, mainClaim);
            mapComplexClaimFields(pcsCase, mainClaim);
        }
    }



}
