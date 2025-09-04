package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.MandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentAreasOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SecureOrFlexibleGroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("secureOrFlexibleGroundsForPossession", this::midEvent)
            .pageLabel("What are your grounds for possession")
            .showCondition("typeOfTenancyLicence=\"SECURE_TENANCY\" OR typeOfTenancyLicence=\"FLEXIBLE_TENANCY\"")
            .label("secureOrFlexibleGroundsForPossession-info", """
               ---
               <p class="govuk-body"> You may have already given defendant's notice of your intention to begin
                possession proceedings.
                If you have, you should have written the grounds you're making your claim under. You should select these
                grounds here and any extra ground you'd like to add to your claim, if you need to.<br><br>

               <a href="#" class="govuk-link" rel="noreferrer noopener" target="_blank">
                More information about possessions grounds (opens in a new tab)
                </a>.
               </p>
               """)
            .optional(PCSCase::getSecureOrFlexibleDiscretionaryGrounds)
            .optional(PCSCase::getSecureOrFlexibleMandatoryGrounds)
            .optional(PCSCase::getSecureOrFlexibleMandatoryGroundsAlternativeAccommodation)
            .optional(PCSCase::getSecureOrFlexibleDiscretionaryGroundsAlternativeAccommodation);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        caseData.setSelectedSecureOrFlexibleDiscretionaryGrounds(
            setSelectedGrounds(caseData.getSecureOrFlexibleDiscretionaryGrounds(),
                               caseData.getSecureOrFlexibleDiscretionaryGroundsAlternativeAccommodation(),
                               DiscretionaryGrounds::fromLabel
            )
        );

        caseData.setSelectedSecureOrFlexibleMandatoryGrounds(
            setSelectedGrounds(caseData.getSecureOrFlexibleMandatoryGrounds(),
                               caseData.getSecureOrFlexibleMandatoryGroundsAlternativeAccommodation(),
                               MandatoryGrounds::fromLabel
            )
        );
        caseData.setRentAreasOrBreachOfTenancy(
            DynamicMultiSelectList.builder().listItems(
                getRentArrearsOrBreachOfTenancyOptions()
            ).value(Collections.emptyList()).build()
        );
        if (caseData.getSelectedSecureOrFlexibleDiscretionaryGrounds().isEmpty()
            && caseData.getSelectedSecureOrFlexibleMandatoryGrounds().isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of("Please select at least one ground"))
                .build();
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private <T> Set<T> setSelectedGrounds(DynamicMultiSelectList list1,
                                          DynamicMultiSelectList list2,
                                          Function<String, T> fromLabel) {
        return Stream.of(list1, list2)
            .filter(Objects::nonNull)
            .map(DynamicMultiSelectList::getValue)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(DynamicListElement::getLabel)
            .map(fromLabel)
            .collect(Collectors.toSet());
    }


    private List<DynamicListElement> getRentArrearsOrBreachOfTenancyOptions(
    ) {
        return Arrays.stream(RentAreasOrBreachOfTenancy.values())
            .map(g -> new DynamicListElement(UUID.randomUUID(), g.getLabel()))
            .collect(Collectors.toList());
    }
}
