package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionStart;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.ScheduledEvictionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.ScheduleEvictionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.AWAITING_PROPERTY_VISIT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@Service
@AllArgsConstructor
public class ScheduleEvictionService {

    public static final String NOT_AVAILABLE = "Scheduling evictions is not available";
    public static final String NO_WARRANT_AWAITING_SCHEDULING = "There is no warrant awaiting scheduling on this case";
    public static final String WARRANT_NO_LONGER_AWAITING_SCHEDULING = "This warrant is no longer awaiting scheduling";
    public static final String DATE_REQUIRED = "Enter the date and time of the eviction";
    public static final String DISPENSE_WITH_SERVICE_REQUIRED =
        "Select whether the court has given permission to dispense with service";

    private final PcsCaseService pcsCaseService;
    private final ScheduleEvictionRepository scheduleEvictionRepository;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final RecipientAddressResolver recipientAddressResolver;
    private final EnforcementOrderMediator enforcementOrderMediator;
    private final FeatureToggleService featureToggleService;

    /**
     * The case and warrant details the bailiff sees when they start scheduling the eviction.
     */
    public ScheduleEvictionStart getStartDetails(long caseReference) {
        requireEvictionScheduling();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        EnforcementOrderEntity warrantApplication = warrantAwaitingScheduling(pcsCase);
        ClaimEntity mainClaim = pcsCase.getMainClaim();

        return new ScheduleEvictionStart(
            warrantApplication.getId(),
            caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()),
            addressFormatter.formatShortAddress(addressMapper.toAddressUK(pcsCase.getPropertyAddress()),
                                                AddressFormatter.COMMA_DELIMITER),
            caseNameFormatter.formatCaseName(pcsCase),
            mainClaim.getClaimantType() == null ? null : mainClaim.getClaimantType().getLabel(),
            partyNames(mainClaim, PartyRole.CLAIMANT),
            partyNames(mainClaim, PartyRole.DEFENDANT)
        );
    }

    /**
     * Moves the warrant on to awaiting the property visit and saves the eviction against it.
     */
    public void scheduleEviction(long caseReference, ScheduleEvictionRequest request) {
        requireEvictionScheduling();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        EnforcementOrderEntity enfApplication = warrantAwaitingScheduling(pcsCase);
        if (!enfApplication.getId().equals(request.enforcementApplicationId())) {
            throw ExternalRejection.because(WARRANT_NO_LONGER_AWAITING_SCHEDULING);
        }
        if (request.scheduledEvictionDate() == null) {
            throw ExternalRejection.because(DATE_REQUIRED);
        }
        if (request.courtGivenPermissionToDispenseWithService() == null) {
            throw ExternalRejection.because(DISPENSE_WITH_SERVICE_REQUIRED);
        }

        enfApplication.setState(AWAITING_PROPERTY_VISIT);
        scheduleEvictionRepository.save(ScheduledEvictionEntity.builder()
            .enfWarrant(enfApplication.getWarrantDetails())
            .courtGivenPermissionToDispenseWithService(request.courtGivenPermissionToDispenseWithService())
            .scheduledEvictionDate(request.scheduledEvictionDate().atZone(UK_ZONE_ID).toInstant())
            .build());
    }

    /**
     * The active enforcement application on the case, when it is a warrant awaiting scheduling.
     */
    private EnforcementOrderEntity warrantAwaitingScheduling(PcsCaseEntity pcsCase) {
        return enforcementOrderMediator.getEnforcementOrder(pcsCase)
            .filter(application -> application.getState() == AWAITING_SCHEDULING)
            .filter(application -> application.getWarrantDetails() != null)
            .orElseThrow(() -> ExternalRejection.because(NO_WARRANT_AWAITING_SCHEDULING));
    }

    private void requireEvictionScheduling() {
        if (!featureToggleService.isEnabled(FeatureFlag.EVICTION_SCHEDULING)) {
            throw ExternalRejection.because(NOT_AVAILABLE);
        }
    }

    private List<String> partyNames(ClaimEntity claim, PartyRole role) {
        return partiesByRole(claim, role).stream()
            .map(recipientAddressResolver::resolveDisplayName)
            .toList();
    }
}
