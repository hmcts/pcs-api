package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

@Component
public class ClaimantDetailsWalesPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantDetailsWales")
            .pageLabel("Exempt Landlord")
            .showCondition("legislativeCountry=\"Wales\"")
            .label("claimantDetailsWales-info", "---")
            .mandatory(PCSCase::getIsExemptLandlord)
            .label("claimantDetailsWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
