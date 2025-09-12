package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Slf4j
@AllArgsConstructor
@Service
public class ResumeClaim implements CcdPageConfiguration {

    private final UnsubmittedCaseDataService unsubmittedCaseDataService;
    private final ModelMapper modelMapper;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("resumeClaim", this::midEvent)
            .pageLabel("Resume claim")
            .showCondition("hasUnsubmittedCaseData=\"Yes\"")
            .readonly(PCSCase::getHasUnsubmittedCaseData, NEVER_SHOW)
            .label("resumeClaim-info", """
                ---
                <p class="govuk-body">

                Your previous answers have been saved. You can either:

                <ul class="govuk-list">
                  <li class="govuk-!-font-size-19">resume your claim using your saved answers</li>
                  <li class="govuk-!-font-size-19">start your claim again from the beginning</li>
                </ul>

                If you resume your claim using your saved answers, you must select ‘Continue’ at the bottom of each
                page until you reach the question you’d like to continue your claim from.

                </p>
                """)
            .mandatory(PCSCase::getResumeClaimKeepAnswers);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        long caseReference = details.getId();
        PCSCase caseData = details.getData();

        log.debug("Resuming claim - keep existing answers = " + caseData.getResumeClaimKeepAnswers());

        if (caseData.getResumeClaimKeepAnswers() == YesOrNo.YES) {
            unsubmittedCaseDataService.getUnsubmittedCaseData(caseReference)
                .ifPresentOrElse(
                    unsubmittedCaseData -> modelMapper.map(unsubmittedCaseData, caseData),
                    () -> {
                        throw new UnsubmittedDataException("No unsubmitted case data found for case " + caseReference);
                    }
                );
        }

        // Initialize defendant flow variables for the first time
        if (caseData.getCurrentDefendantNumber() == null) {
            caseData.setCurrentDefendantNumber(1);
        }
        if (caseData.getAddAnotherDefendant() == null) {
            caseData.setAddAnotherDefendant(YesOrNo.YES);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
