package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@AllArgsConstructor
@Component
public class AddMoreThan25DefendantsDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("moreThan25DefendantsDetails")
            .pageLabel("Upload additional defendant list")
            .showCondition("addAdditionalDefendant=\"Yes\" AND addMoreThan25Defendants=\"Yes\"")
            .label("lineSeparator", "---")
            .label("moreThan25DefendantsDetails-label", """
                Upload a document showing each additional defendant's:
                <ul class="govuk-list govuk-list--bullet govuk-!-margin-left-4">
                    <li class="govuk-!-font-size-19">
                        first and last name
                    </li>
                    <li class="govuk-!-font-size-19">
                        correspondence address
                    </li>
                </ul>
                <span class="govuk-!-font-size-19">
                    If any details are unknown, you should indicate this.
                </span>
                <h2 class="govuk-heading-s">
                    Before you upload your documents
                </h2>
                <span class="govuk-!-font-size-19">
                    Give your document a name that explains what it is.
                </span>
                """
            )
            .mandatory(PCSCase::getMoreThan25DefendantsDocuments);
    }
}
