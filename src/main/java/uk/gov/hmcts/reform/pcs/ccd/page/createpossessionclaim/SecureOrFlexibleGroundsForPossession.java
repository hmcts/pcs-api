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
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SecureOrFlexibleGroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundsForPossession", this::midEvent)
            .pageLabel("What are your grounds for possession")
            .showCondition("typeOfTenancyLicence=\"SECURE_TENANCY\" OR typeOfTenancyLicence=\"FLEXIBLE_TENANCY\"")
            .label("groundsForPossession-lineSeparator", "---")
            .mandatory(PCSCase::getSecureOrFlexibleTenancyDiscretionaryGrounds)
            .mandatory(PCSCase::getSecureOrFlexibleTenancyMandatoryGrounds)
            .mandatory(PCSCase::getSecureOrFlexibleTenancyMandatoryGrounds2)
            .mandatory(PCSCase::getSecureOrFlexibleTenancyDiscretionaryGrounds2);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        caseData.setSelectedSecureOrFlexibleDiscretionaryGrounds(
            setSelectedDiscretionaryGrounds(caseData.getSecureOrFlexibleTenancyDiscretionaryGrounds()
                ,caseData.getSecureOrFlexibleTenancyDiscretionaryGrounds2()
            )
        );

        caseData.setSelectedSecureOrFlexibleMandatoryGrounds(
            setSelectedMandatoryGrounds(caseData.getSecureOrFlexibleTenancyMandatoryGrounds()
                ,caseData.getSecureOrFlexibleTenancyMandatoryGrounds2()
            )
        );
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }


    private Set<DiscretionaryGrounds> setSelectedDiscretionaryGrounds(DynamicMultiSelectList list1, DynamicMultiSelectList list2) {
        return Stream.of(list1, list2)
            .filter(Objects::nonNull)
            .map(DynamicMultiSelectList::getValue)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(DynamicListElement::getLabel)
            .map(DiscretionaryGrounds::fromLabel)
            .collect(Collectors.toSet());
    }


    private Set<MandatoryGrounds> setSelectedMandatoryGrounds(DynamicMultiSelectList list1, DynamicMultiSelectList list2) {
        return Stream.of(list1, list2)
            .filter(Objects::nonNull)
            .map(DynamicMultiSelectList::getValue)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(DynamicListElement::getLabel)
            .map(MandatoryGrounds::fromLabel)
            .collect(Collectors.toSet());
    }

}
