package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.PartyDocumentDto;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private final PcsCaseService pcsCaseService;
    private final UnsubmittedCaseDataService unsubmittedCaseDataService;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseReference The CCD case reference to load
     * @param state the current case state
     */
    @Override
    public PCSCase getCase(long caseReference, String state) {
        PCSCase pcsCase = getSubmittedCase(caseReference);

        boolean hasUnsubmittedCaseData = caseHasUnsubmittedData(caseReference, state);
        pcsCase.setHasUnsubmittedCaseData(YesOrNo.from(hasUnsubmittedCaseData));

        setMarkdownFields(pcsCase);

        return pcsCase;
    }

    private boolean caseHasUnsubmittedData(long caseReference, String state) {
        if (State.AWAITING_FURTHER_CLAIM_DETAILS.name().equals(state)) {
            return unsubmittedCaseDataService.hasUnsubmittedCaseData(caseReference);
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
            .claimantType(pcsCaseEntity.getClaimantType() != null 
                ? DynamicStringList.builder()
                    .value(DynamicStringListElement.builder()
                        .code(pcsCaseEntity.getClaimantType().name())
                        .label(pcsCaseEntity.getClaimantType().getLabel())
                        .build())
                    .build() 
                : null)
            .preActionProtocolCompleted(pcsCaseEntity.getPreActionProtocolCompleted() != null
                ? VerticalYesNo.from(pcsCaseEntity.getPreActionProtocolCompleted())
                : null)
            .currentRent(pcsCaseEntity.getTenancyLicence() != null
                && pcsCaseEntity.getTenancyLicence().getRentAmount() != null
                ? poundsToPence(pcsCaseEntity.getTenancyLicence().getRentAmount()) : null)
            .rentFrequency(pcsCaseEntity.getTenancyLicence() != null
                ? pcsCaseEntity.getTenancyLicence().getRentPaymentFrequency() : null)
            .otherRentFrequency(pcsCaseEntity.getTenancyLicence() != null
                ? pcsCaseEntity.getTenancyLicence().getOtherRentFrequency() : null)
            .dailyRentChargeAmount(pcsCaseEntity.getTenancyLicence() != null
                && pcsCaseEntity.getTenancyLicence().getDailyRentChargeAmount() != null
                ? poundsToPence(pcsCaseEntity.getTenancyLicence().getDailyRentChargeAmount()) : null)
            .noticeServed(pcsCaseEntity.getTenancyLicence() != null
                && pcsCaseEntity.getTenancyLicence().getNoticeServed() != null
                ? YesOrNo.from(pcsCaseEntity.getTenancyLicence().getNoticeServed()) : null)
            .defendants(pcsCaseService.mapToDefendantDetails(pcsCaseEntity.getDefendants()))
            .citizenDocuments(mapCitizenDocumentsToListValue(pcsCaseEntity.getCitizenDocuments()))
            .build();

        setDerivedProperties(pcsCase, pcsCaseEntity);

        return pcsCase;
    }

    private void setDerivedProperties(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        boolean pcqIdSet = findPartyForCurrentUser(pcsCaseEntity)
            .map(party -> party.getPcqId() != null)
            .orElse(false);

        pcsCase.setUserPcqIdSet(YesOrNo.from(pcqIdSet));

        pcsCase.setParties(mapAndWrapParties(pcsCaseEntity.getParties()));
    }

    private void setMarkdownFields(PCSCase pcsCase) {
        pcsCase.setPageHeadingMarkdown("""
                <p class="govuk-!-font-size-24
                govuk-!-margin-top-0 govuk-!-margin-bottom-0">
                Case number: ${[CASE_REFERENCE]}</p>""");

        if (pcsCase.getHasUnsubmittedCaseData() == YesOrNo.YES) {
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
                                             """.formatted(EventId.resumePossessionClaim));
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
                                             """.formatted(EventId.resumePossessionClaim));
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

    private List<ListValue<AdditionalDocument>> mapCitizenDocumentsToListValue(
        List<PartyDocumentDto> citizenDocuments) {
        if (citizenDocuments == null || citizenDocuments.isEmpty()) {
            return Collections.emptyList();
        }

        return citizenDocuments.stream()
            .map(dto -> AdditionalDocument.builder()
                .documentType(dto.getDocumentType())
                .document(dto.getDocument())
                .description(dto.getDescription())
                .build())
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                ListValueUtils::wrapListItems
            ));
    }

    private static String poundsToPence(BigDecimal pounds) {
        return pounds.movePointRight(2).toPlainString();
    }
}
