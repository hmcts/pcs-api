package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;

@Component
public class ExemptLandlord implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("exemptLandlord")
            .pageLabel("Exempt landlord")
            .showCondition(WALES)
            .label("exemptLandlord-info", "---")
            .mandatory(PCSCase::getIsExemptLandlord)
            .label("exemptLandlord-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
