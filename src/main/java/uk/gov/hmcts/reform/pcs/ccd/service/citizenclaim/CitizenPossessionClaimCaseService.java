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
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenCreatePossessionClaim.SERVICE_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.CASE_ACCESS_DENIED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenMakePossessionClaim;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenPossessionClaimCaseService {

    public static final String PROPERTY_ADDRESS_REQUIRED = "PROPERTY_ADDRESS_REQUIRED";
    public static final String PROPERTY_NOT_IN_ENGLAND_OR_WALES = "PROPERTY_NOT_IN_ENGLAND_OR_WALES";
    public static final String LEGISLATIVE_COUNTRY_REQUIRED = "LEGISLATIVE_COUNTRY_REQUIRED";
    public static final String POSTCODE_NOT_ASSIGNED_TO_COURT = "POSTCODE_NOT_ASSIGNED_TO_COURT";

    private static final Set<LegislativeCountry> CLAIMABLE_COUNTRIES =
        Set.of(LegislativeCountry.ENGLAND, LegislativeCountry.WALES);

    private final EligibilityService eligibilityService;
    private final PcsCaseRepository pcsCaseRepository;
    private final PcsCaseService pcsCaseService;
    private final AddressMapper addressMapper;
    private final DraftCaseDataService draftCaseDataService;
    private final SecurityContextService securityContextService;
    private final FeatureToggleService featureToggleService;

    /**
     * The outcome of {@link #accessAsClaimCreator}: the case, or the error code that refuses the event.
     */
    public record ClaimCreatorAccess(PcsCaseEntity pcsCase, String error) {

        public static ClaimCreatorAccess refused(String error) {
            return new ClaimCreatorAccess(null, error);
        }

        public boolean isRefused() {
            return error != null;
        }
    }


    /**
     * Checks the property is in England or Wales, from the country its postcode's court mapping gives. Whether
     * that court takes claims through the service yet is not checked: citizen claims are not limited to it.
     *
     * @param chosenCountry the country the claimant chose for a cross-border postcode, if they were asked
     */
    public CitizenPropertyCheck checkProperty(AddressUK propertyAddress, LegislativeCountry chosenCountry) {
        if (propertyAddress == null || StringUtils.isBlank(propertyAddress.getPostCode())) {
            return CitizenPropertyCheck.refused(PROPERTY_ADDRESS_REQUIRED);
        }
        return checkPostcode(propertyAddress.getPostCode(), chosenCountry);
    }

    /**
     * The postcode half of {@link #checkProperty}, shared with the property-eligibility endpoint so the
     * frontend can check a postcode before the case is created.
     */
    public CitizenPropertyCheck checkPostcode(String postcode, LegislativeCountry chosenCountry) {
        EligibilityResult result = eligibilityService.checkEligibility(postcode, null);

        if (result.getStatus() == EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED) {
            List<LegislativeCountry> countries = Optional.ofNullable(result.getLegislativeCountries())
                .orElse(List.of());
            if (countries.stream().noneMatch(CLAIMABLE_COUNTRIES::contains)) {
                return CitizenPropertyCheck.refused(PROPERTY_NOT_IN_ENGLAND_OR_WALES);
            }
            if (chosenCountry == null) {
                return CitizenPropertyCheck.refused(LEGISLATIVE_COUNTRY_REQUIRED);
            }
            if (!CLAIMABLE_COUNTRIES.contains(chosenCountry) || !countries.contains(chosenCountry)) {
                return CitizenPropertyCheck.refused(PROPERTY_NOT_IN_ENGLAND_OR_WALES);
            }
            result = eligibilityService.checkEligibility(postcode, chosenCountry);
        }

        return switch (result.getStatus()) {
            case ELIGIBLE, NOT_ELIGIBLE -> CLAIMABLE_COUNTRIES.contains(result.getLegislativeCountry())
                ? CitizenPropertyCheck.accepted(result.getLegislativeCountry())
                : CitizenPropertyCheck.refused(PROPERTY_NOT_IN_ENGLAND_OR_WALES);
            // Without a court mapping there is no court to issue at, and no country to say the property is in
            case NO_MATCH_FOUND, MULTIPLE_MATCHES_FOUND -> CitizenPropertyCheck.refused(POSTCODE_NOT_ASSIGNED_TO_COURT);
            case LEGISLATIVE_COUNTRY_REQUIRED -> CitizenPropertyCheck.refused(LEGISLATIVE_COUNTRY_REQUIRED);
        };
    }

    /**
     * Creates the shell case for a citizen claimant. Unlike the ExUI claim there is no organisation:
     * the citizen reaches the case through the [CREATOR] case role CCD grants on creation.
     */
    public void createCase(long caseReference, AddressUK propertyAddress, CitizenClaimEligibility eligibility,
                           LegislativeCountry legislativeCountry) {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setPropertyAddress(addressMapper.toAddressEntityAndNormalise(propertyAddress));
        pcsCaseEntity.setLegislativeCountry(legislativeCountry);

        PartyEntity claimantParty = new PartyEntity();
        claimantParty.setRemoved(false);
        claimantParty.setIdamId(securityContextService.getCurrentUserId());
        claimantParty.setClaimCreator(true);
        pcsCaseEntity.addParty(claimantParty);

        pcsCaseRepository.save(pcsCaseEntity);

        PCSCase draft = PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaim.builder()
                                        .propertyAddress(propertyAddress)
                                        .eligibility(withCountry(eligibility, legislativeCountry))
                                        .build())
            .build();
        draftCaseDataService.patchUnsubmittedEventData(caseReference, draft, citizenMakePossessionClaim);
    }

    /**
     * The claim with the case's legislative country as its eligibility country, which decides the questions that
     * apply. Only the case's country counts, whatever a saved draft says.
     */
    public static CitizenPossessionClaim withCaseCountry(CitizenPossessionClaim claim, PcsCaseEntity pcsCaseEntity) {
        claim.setEligibility(withCountry(claim.getEligibility(), pcsCaseEntity.getLegislativeCountry()));
        return claim;
    }

    private static CitizenClaimEligibility withCountry(CitizenClaimEligibility eligibility,
                                                       LegislativeCountry legislativeCountry) {
        return Optional.ofNullable(eligibility)
            .map(CitizenClaimEligibility::toBuilder)
            .orElseGet(CitizenClaimEligibility::builder)
            .legislativeCountry(legislativeCountry)
            .build();
    }

    /**
     * The case a citizen claim event acts on, or the error to refuse it with: the journey is switched off, or the
     * current user did not create the claim.
     */
    public ClaimCreatorAccess accessAsClaimCreator(long caseReference) {
        if (!featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)) {
            return ClaimCreatorAccess.refused(SERVICE_NOT_AVAILABLE);
        }
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        if (!isCurrentUserClaimCreator(pcsCaseEntity)) {
            return ClaimCreatorAccess.refused(CASE_ACCESS_DENIED);
        }
        return new ClaimCreatorAccess(pcsCaseEntity, null);
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
     * The saved draft, with the property address and country the case was created with, creating an empty
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
        return withCaseCountry(draft, pcsCaseEntity);
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
