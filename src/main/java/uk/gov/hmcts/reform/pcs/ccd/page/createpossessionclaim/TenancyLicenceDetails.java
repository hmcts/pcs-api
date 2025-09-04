package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.MandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TenancyLicenceDetails implements CcdPageConfiguration {

    private final Clock ukClock;

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
               <p class="govuk-!-font-size-16 govuk-!-margin-bottom-2">
                Do you want to upload a copy of the tenancy or licence agreement?
                </p>
               <p class="govuk-hint govuk-!-font-size-16 govuk-!-margin-top-1">
                You can either upload this now or closer to the hearing date. Any documents you upload now will be
                included in the pack of documents a judge will receive before hearing the hearing (the bundle).
                </p>
               """)
            .optional(PCSCase::getTenancyLicenceDocuments)
            .label("lineSeparator", "---");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        LocalDate tenancyLicenceDate = details.getData().getTenancyLicenceDate();
        LocalDate currentDate = LocalDate.now(ukClock);

        if (tenancyLicenceDate != null && !tenancyLicenceDate.isBefore(currentDate)) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(List.of("Date the tenancy or licence began must be in the past"))
            .build();
        }

        if (caseData.getTypeOfTenancyLicence() == TenancyLicenceType.SECURE_TENANCY
            || caseData.getTypeOfTenancyLicence() == TenancyLicenceType.FLEXIBLE_TENANCY)  {

            caseData.setSecureOrFlexibleDiscretionaryGrounds(
                    DynamicMultiSelectList.builder()
                            .listItems(getDiscretionaryGroundOptions(
                                TenancyLicenceType.FLEXIBLE_TENANCY,false))
                        .value(Collections.emptyList())
                            .build());
            caseData.setSecureOrFlexibleDiscretionaryGroundsAlternativeAccommodation(
                    DynamicMultiSelectList.builder()
                            .listItems(getDiscretionaryGroundOptions(
                                TenancyLicenceType.FLEXIBLE_TENANCY,true))
                        .value(Collections.emptyList())
                            .build()
            );
            caseData.setSecureOrFlexibleMandatoryGrounds(
                    DynamicMultiSelectList.builder()
                            .listItems(getMandatoryGroundOptions(
                                TenancyLicenceType.FLEXIBLE_TENANCY,false))
                        .value(Collections.emptyList())
                            .build()
            );
            caseData.setSecureOrFlexibleMandatoryGroundsAlternativeAccommodation(
                    DynamicMultiSelectList.builder()
                            .listItems(getMandatoryGroundOptions(
                                TenancyLicenceType.FLEXIBLE_TENANCY,true))
                        .value(Collections.emptyList())
                            .build()
            );

            caseData.setIsTenancyTypeSecureOrFlexible(YesOrNo.YES);
        } else {
            caseData.setIsTenancyTypeSecureOrFlexible(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private List<DynamicListElement> getDiscretionaryGroundOptions(
            TenancyLicenceType tenancyType,
            boolean alternativeAccommodationSection
    ) {
        return Arrays.stream(DiscretionaryGrounds.values())
                .filter(g -> g.isApplicableFor(tenancyType))
                .filter(g ->
                            g.isAlternativeAccommodationAvailable() == alternativeAccommodationSection)
                .map(g -> new DynamicListElement(UUID.randomUUID(), g.getLabel()))
                .collect(Collectors.toList());
    }

    private List<DynamicListElement> getMandatoryGroundOptions(
            TenancyLicenceType tenancyType,
            boolean alternativeAccommodationSection
    ) {
        return  Arrays.stream(MandatoryGrounds.values())
                .filter(g -> g.isApplicableFor(tenancyType))
                .filter(g ->
                            g.isAlternativeAccommodationAvailable() == alternativeAccommodationSection)
                .map(g -> new DynamicListElement(UUID.randomUUID(), g.getLabel()))
                .collect(Collectors.toList());
    }

}


