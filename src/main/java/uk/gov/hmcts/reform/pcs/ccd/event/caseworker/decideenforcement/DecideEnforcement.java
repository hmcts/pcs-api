package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.decideenforcement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AcceptReject;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementApplicationDecisionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementApplicationDecisionEntity.EnforcementApplicationDecisionEntityBuilder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.decideenforcement.AcceptOrRejectPage;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WritRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.exception.WarrantNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.WritNotFoundException;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_PROGRESSION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_ENFORCEMENT_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PREPARE_FOR_HEARING_CONDUCT_HEARING;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.decideEnforcement;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1B;

@Component
@RequiredArgsConstructor
public class DecideEnforcement implements CCDConfig<PCSCase, State, UserRole> {

    private final ClaimService claimService;
    private final AddressFormatter addressFormatter;
    private final WarrantRepository warrantRepository;
    private final WritRepository writRepository;
    private final AcceptOrRejectPage acceptOrRejectPage;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(decideEnforcement.name(), this::submit, this::start)
            // TODO: Review states as part of HDPI-10102
            .forStates(CASE_PROGRESSION, PREPARE_FOR_HEARING_CONDUCT_HEARING, PENDING_ENFORCEMENT_ISSUED)
            .name("Decide enforcement")
            .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1B))
            .grant(Permission.CRU, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .endButtonLabel("Submit")
            .showSummary();

        new PageBuilder(eventBuilder)
            .add(acceptOrRejectPage);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        EnforcementDecision enforcementDecision = caseData.getEnforcementDecision();

        EnforcementOrderEntity enforcementOrderEntity = getEnforcementOrder(caseReference);

        if (enforcementOrderEntity.getWarrantDetails() != null) {
            enforcementDecision.setWarrantId(enforcementOrderEntity.getWarrantDetails().getId());
        } else if (enforcementOrderEntity.getWritDetails() != null) {
            enforcementDecision.setWritId(enforcementOrderEntity.getWritDetails().getId());
        } else {
            throw new IllegalStateException("No warrant or writ found for enforcement order "
                                                + enforcementOrderEntity.getId());
        }

        return caseData;
    }

    private EnforcementOrderEntity getEnforcementOrder(long caseReference) {
        ClaimEntity claimEntity = claimService.loadClaim(caseReference);

        Set<EnforcementOrderEntity> enforcementOrders = claimEntity.getEnforcementOrders();

        if (enforcementOrders.isEmpty()) {
            throw new IllegalStateException("No enforcement order found for case " + caseReference);
        }

        // TODO: Review this when enforcement state model is implemented so that filtering can be applied (HDPI-10102)
        if (enforcementOrders.size() > 1) {
            throw new IllegalStateException("Multiple active enforcement orders found for case " + caseReference);
        }

        return enforcementOrders.iterator().next();
    }


    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        EnforcementDecision enforcementDecision = caseData.getEnforcementDecision();

        EnforcementApplicationDecisionEntity decisionEntity = createEnforcementDecisionEntity(enforcementDecision);

        if (enforcementDecision.getWarrantId() != null) {
            Integer warrantId = enforcementDecision.getWarrantId();
            WarrantEntity warrantEntity = warrantRepository.findById(warrantId)
                .orElseThrow(() -> new WarrantNotFoundException("Unable to find warrant " + warrantId));

            warrantEntity.setEnforcementApplicationDecision(decisionEntity);
        } else if (enforcementDecision.getWritId() != null) {
            Integer writId = enforcementDecision.getWritId();
            WritEntity writEntity = writRepository.findById(writId)
                .orElseThrow(() -> new WritNotFoundException("Unable to find writ " + writId));

            writEntity.setEnforcementApplicationDecision(decisionEntity);
        } else {
            throw new IllegalStateException("No warrant or writ ID provided");
        }

        // TODO: Update enforcement state (HDPI-10102)

        return SubmitResponse.<State>builder()
            .confirmationBody(buildConfirmationMarkdown(
                caseData,
                eventPayload.caseReference(),
                caseData.getCaseNameHmctsInternal(),
                enforcementDecision.getAcceptReject()
            ))
            .build();
    }

    private EnforcementApplicationDecisionEntity createEnforcementDecisionEntity(EnforcementDecision decision) {
        EnforcementApplicationDecisionEntityBuilder builder = EnforcementApplicationDecisionEntity.builder()
            .decision(decision.getAcceptReject());

        if (decision.getAcceptReject() == AcceptReject.REJECT) {
            builder.rejectionReason(decision.getRejectionReason());
            builder.rejectionReasonAdditionalDetails(getRejectionDetails(decision));
        }

        return builder.build();
    }

    private String getRejectionDetails(EnforcementDecision enforcementDecision) {
        return switch (enforcementDecision.getRejectionReason()) {
            case CHANGE_IN_PARTY_CIRCUMSTANCES -> enforcementDecision.getChangeInPartyCircumstancesDetails();
            case ORDER_NOT_ENFORCEABLE -> enforcementDecision.getOrderNotEnforceableDetails();
            case ORDER_SET_ASIDE_OR_SUSPENDED -> enforcementDecision.getSetAsideOrSuspendedDetails();
            case OTHER -> enforcementDecision.getOtherRejectionDetails();
            default -> null;
        };
    }

    private String buildConfirmationMarkdown(PCSCase pcsCase,
                                             long caseReference,
                                             String caseName,
                                             AcceptReject decision) {

        String address = addressFormatter.formatShortAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER);

        String title = getConfirmationTitle(decision);
        String whatNextDescription = getWhatNextDescription(decision);

        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">
                Case number #%s<br>
                %s<br>
                %s
            </span>
            </div>

            <h3 class="govuk-heading-s">What happens next</h3>
            %s
            """.formatted(title, caseReference, address, caseName, whatNextDescription);
    }

    private static String getConfirmationTitle(AcceptReject decision) {
        return decision == AcceptReject.ACCEPT ? "Warrant or writ approved" : "Warrant or writ rejected";
    }

    private static String getWhatNextDescription(AcceptReject decision) {
        if (decision == AcceptReject.ACCEPT) {
            return """
                <p class="govuk-body govuk-!-margin-bottom-3">
                    If a warrant was approved, it will be sent to the bailiff.
                </p>
                <p class="govuk-body govuk-!-margin-bottom-6">
                    If a writ was approved, it will be sent to the applicant.
                </p>
                """;
        } else {
            return """
                <p class="govuk-body govuk-!-margin-bottom-3">
                    A notification will be sent to the applicant.
                </p>
                """;

        }
    }

}
