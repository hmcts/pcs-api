package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.AbstractVulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

@Component
public class VulnerableAdultsChildrenPage extends AbstractVulnerableAdultsChildrenPage implements CcdPageConfiguration {

    public VulnerableAdultsChildrenPage(TextAreaValidationService textAreaValidationService) {
        super(textAreaValidationService);
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String fieldPrefix = getFieldPrefix();
        pageBuilder
            .page(fieldPrefix, this::midEvent)
            .pageLabel(PAGE_LABEL)
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW)
            .label(fieldPrefix + "-line-separator", "---")
            .label(fieldPrefix + "-information-text", INFO_MARKUP)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getRawWarrantDetails)
            .mandatory(RawWarrantDetails::getVulnerablePeoplePresent)
            .complex(RawWarrantDetails::getVulnerableAdultsChildren,
                    "vulnerablePeoplePresent=\"YES\"")
            .mandatory(VulnerableAdultsChildren::getVulnerableCategory)
            .mandatory(VulnerableAdultsChildren::getVulnerableReasonText, getVulnerablePeoplePresentShowCondition())
            .done()
            .done()
            .done()
            .label(fieldPrefix + "-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    @Override
    public String getVulnerablePeoplePresentShowCondition() {
        return "vulnerablePeoplePresent=\"YES\" "
                + "AND (vulnerableAdultsChildren.vulnerableCategory=\"VULNERABLE_ADULTS\" "
                + "OR vulnerableAdultsChildren.vulnerableCategory=\"VULNERABLE_CHILDREN\" "
                + "OR vulnerableAdultsChildren.vulnerableCategory=\"VULNERABLE_ADULTS_AND_CHILDREN\")";
    }

    @Override
    public String getVulnerableReasonTextToValidate(PCSCase data) {
        return data.getEnforcementOrder()
                .getRawWarrantDetails().getVulnerableAdultsChildren() != null
                ? data.getEnforcementOrder()
                .getRawWarrantDetails().getVulnerableAdultsChildren().getVulnerableReasonText()
                : null;
    }

    @Override
    public String getFieldPrefix() {
        return CcdPage.getFieldPrefix(this.getClass());
    }
}
