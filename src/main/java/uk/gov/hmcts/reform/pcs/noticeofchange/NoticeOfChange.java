package uk.gov.hmcts.reform.pcs.noticeofchange;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.*;

@Component
@RequiredArgsConstructor
public class NoticeOfChange implements CCDConfig<PCSCase, State, UserRole> {

    private final NoticeOfChangeApplyService noticeOfChangeApplyService;

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder.noticeOfChange().challenge("NoCChallenge").question(
                "defendantFirstName",
                "Enter client first name"
            )
//            .answer(UserRole.DEFENDANT_SOLICITOR).complex(PCSCase::getAlternativesToPossession).field(
//                AlternativesToPossession::getLabel).done().question(
//                "defendantLastName",
//                "Enter client last name"
//            )


            .answer(UserRole.DEFENDANT_SOLICITOR).complex(PCSCase::getDefendant1).field(DefendantDetails::getFirstName).done().question(
                "defendantLastName",
                "Enter client last name"
            ).answer(UserRole.DEFENDANT_SOLICITOR).complex(PCSCase::getDefendant1).field(DefendantDetails::getLastName).done()
            .aboutToSubmitCallback(
                noticeOfChangeApplyService::applyNoticeOfChange)
            .submittedCallback(this::noticeOfChangeSubmitted);
    }

    private SubmittedCallbackResponse noticeOfChangeSubmitted(
        CaseDetails<PCSCase, State> details, CaseDetails<PCSCase, State> before) {
        // TODO
        return SubmittedCallbackResponse.builder().build();
    }
}
