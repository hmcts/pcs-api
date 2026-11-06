package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Slf4j
@AllArgsConstructor
public class ChooseAnApplication implements CcdPageConfiguration {


    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("chooseAnApplication", this::midEvent)
            .pageLabel("Choose an application")
            .label("chooseAnApplication-lineSeparator", "---")
            .readonly(PCSCase::getCurrentRepresentedPartyId, NEVER_SHOW, true)
            .readonly(PCSCase::getCurrentRepresentedPartyName, NEVER_SHOW, true)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getApplicationTypeOptionList)
            .readonly(XuiGenAppRequest::getApplicationType, NEVER_SHOW, true)
            .readonly(XuiGenAppRequest::getStandardFee, NEVER_SHOW, true)
            .readonly(XuiGenAppRequest::getMaxFee, NEVER_SHOW, true)
            .readonly(XuiGenAppRequest::getSuspendFee, NEVER_SHOW, true)
            .readonly(XuiGenAppRequest::getShowHwfScreens, NEVER_SHOW, true)
            .readonly(XuiGenAppRequest::getShowNoticeScreens, NEVER_SHOW, true)
            .readonly(XuiGenAppRequest::getApplicationType, NEVER_SHOW, true)
            .done();
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                 CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        XuiGenAppRequest xuiGenAppRequest = caseData.getXuiGenAppRequest();

        String selectedApplicationType = xuiGenAppRequest.getApplicationTypeOptionList().getValue().getCode();
        xuiGenAppRequest.setApplicationType(GenAppType.valueOf(selectedApplicationType));

        GenAppType genAppType = xuiGenAppRequest.getApplicationType();

        if (genAppType != GenAppType.SUSPEND) {
            xuiGenAppRequest.setShowNoticeScreens(VerticalYesNo.YES);
        } else {
            xuiGenAppRequest.setShowNoticeScreens(VerticalYesNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
