package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@AllArgsConstructor
@Component
@Slf4j
public class TenancyLicenceDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("tenancyOrLicenceDetails")
            .pageLabel("Tenancy Or Licence Details")
            .label("tenancyOrLicenceDetails-1", """
               ---
               <h2>Tenancy or licence type</h2>
              """)
            .optional(PCSCase::getTypeOfTenancyLicence)
            .mandatory(PCSCase::getDetailsOfOtherTypeOfTenancyLicence, "typeOfTenancyLicence=\"OTHER\"")
            .label("tenancyOrLicenceDetails-2", """
               ---
               <h2>Tenancy or licence start date</h2>
              """)
            .optional(PCSCase::getTenancyLicenceDate)
            .label("tenancyOrLicenceDetails-3", """
               ---
               <h2>Upload tenancy or licence agreement</h2>
               <h3>Do you want to upload a copy of the tenancy or licence agreement? (Optional)</h3>
               <p class='govuk-hint govuk-!-font-size-16 govuk-!-margin-top-1'>
               You can either upload this now or closer to the hearing data. Any documents you upload now will be
                included in the pack of documents a judge will receive before hearing the hearing (the bundle)
                </p>
              """)
            .optional(PCSCase::getTenancyLicenceDocuments)
            .optional(PCSCase::getCaseFileView)
            .label("lineSeperator", "---");
    }
}


