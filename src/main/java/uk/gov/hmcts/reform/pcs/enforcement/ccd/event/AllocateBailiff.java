package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.allocatebailiff.AllocateBailiffConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.allocatebailiff.FindBailiffPage;
import uk.gov.hmcts.reform.pcs.enforcement.domain.BailiffAllocationDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffDirectoryService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.allocateBailiff;
import static uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.BailiffAllocationRoles.BAILIFF_ALLOCATION_ROLES;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

/**
 * Bailiff staff allocate the claim's current warrant to a bailiff (HDPI-7356, HDPI-8183, HDPI-8189). It is
 * offered for a warrant awaiting bailiff allocation, and to reallocate a warrant while it is live.
 *
 * <p>The event is gated on the feature flag only: the enforcement status is checked when it starts, because
 * the status is held on the enforcement record rather than on the case.
 */
@Component
@AllArgsConstructor
public class AllocateBailiff implements CCDConfig<PCSCase, State, UserRole> {

    private final AllocateBailiffConfigurer allocateBailiffConfigurer;
    private final BailiffAllocationService bailiffAllocationService;
    private final BailiffDirectoryService bailiffDirectoryService;
    private final CaseNameFormatter caseNameFormatter;
    private final CaseReferenceFormatter caseReferenceFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(allocateBailiff.name(), this::submit, this::start)
                .forStates(AddBailiffNote.STATES)
                .name("Allocate case to bailiff")
                .grant(Permission.CRUD, BAILIFF_ALLOCATION_ROLES)
                .showCondition(featureFlagsEnabled(ENFORCEMENT))
                .grantHistoryOnly(UserRole.HEARING_CENTRE_ADMIN, UserRole.HEARING_CENTRE_TEAM_LEADER)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary();
        allocateBailiffConfigurer.configurePages(new PageBuilder(eventBuilder));
        SummaryFields.excludeNeverShownFields(eventBuilder);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        bailiffAllocationService.getAllocatableEnforcement(eventPayload.caseReference());
        PCSCase pcsCase = eventPayload.caseData();
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        pcsCase.getEnforcementOrder().setBailiffAllocationDetails(
            BailiffAllocationDetails.builder()
                .bailiffAllocationCaseName(caseNameFormatter.formatCaseName(pcsCase))
                .build());
        return pcsCase;
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        EnforcementOrder enforcementOrder = eventPayload.caseData().getEnforcementOrder();
        String userId = FindBailiffPage.chosenUserId(
            enforcementOrder == null ? null : enforcementOrder.getBailiffAllocationDetails());
        if (userId == null) {
            return errorResponse(FindBailiffPage.CHOOSE_A_BAILIFF_ERROR);
        }
        Optional<BailiffUser> bailiff = bailiffDirectoryService.findBailiff(userId);
        if (bailiff.isEmpty()) {
            return errorResponse(FindBailiffPage.NOT_A_BAILIFF_ERROR);
        }
        EnforcementChange<Void> allocated = bailiffAllocationService.allocate(caseReference, bailiff.get());
        if (allocated.refused()) {
            return errorResponse(allocated.errors());
        }
        return SubmitResponse.<State>builder()
            .confirmationHeader("# Case assigned to " + displayName(bailiff.get()))
            .confirmationBody("Case number: " + caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference))
            .build();
    }

    private static String displayName(BailiffUser bailiff) {
        return bailiff.name() == null || bailiff.name().isBlank() ? bailiff.email() : bailiff.name();
    }

    private static SubmitResponse<State> errorResponse(String error) {
        return errorResponse(List.of(error));
    }

    private static SubmitResponse<State> errorResponse(List<String> errors) {
        return SubmitResponse.<State>builder().errors(errors).build();
    }
}
