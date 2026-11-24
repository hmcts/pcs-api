package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;

// Grounds for possession Standard Contract and Other Contract
@Component
@Slf4j
public class GroundsForPossessionWalesPage implements CcdPageConfiguration {

    private final GroundsRules groundsRules;

    @Autowired
    public GroundsForPossessionWalesPage() {
        this(new GroundsRules());
    }

    public GroundsForPossessionWalesPage(GroundsRules groundsRules) {
        this.groundsRules = groundsRules;
    }

    private static final String DISCRETIONARY_GROUNDS = "possessionGroundsWales_DiscretionaryGrounds";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundsForPossessionWales", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition(WALES + " AND "
                + "(occupationLicenceTypeWales=\"STANDARD_CONTRACT\" OR occupationLicenceTypeWales=\"OTHER\")")
            .label(
                "groundsForPossessionWales-info",
                """
                  ---
                  <p>You may have already given the defendants notice of your intention to begin possession
                  proceedings. If you have, you should have written the grounds you’re making your claim under.
                  You should select these grounds here and select any extra grounds you’d like to add to your claim,
                  if you need to.</p>
                  <p class="govuk-body">
                    <a href="https://www.gov.wales/understanding-possession-action-process-guidance-tenants-contract-holders-html" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>.
                  </p>
                """
            )
            .complex(PCSCase::getGroundsForPossessionWales)
                .optional(GroundsForPossessionWales::getDiscretionaryGrounds)
                .optional(
                    GroundsForPossessionWales::getEstateManagementGrounds,
                    ShowConditions.fieldContains(DISCRETIONARY_GROUNDS,
                                                 DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160
                    )
                )
                .optional(GroundsForPossessionWales::getMandatoryGrounds)
                .done()
            .label("groundsForPossessionWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        PCSCase data = details.getData();

        GroundsForPossessionWales grounds = data.getGroundsForPossessionWales();
        if (grounds == null) {
            grounds = GroundsForPossessionWales.builder().build();
            data.setGroundsForPossessionWales(grounds);
        }

        List<String> errors = groundsRules.validateWalesGrounds(grounds);

        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
                .build();
        }

        groundsRules.deriveWalesGroundsRouting(grounds).ifPresent(routing -> {
            data.setShowASBQuestionsPageWales(routing.showAsbQuestionsPage());
            data.setShowReasonsForGroundsPageWales(routing.showReasonsPage());
        });

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .build();
    }
}
