package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.ProhibitedConductWalesWrappedQuestion;

@Component
public class ProhibitedConductWalesPage implements CcdPageConfiguration {

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
            .complex(PCSCase::getProhibitedConductWalesWrappedQuestion, "prohibitedConductWalesClaim=\"YES\"")
                .mandatory(ProhibitedConductWalesWrappedQuestion::getAgreedTermsOfPeriodicContract)
                .mandatory(ProhibitedConductWalesWrappedQuestion::getDetailsOfTerms, 
                    "prohibitedConductWalesWrappedQuestion.agreedTermsOfPeriodicContract=\"YES\"")
            .done()
            .mandatory(PCSCase::getProhibitedConductWalesWhyMakingClaim, "prohibitedConductWalesClaim=\"YES\"");
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}

