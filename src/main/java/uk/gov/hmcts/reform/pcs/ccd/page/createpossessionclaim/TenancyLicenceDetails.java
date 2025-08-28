package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class TenancyLicenceDetails implements CcdPageConfiguration {

    private Clock ukClock;

    public TenancyLicenceDetails(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("tenancyLicenceDetails", this::midEvent)
            .pageLabel("Tenancy or licence details")
            .label("tenancyLicenceDetails-info", """
               ---
               <h2 class="govuk-heading-m">Tenancy or licence type</h2>
              """)
            .mandatory(PCSCase::getTypeOfTenancyLicence)
            .mandatory(PCSCase::getDetailsOfOtherTypeOfTenancyLicence, "typeOfTenancyLicence=\"OTHER\"")
            .label("tenancyLicenceDetails-date-section", """
               ---
               <h2 class="govuk-heading-m">Tenancy or licence start date</h2>
              """)
            .optional(PCSCase::getTenancyLicenceDate)
            .label("tenancyLicenceDetails-doc-section", """
               ---
               <h2 class="govuk-heading-m">Upload tenancy or licence agreement</h2>
               <h3 class="govuk-heading-s">Do you want to upload a copy of the tenancy or licence agreement?</h3>
               <p class='govuk-hint govuk-!-font-size-16 govuk-!-margin-top-1'>
               You can either upload this now or closer to the hearing data. Any documents you upload now will be
                included in the pack of documents a judge will receive before hearing the hearing (the bundle)
                </p>
               """)
            .optional(PCSCase::getTenancyLicenceDocuments)
            .label("lineSeperator", "---");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        String date = details.getData().getTenancyLicenceDate();

        if (!isDateInThePast(date)) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(List.of("Date the tenancy or licence began must be in the past"))
            .build();
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private Boolean isDateInThePast(String date) {
        LocalDate currentDate = LocalDate.now(ukClock);
        LocalDate tenancyLicenceDate = LocalDate.parse(date);
        return tenancyLicenceDate.isBefore(currentDate);
    }
}


