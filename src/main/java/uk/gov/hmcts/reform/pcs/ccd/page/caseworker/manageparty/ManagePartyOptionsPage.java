package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;

@Component
public class ManagePartyOptionsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("managePartyOptions")
            .pageLabel("Update, add or remove")
            .label("managePartyOptions-separator", "---")
            .complex(PCSCase::getAddPartyDetails)
                .mandatory(AddPartyDetails::getManagePartyOptions)
                .mandatory(
                    AddPartyDetails::getAddPartyType,
                    ShowConditions.fieldEquals("addParty_ManagePartyOptions", ManagePartyOptions.ADD_PARTY))
            .done();
    }
}