package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class AppliedForHelpWithFees implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("appliedForHelpWithFees")
            .pageLabel("Have they already applied for help with their application fee?")
            .showCondition(ShowConditions.and(
                fieldEquals("xui_genapp_ShowHwfScreens", VerticalYesNo.YES),
                fieldEquals("xui_genapp_NeedHwf", VerticalYesNo.YES)
            ))
            .label("appliedForHelpWithFees-lineSeparator", "---")
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getAppliedForHwf)
            .mandatory(XuiGenAppRequest::getHwfReference, fieldEquals("xui_genapp_AppliedForHwf", VerticalYesNo.YES))
            .done();
    }

}
