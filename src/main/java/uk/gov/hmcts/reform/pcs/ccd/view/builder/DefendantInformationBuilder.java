package uk.gov.hmcts.reform.pcs.ccd.view.builder;


import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseTabView;

public class DefendantInformationBuilder {
    protected AddressUK getDefendantAddressForService(Party defendant, PCSCase pcsCase) {
        if (defendant.getAddressKnown() != VerticalYesNo.YES) {
            return pcsCase.getPropertyAddress();
        }

        return defendant.getAddress() != null ? defendant.getAddress() : pcsCase.getPropertyAddress();
    }

    protected String getDefendantFirstName(Party defendant) {
        return defendant.getNameKnown() == VerticalYesNo.YES ? defendant.getFirstName() : CaseTabView.NAME_UNKNOWN;
    }

    protected String getDefendantLastName(Party defendant) {
        return defendant.getNameKnown() == VerticalYesNo.YES ? defendant.getLastName() : CaseTabView.NAME_UNKNOWN;
    }
}
