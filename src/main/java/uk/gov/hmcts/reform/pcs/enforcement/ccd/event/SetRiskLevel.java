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
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.risklevel.SetRiskLevelConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevelDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.setRiskLevel;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.BailiffStaffRoles.BAILIFF_STAFF_ROLES;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

/**
 * Bailiff staff set the eviction risk level of the claim's current warrant (HDPI-7343). The allocated
 * bailiff's allocated-bailiff case role maps to the bailiff access profile, so it is covered by the bailiff
 * grant. The current level is pre-selected when the level is reset.
 */
@Component
@AllArgsConstructor
public class SetRiskLevel implements CCDConfig<PCSCase, State, UserRole> {

    static final int MAX_REASON_LENGTH = RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;
    static final String RISK_LEVEL_REQUIRED_ERROR = "Choose a risk level";
    static final String REASON_REQUIRED_ERROR = "Enter why you set the risk level to high";
    static final String REASON_TOO_LONG_ERROR = "The reason must be 6,800 characters or fewer";

    private final SetRiskLevelConfigurer setRiskLevelConfigurer;
    private final RiskLevelService riskLevelService;
    private final CaseReferenceFormatter caseReferenceFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(setRiskLevel.name(), this::submit, this::start)
                .forStates(AddBailiffNote.STATES)
                .name("Set risk level")
                .grant(Permission.CRUD, BAILIFF_STAFF_ROLES)
                .showCondition(featureFlagsEnabled(ENFORCEMENT))
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .endButtonLabel("Continue");
        setRiskLevelConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        EnforcementOrderEntity enforcement = riskLevelService.getEnforcementForRiskLevel(eventPayload.caseReference());
        PCSCase pcsCase = eventPayload.caseData();
        RiskLevelDetails riskLevelDetails = riskLevelDetailsOf(pcsCase);
        riskLevelDetails.setRiskLevel(enforcement.getRiskLevel());
        riskLevelDetails.setRiskLevelReason(enforcement.getRiskLevelReason());
        return pcsCase;
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        EnforcementOrder enforcementOrder = eventPayload.caseData().getEnforcementOrder();
        RiskLevelDetails details = enforcementOrder == null ? null : enforcementOrder.getRiskLevelDetails();
        RiskLevel riskLevel = details == null ? null : details.getRiskLevel();
        String reason = details == null ? null : details.getRiskLevelReason();

        if (riskLevel == null) {
            return errorResponse(RISK_LEVEL_REQUIRED_ERROR);
        }
        if (riskLevel == RiskLevel.HIGH) {
            if (reason == null || reason.isBlank()) {
                return errorResponse(REASON_REQUIRED_ERROR);
            }
            if (reason.length() > MAX_REASON_LENGTH) {
                return errorResponse(REASON_TOO_LONG_ERROR);
            }
        }
        try {
            riskLevelService.setRiskLevel(caseReference, riskLevel, reason);
        } catch (NoEnforcementException | EnforcementEventNotAvailableException ex) {
            return errorResponse(ex.getMessage());
        }
        return SubmitResponse.<State>builder()
            .confirmationHeader("# Risk level set to " + riskLevel.getLabel())
            .confirmationBody("Case number: " + caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference))
            .build();
    }

    private static RiskLevelDetails riskLevelDetailsOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        if (enforcementOrder.getRiskLevelDetails() == null) {
            enforcementOrder.setRiskLevelDetails(new RiskLevelDetails());
        }
        return enforcementOrder.getRiskLevelDetails();
    }

    private static SubmitResponse<State> errorResponse(String error) {
        return SubmitResponse.<State>builder().errors(List.of(error)).build();
    }
}
