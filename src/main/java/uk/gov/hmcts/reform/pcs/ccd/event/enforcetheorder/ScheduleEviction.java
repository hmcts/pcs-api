package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalEventId;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartResponse;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionStart;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.ScheduledEvictionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.ScheduledEvictionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
// import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.CASE_PROGRESSION;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * A bailiff schedules the eviction for a warrant of possession.
 */
@Component
public class ScheduleEviction implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<ScheduleEvictionStart, ScheduleEvictionRequest> SCHEDULE_EVICTION =
        ExternalEventId.of("ext:scheduleEviction", ScheduleEvictionStart.class, ScheduleEvictionRequest.class);

    static final String NOT_AVAILABLE = "Scheduling evictions is not available";
    static final String NO_WARRANT_AWAITING_SCHEDULING = "There is no warrant awaiting scheduling on this case";
    static final String DATE_REQUIRED = "Enter the date and time of the eviction";
    static final String DISPENSE_WITH_SERVICE_REQUIRED =
        "Select whether the court has given permission to dispense with service";

    private final PcsCaseService pcsCaseService;
    private final ScheduledEvictionRepository scheduledEvictionRepository;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final RecipientAddressResolver recipientAddressResolver;
    private final FeatureToggleService featureToggleService;

    public ScheduleEviction(PcsCaseService pcsCaseService,
                            ScheduledEvictionRepository scheduledEvictionRepository,
                            CaseReferenceFormatter caseReferenceFormatter,
                            CaseNameFormatter caseNameFormatter,
                            AddressMapper addressMapper,
                            AddressFormatter addressFormatter,
                            RecipientAddressResolver recipientAddressResolver,
                            FeatureToggleService featureToggleService) {
        this.pcsCaseService = pcsCaseService;
        this.scheduledEvictionRepository = scheduledEvictionRepository;
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.caseNameFormatter = caseNameFormatter;
        this.addressMapper = addressMapper;
        this.addressFormatter = addressFormatter;
        this.recipientAddressResolver = recipientAddressResolver;
        this.featureToggleService = featureToggleService;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .externalEvent(SCHEDULE_EVICTION, this::submit)
            .forState(State.CASE_ISSUED)
            .name("Schedule eviction")
            .showCondition(ShowConditions.featureFlagsEnabled(FeatureFlag.RELEASE_1B))
            // TODO HDPI-7344: grant the bailiff roles once they are pulled in
            .grant(Permission.CRUD, CASEWORKER_ROLES)
            // TODO HDPI-7344: temporary, to test locally as pcs-solicitor1 until the bailiff roles are in
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
            .grant(Set.of(Permission.R), JUDICIAL_HISTORY_ROLES)
            .onStart(this::start);
    }

    private ExternalStartResponse<ScheduleEvictionStart> start(ExternalStartRequest start) {
        requireRelease1b();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(start.caseReference());
        EnforcementOrderEntity warrantApplication = warrantAwaitingScheduling(pcsCase);
        ClaimEntity mainClaim = pcsCase.getMainClaim();

        return ExternalStartResponse.started(new ScheduleEvictionStart(
            warrantApplication.getId(),
            caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()),
            addressFormatter.formatShortAddress(addressMapper.toAddressUK(pcsCase.getPropertyAddress()),
                                                AddressFormatter.COMMA_DELIMITER),
            caseNameFormatter.formatCaseName(pcsCase),
            mainClaim.getClaimantType() == null ? null : mainClaim.getClaimantType().getLabel(),
            partyNames(mainClaim, PartyRole.CLAIMANT),
            partyNames(mainClaim, PartyRole.DEFENDANT)
        ));
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<ScheduleEvictionRequest> submit) {
        requireRelease1b();
        ScheduleEvictionRequest request = submit.payload();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(submit.caseReference());
        EnforcementOrderEntity warrantApplication = warrantAwaitingScheduling(pcsCase);
        if (!warrantApplication.getId().equals(request.enforcementApplicationId())) {
            throw ExternalRejection.because(NO_WARRANT_AWAITING_SCHEDULING);
        }
        LocalDateTime evictionDate = validEvictionDate(request.scheduledEvictionDate());
        if (request.courtGivenPermissionToDispenseWithService() == null) {
            throw ExternalRejection.because(DISPENSE_WITH_SERVICE_REQUIRED);
        }

        scheduledEvictionRepository.save(ScheduledEvictionEntity.builder()
            .enfWarrant(warrantApplication.getWarrantDetails())
            .courtGivenPermissionToDispenseWithService(request.courtGivenPermissionToDispenseWithService())
            .scheduledEvictionDate(evictionDate.atZone(UK_ZONE_ID).toInstant())
            .build());

        return ExternalSubmitResponse.accepted("Eviction scheduled", "Scheduled the eviction");
    }

    /**
     * The warrant awaiting scheduling on the case. A change is coming to limit a case to one active warrant at a
     * time; until then a case can have more than one, so this uses the latest.
     */
    private EnforcementOrderEntity warrantAwaitingScheduling(PcsCaseEntity pcsCase) {
        return pcsCase.getEnforcementOrders().stream()
            // TODO HDPI-7344: back to AWAITING_SCHEDULING once something moves a warrant into it
            // .filter(application -> application.getState() == AWAITING_SCHEDULING)
            .filter(application -> application.getState() == CASE_PROGRESSION)
            .filter(application -> application.getWarrantDetails() != null)
            .max(Comparator.comparing(application -> application.getWarrantDetails().getCreated()))
            .orElseThrow(() -> ExternalRejection.because(NO_WARRANT_AWAITING_SCHEDULING));
    }

    private LocalDateTime validEvictionDate(LocalDateTime evictionDate) {
        if (evictionDate == null) {
            throw ExternalRejection.because(DATE_REQUIRED);
        }
        return evictionDate;
    }

    private List<String> partyNames(ClaimEntity claim, PartyRole role) {
        return partiesByRole(claim, role).stream()
            .map(recipientAddressResolver::resolveDisplayName)
            .toList();
    }

    private void requireRelease1b() {
        if (!featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)) {
            throw ExternalRejection.because(NOT_AVAILABLE);
        }
    }
}
