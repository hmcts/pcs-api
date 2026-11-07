package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenMakePossessionClaim;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenPossessionClaimCaseService {

    public static final String PROPERTY_ADDRESS_REQUIRED = "PROPERTY_ADDRESS_REQUIRED";
    public static final String PROPERTY_NOT_IN_ENGLAND = "PROPERTY_NOT_IN_ENGLAND";
    public static final String LEGISLATIVE_COUNTRY_REQUIRED = "LEGISLATIVE_COUNTRY_REQUIRED";
    public static final String POSTCODE_NOT_ASSIGNED_TO_COURT = "POSTCODE_NOT_ASSIGNED_TO_COURT";
    public static final String PROPERTY_NOT_ELIGIBLE = "PROPERTY_NOT_ELIGIBLE";

    private final EligibilityService eligibilityService;
    private final PcsCaseRepository pcsCaseRepository;
    private final PcsCaseService pcsCaseService;
    private final AddressMapper addressMapper;
    private final DraftCaseDataService draftCaseDataService;
    private final SecurityContextService securityContextService;

    /**
     * Checks the property is in England and served by a live court, the same way the ExUI
     * claim's address page does.
     *
     * @return the refusal code, if the property cannot be claimed for on this journey
     */
    public Optional<String> checkPropertyInEngland(AddressUK propertyAddress, LegislativeCountry chosenCountry) {
        if (propertyAddress == null || StringUtils.isBlank(propertyAddress.getPostCode())) {
            return Optional.of(PROPERTY_ADDRESS_REQUIRED);
        }

        String postcode = propertyAddress.getPostCode();
        EligibilityResult result = eligibilityService.checkEligibility(postcode, null);

        if (result.getStatus() == EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED) {
            List<LegislativeCountry> countries = result.getLegislativeCountries();
            if (countries == null || !countries.contains(LegislativeCountry.ENGLAND)) {
                return Optional.of(PROPERTY_NOT_IN_ENGLAND);
            }
            if (chosenCountry == null) {
                return Optional.of(LEGISLATIVE_COUNTRY_REQUIRED);
            }
            if (chosenCountry != LegislativeCountry.ENGLAND) {
                return Optional.of(PROPERTY_NOT_IN_ENGLAND);
            }
            result = eligibilityService.checkEligibility(postcode, LegislativeCountry.ENGLAND);
        }

        return switch (result.getStatus()) {
            case ELIGIBLE -> result.getLegislativeCountry() == LegislativeCountry.ENGLAND
                ? Optional.empty() : Optional.of(PROPERTY_NOT_IN_ENGLAND);
            case NOT_ELIGIBLE -> result.getLegislativeCountry() == LegislativeCountry.ENGLAND
                ? Optional.of(PROPERTY_NOT_ELIGIBLE) : Optional.of(PROPERTY_NOT_IN_ENGLAND);
            case NO_MATCH_FOUND, MULTIPLE_MATCHES_FOUND -> Optional.of(POSTCODE_NOT_ASSIGNED_TO_COURT);
            case LEGISLATIVE_COUNTRY_REQUIRED -> Optional.of(LEGISLATIVE_COUNTRY_REQUIRED);
        };
    }

    /**
     * Creates the shell case for a citizen claimant. Unlike the ExUI claim there is no organisation:
     * the citizen reaches the case through the [CREATOR] case role CCD grants on creation.
     */
    public void createCase(long caseReference, AddressUK propertyAddress, CitizenClaimEligibility eligibility) {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setPropertyAddress(addressMapper.toAddressEntityAndNormalise(propertyAddress));
        pcsCaseEntity.setLegislativeCountry(LegislativeCountry.ENGLAND);

        PartyEntity claimantParty = new PartyEntity();
        claimantParty.setRemoved(false);
        claimantParty.setIdamId(securityContextService.getCurrentUserId());
        claimantParty.setClaimCreator(true);
        pcsCaseEntity.addParty(claimantParty);

        pcsCaseRepository.save(pcsCaseEntity);

        PCSCase draft = PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaim.builder()
                                        .propertyAddress(propertyAddress)
                                        .eligibility(eligibility)
                                        .build())
            .build();
        draftCaseDataService.patchUnsubmittedEventData(caseReference, draft, citizenMakePossessionClaim);
    }

    public PcsCaseEntity loadCase(long caseReference) {
        return pcsCaseService.loadCase(caseReference);
    }

    public boolean isCurrentUserClaimCreator(PcsCaseEntity pcsCaseEntity) {
        String currentUserId = securityContextService.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        return pcsCaseEntity.getParties().stream()
            .filter(PartyEntity::isClaimCreator)
            .map(PartyEntity::getIdamId)
            .filter(Objects::nonNull)
            .anyMatch(currentUserId::equals);
    }

    /**
     * The saved draft, with the property address the case was created with, creating an empty
     * draft first if there is none so that the first save has something to replace.
     */
    public CitizenPossessionClaim loadOrInitialiseDraft(PcsCaseEntity pcsCaseEntity) {
        long caseReference = pcsCaseEntity.getCaseReference();
        CitizenPossessionClaim draft = loadDraft(caseReference).orElseGet(() -> {
            draftCaseDataService.patchUnsubmittedEventData(
                caseReference,
                PCSCase.builder().citizenPossessionClaim(CitizenPossessionClaim.builder().build()).build(),
                citizenMakePossessionClaim);
            return loadDraft(caseReference).orElseGet(() -> CitizenPossessionClaim.builder().build());
        });
        draft.setPropertyAddress(caseAddress(pcsCaseEntity));
        return draft;
    }

    public Optional<CitizenPossessionClaim> loadDraft(long caseReference) {
        return draftCaseDataService.getUnsubmittedCaseData(caseReference, citizenMakePossessionClaim)
            .map(PCSCase::getCitizenPossessionClaim);
    }

    /**
     * Replaces the draft with the given answers, minus what only pcs-api may set. When the answers
     * carry a draft version the save is refused if the stored draft has moved on; without one the
     * last write wins.
     *
     * @return the version the draft is stored at once this save is written, which the next save or
     *     the submit must echo
     * @throws uk.gov.hmcts.reform.pcs.exception.DraftVersionConflictException if the draft version is stale
     */
    public Long saveDraft(long caseReference, CitizenPossessionClaim claim) {
        Long expectedVersion = claim.getDraftVersion();
        PCSCase draft = PCSCase.builder().citizenPossessionClaim(withoutServerOwnedFields(claim)).build();
        if (draftCaseDataService.hasUnsubmittedCaseData(caseReference, citizenMakePossessionClaim)) {
            return draftCaseDataService.saveUnsubmittedEventData(
                caseReference, draft, citizenMakePossessionClaim, expectedVersion);
        }
        draftCaseDataService.patchUnsubmittedEventData(caseReference, draft, citizenMakePossessionClaim);
        return loadDraft(caseReference).map(CitizenPossessionClaim::getDraftVersion).orElse(null);
    }

    public AddressUK caseAddress(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getPropertyAddress() != null
            ? addressMapper.toAddressUK(pcsCaseEntity.getPropertyAddress()) : null;
    }

    public static CitizenPossessionClaim withoutServerOwnedFields(CitizenPossessionClaim claim) {
        CitizenClaimSubmission submission = claim.getSubmission();
        return claim.toBuilder()
            .propertyAddress(null)
            .draftVersion(null)
            .submission(submission == null ? null : submission.toBuilder()
                .serviceRequestReference(null)
                .feeAmount(null)
                .paymentStatus(null)
                .build())
            .build();
    }

    public void deleteDraft(long caseReference) {
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, citizenMakePossessionClaim);
    }

}
