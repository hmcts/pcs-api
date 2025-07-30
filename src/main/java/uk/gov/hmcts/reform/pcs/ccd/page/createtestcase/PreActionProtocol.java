package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class PreActionProtocol implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("pre action protocol")
                .pageLabel("Pre-action Protocol")
                .label("preActionProtocolInfo",
                        """
                                    Registered providers of social housing should follow the pre-action protocol before 
                                    making a housing possession claim. You should have:

                                    Contacted, or attempted to contact, the defendants

                                    Tried to agree a repayment plan

                                    Applied for arrears to be paid by the Department for Work and Pensions 
                                    (DWP) by deductions from the defendants' benefits

                                    Offered to assist the defendants in a claim for housing benefit or Universal Credit
                                    Your case could be delayed or rejected if you have not followed the 
                                    pre-action protocol and completed all the steps.
                                """)
                .mandatory(PCSCase::getPreActionProtocolCompleted);
    }
}