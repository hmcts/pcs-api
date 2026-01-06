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
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
public class ProhibitedConductWalesPage implements CcdPageConfiguration {

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
            .complex(PCSCase::getProhibitedConductWales)
            .mandatory(ProhibitedConductWales::getProhibitedConductWalesClaim)
            .complex(ProhibitedConductWales::getPeriodicContractTermsWales, "wales_ProhibitedConductWalesClaim=\"YES\"")
                .mandatory(PeriodicContractTermsWales::getAgreedTermsOfPeriodicContract)
                .mandatory(PeriodicContractTermsWales::getDetailsOfTerms,
                "wales_PeriodicContractTermsWales.agreedTermsOfPeriodicContract=\"YES\"")
                .done()
            .mandatory(ProhibitedConductWales::getProhibitedConductWalesWhyMakingClaim,
                "wales_ProhibitedConductWalesClaim=\"YES\"")
            .done()
            .label("prohibitedConductWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        ProhibitedConductWales prohibitedConductWales =
            caseData.getProhibitedConductWales();

        Optional.ofNullable(prohibitedConductWales)
            .filter(pc -> pc.getProhibitedConductWalesClaim() == VerticalYesNo.YES)
            .map(ProhibitedConductWales::getPeriodicContractTermsWales)
            .filter(terms -> terms.getAgreedTermsOfPeriodicContract() == VerticalYesNo.YES)
            .ifPresent(terms ->
                           textAreaValidationService.validateTextArea(
                               terms.getDetailsOfTerms(),
                               "Give details of the terms you’ve agreed",
                               TextAreaValidationService.SHORT_TEXT_LIMIT,
                               validationErrors
                           )
            );

        if (prohibitedConductWales.getProhibitedConductWalesClaim() == VerticalYesNo.YES) {
            textAreaValidationService.validateTextArea(
                prohibitedConductWales.getProhibitedConductWalesWhyMakingClaim(),
                "Why are you making this claim?",
                TextAreaValidationService.SHORT_TEXT_LIMIT,
                validationErrors
            );
        }

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}

