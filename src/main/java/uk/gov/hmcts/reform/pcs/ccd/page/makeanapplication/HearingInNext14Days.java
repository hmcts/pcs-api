package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class HearingInNext14Days implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("hearingInNext14Days")
            .pageLabel("Is the defendant’s court hearing in the next 14 days?")
            .showCondition(fieldEquals("xui_genapp_ApplicationType", GenAppType.ADJOURN))
            .label("hearingInNext14Days-lineSeparator", "---")
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getWithin14Days)
            .done();
    }

}
