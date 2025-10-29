package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesPeriodicContractTerms;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class ProhibitedConductWales implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("prohibitedConductWales", this::midEvent)
            .pageLabel("Prohibited conduct standard contract")
            .showCondition("legislativeCountry=\"Wales\"")
            .label("prohibitedConductWales-info", """
                 ---
                <p class="govuk-body" tabindex="0">
                    If a judge decides that possession is not reasonable at this time, 
                    they may instead decide to make an order imposing a prohibited 
                    conduct standard contract.
                </p>
                <p class="govuk-body" tabindex="0">This is a 12-month probationary contract.</p>
                """)
            .mandatory(PCSCase::getProhibitedConductWalesClaim)
            .complex(PCSCase::getWalesPeriodicContractTerms, "prohibitedConductWalesClaim=\"YES\"")
                .mandatory(WalesPeriodicContractTerms::getAgreedTermsOfPeriodicContract)
                .mandatory(WalesPeriodicContractTerms::getDetailsOfTerms, 
                    "walesPeriodicContractTerms.agreedTermsOfPeriodicContract=\"YES\"")
            .done()
            .mandatory(PCSCase::getProhibitedConductWalesWhyMakingClaim, "prohibitedConductWalesClaim=\"YES\"");
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        WalesPeriodicContractTerms walesPeriodicContractTerms =
            caseData.getWalesPeriodicContractTerms();

        if (caseData.getProhibitedConductWalesClaim() == VerticalYesNo.YES) {
            textAreaValidationService.validateTextArea(
                caseData.getProhibitedConductWalesWhyMakingClaim(),
                "Why are you making this claim?",
                TextAreaValidationService.SHORT_TEXT_LIMIT,
                validationErrors
            );
        }

        if (caseData.getProhibitedConductWalesClaim() == VerticalYesNo.YES
            && walesPeriodicContractTerms != null
            && walesPeriodicContractTerms.getAgreedTermsOfPeriodicContract() == VerticalYesNo.YES) {
            textAreaValidationService.validateTextArea(
                walesPeriodicContractTerms.getDetailsOfTerms(),
                "Give details of the terms you've agreed",
                TextAreaValidationService.SHORT_TEXT_LIMIT,
                validationErrors
            );
        }
        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}

