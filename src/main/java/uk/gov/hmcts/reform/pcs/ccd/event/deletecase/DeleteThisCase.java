package uk.gov.hmcts.reform.pcs.ccd.event.deletecase;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.TTL;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.deletecase.DeleteCasePageConfigurer;

import java.time.LocalDate;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.deleteThisCase;

@Slf4j
@Component
@AllArgsConstructor
public class DeleteThisCase implements CCDConfig<PCSCase, State, UserRole> {

    private static final String CONFIRMATION_BODY = """
                <p class="govuk-body">
                    <a href="/cases/"
                        class="govuk-link govuk-link--no-visited-state">Close and return to case list</a>
                </p>
                """;
    private final DeleteCasePageConfigurer deleteCasePageConfigurer;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
            .decentralisedEvent(deleteThisCase.name(), this::submit)
            .forStates(State.AWAITING_SUBMISSION_TO_HMCTS, State.PENDING_CASE_ISSUED)
            .name("Delete this claim")
            .showCondition(unissuedClaimStateCondition())
            .grant(Permission.CRUD, UserRole.CREATOR)
            .grant(Permission.CRUD, CLAIMANT_SOLICITOR);

        deleteCasePageConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        if (caseData.getDeleteUnsubmittedClaim().equals(YesOrNo.YES)) {
            caseData.setTtl(TTL.builder()
                    .systemTTL(LocalDate.now())
                    .suspended(YesOrNo.NO)
                    .build());
            return SubmitResponse.<State>builder()
                    .state(State.DELETED)
                    .confirmationBody(CONFIRMATION_BODY)
                    .build();
        } else {
            return SubmitResponse.<State>builder().build();
        }
    }

    private static String unissuedClaimStateCondition() {
        return ShowConditions.stateEquals(State.AWAITING_SUBMISSION_TO_HMCTS)
            + " OR "
            + ShowConditions.stateEquals(State.PENDING_CASE_ISSUED);
    }
}
