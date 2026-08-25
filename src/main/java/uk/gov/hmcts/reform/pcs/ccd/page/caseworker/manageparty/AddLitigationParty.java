package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;

@Component
public class AddLitigationParty implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("addLitigationParty")
            .showCondition(ShowConditions.and(
                ShowConditions.fieldEquals("addParty_ManagePartyOptions", ManagePartyOptions.ADD_PARTY),
                ShowConditions.fieldEquals("addParty_AddPartyType", PartyType.LITIGATION_FRIEND)))
            .pageLabel("Add a party")
            .label("addLitigationParty-separator", "---")
            .complex(PCSCase::getAddPartyDetails)
                .mandatory(AddPartyDetails::getPartyRadioList)
            .done();
    }
}
