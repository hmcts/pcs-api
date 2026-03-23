package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.AbstractVulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_FLOW;

@Component
public class VulnerableAdultsChildrenPage extends AbstractVulnerableAdultsChildrenPage implements CcdPageConfiguration {

    public VulnerableAdultsChildrenPage(TextAreaValidationService textAreaValidationService) {
        super(textAreaValidationService);
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String fieldPrefix = getPageKey();
        pageBuilder
            .page(fieldPrefix, this::midEvent)
            .pageLabel(PAGE_LABEL)
            .showCondition(WARRANT_FLOW)
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
            .label(fieldPrefix + "-saveAndReturn", SAVE_AND_RETURN);
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
    public String getFieldSuffix() {
        return "";
    }
}
