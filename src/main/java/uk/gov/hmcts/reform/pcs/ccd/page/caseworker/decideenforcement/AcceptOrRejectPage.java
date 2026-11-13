package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.decideenforcement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RejectionReason;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AcceptReject.ACCEPT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AcceptReject.REJECT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnforcementDecision.NOT_ENFORCEABLE_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnforcementDecision.OTHER_REJECTION_REASON_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnforcementDecision.PARTY_CIRCUMSTANCES_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnforcementDecision.SET_ASIDE_OR_SUSPENDED_LABEL;

@Component
@RequiredArgsConstructor
public class AcceptOrRejectPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("acceptOrReject", this::midEvent)
            .pageLabel("Decide enforcement")
            .label("acceptOrReject-lineSeparator", "---")
            .complex(PCSCase::getEnforcementDecision)
            .readonly(EnforcementDecision::getWarrantId, NEVER_SHOW, true)
            .readonly(EnforcementDecision::getWritId, NEVER_SHOW, true)
            .mandatory(EnforcementDecision::getAcceptReject)
            .mandatory(EnforcementDecision::getRejectionReason, fieldEquals("enfDecision_AcceptReject", REJECT))
            .mandatory(EnforcementDecision::getChangeInPartyCircumstancesDetails,
                       fieldEquals("enfDecision_RejectionReason", RejectionReason.CHANGE_IN_PARTY_CIRCUMSTANCES))
            .mandatory(EnforcementDecision::getOrderNotEnforceableDetails,
                fieldEquals("enfDecision_RejectionReason", RejectionReason.ORDER_NOT_ENFORCEABLE))
            .mandatory(EnforcementDecision::getSetAsideOrSuspendedDetails,
                fieldEquals("enfDecision_RejectionReason", RejectionReason.ORDER_SET_ASIDE_OR_SUSPENDED))
            .mandatory(EnforcementDecision::getOtherRejectionDetails,
                fieldEquals("enfDecision_RejectionReason", RejectionReason.OTHER))
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        EnforcementDecision enforcementDecision = caseData.getEnforcementDecision();

        if (enforcementDecision.getAcceptReject() == ACCEPT) {
            return createDefaultResponse(caseData);
        }

        RejectionReason rejectionReason = enforcementDecision.getRejectionReason();

        return switch (rejectionReason) {
            case CHANGE_IN_PARTY_CIRCUMSTANCES -> createValidationResponse(caseData,
                enforcementDecision.getChangeInPartyCircumstancesDetails(), PARTY_CIRCUMSTANCES_LABEL);
            case ORDER_NOT_ENFORCEABLE -> createValidationResponse(caseData,
                enforcementDecision.getOrderNotEnforceableDetails(), NOT_ENFORCEABLE_LABEL);
            case ORDER_SET_ASIDE_OR_SUSPENDED -> createValidationResponse(caseData,
                enforcementDecision.getSetAsideOrSuspendedDetails(), SET_ASIDE_OR_SUSPENDED_LABEL);
            case OTHER -> createValidationResponse(caseData,
                enforcementDecision.getOtherRejectionDetails(), OTHER_REJECTION_REASON_LABEL);
            default -> createDefaultResponse(caseData);
        };

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> createValidationResponse(PCSCase caseData,
                                                                                  String rejectionDetails,
                                                                                  String textAreaLabel) {

        List<String> validationErrors = textAreaValidationService.validateSingleTextArea(
            rejectionDetails,
            textAreaLabel,
            TextAreaValidationService.EXTRA_LONG_TEXT_LIMIT
        );

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private static AboutToStartOrSubmitResponse<PCSCase, State> createDefaultResponse(PCSCase caseData) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
