package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CaseView;
import uk.gov.hmcts.ccd.sdk.CaseViewRequest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseTitleService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.ccd.view.AlternativesToPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.AsbProhibitedConductView;
import uk.gov.hmcts.reform.pcs.ccd.view.ClaimGroundsView;
import uk.gov.hmcts.reform.pcs.ccd.view.HousingActWalesView;
import uk.gov.hmcts.reform.pcs.ccd.view.NoticeOfPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentArrearsView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentDetailsView;
import uk.gov.hmcts.reform.pcs.ccd.view.StatementOfTruthView;
import uk.gov.hmcts.reform.pcs.ccd.view.TenancyLicenceView;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

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
    private final TenancyLicenceView tenancyLicenceView;
    private final ClaimGroundsView claimGroundsView;
    private final RentDetailsView rentDetailsView;
    private final AlternativesToPossessionView alternativesToPossessionView;
    private final HousingActWalesView housingActWalesView;
    private final AsbProhibitedConductView asbProhibitedConductView;
    private final RentArrearsView rentArrearsView;
    private final NoticeOfPossessionView noticeOfPossessionView;
    private final StatementOfTruthView statementOfTruthView;


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

        Map<PartyRole, List<ListValue<Party>>> partyMap = getPartyMap(pcsCaseEntity);

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(convertAddress(pcsCaseEntity.getPropertyAddress()))
            .legislativeCountry(pcsCaseEntity.getLegislativeCountry())
            .caseManagementLocation(pcsCaseEntity.getCaseManagementLocation())
            .allClaimants(partyMap.get(PartyRole.CLAIMANT))
            .allDefendants(partyMap.get(PartyRole.DEFENDANT))
            .allUnderlesseeOrMortgagees(partyMap.get(PartyRole.UNDERLESSEE_OR_MORTGAGEE))
            .allDocuments(mapAndWrapDocuments(pcsCaseEntity))
            .build();

        setDerivedProperties(pcsCase, pcsCaseEntity);
        setClaimFields(pcsCase, pcsCaseEntity);

        tenancyLicenceView.setCaseFields(pcsCase, pcsCaseEntity);
        claimGroundsView.setCaseFields(pcsCase, pcsCaseEntity);
        rentDetailsView.setCaseFields(pcsCase, pcsCaseEntity);
        alternativesToPossessionView.setCaseFields(pcsCase, pcsCaseEntity);
        housingActWalesView.setCaseFields(pcsCase, pcsCaseEntity);
        asbProhibitedConductView.setCaseFields(pcsCase, pcsCaseEntity);

        rentArrearsView.setCaseFields(pcsCase, pcsCaseEntity);
        noticeOfPossessionView.setCaseFields(pcsCase, pcsCaseEntity);
        statementOfTruthView.setCaseFields(pcsCase, pcsCaseEntity);

        return pcsCase;
    }

    private Map<PartyRole, List<ListValue<Party>>> getPartyMap(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();

        if (claims.isEmpty()) {
            return Map.of();
        }

        ClaimEntity mainClaim = claims.getFirst();
        return mainClaim.getClaimParties().stream()
            .collect(Collectors.groupingBy(
                ClaimPartyEntity::getRole,
                Collectors.mapping(this::getPartyListValue, Collectors.toList())
            ));
    }

    private ListValue<Party> getPartyListValue(ClaimPartyEntity claimPartyEntity) {
        Party party = modelMapper.map(claimPartyEntity.getParty(), Party.class);

        return ListValue.<Party>builder()
            .id(claimPartyEntity.getId().getPartyId().toString())
            .value(party)
            .build();
    }

    private void setDerivedProperties(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        boolean pcqIdSet = findPartyForCurrentUser(pcsCaseEntity)
            .map(party -> party.getPcqId() != null)
            .orElse(false);

        pcsCase.setUserPcqIdSet(YesOrNo.from(pcqIdSet));

        pcsCase.setParties(mapAndWrapParties(pcsCaseEntity.getParties()));
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

    private List<ListValue<Document>> mapAndWrapDocuments(PcsCaseEntity pcsCaseEntity) {

        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return List.of();
        }

        return pcsCaseEntity.getDocuments().stream()
            .map(entity -> ListValue.<Document>builder()
                .id(entity.getId().toString())
                .value(Document.builder()
                           .filename(entity.getFileName())
                           .url(entity.getUrl())
                           .binaryUrl(entity.getBinaryUrl())
                           .categoryId(entity.getCategoryId())
                           .build())
                .build())
            .collect(Collectors.toList());
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
        pcsCase.setWantToUploadDocuments(claim.getAdditionalDocsProvided());
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
