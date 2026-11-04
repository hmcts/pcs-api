package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantofrestitution;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.AbstractVulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

@Component
public class VulnerableAdultsChildrenWarrantRestPage extends AbstractVulnerableAdultsChildrenPage
        implements CcdPageConfiguration {

    private static final String FIELD_SUFFIX = "WarrantRest";

    public VulnerableAdultsChildrenWarrantRestPage(TextAreaValidationService textAreaValidationService) {
        super(textAreaValidationService);
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String fieldPrefix = getPageKey();
        pageBuilder
            .page(fieldPrefix, this::midEvent)
            .pageLabel(PAGE_LABEL)
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label(fieldPrefix + "-line-separator", "---")
            .label(fieldPrefix + "-information-text", INFO_MARKUP)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getRawWarrantRestDetails)
            .mandatory(RawWarrantRestDetails::getVulnerablePeoplePresentWarrantRest)
            .complex(RawWarrantRestDetails::getVulnerableAdultsChildrenWarrantRest,
                    "vulnerablePeoplePresentWarrantRest=\"YES\"")
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
                .getRawWarrantRestDetails().getVulnerableAdultsChildrenWarrantRest() != null
                ? data.getEnforcementOrder()
                .getRawWarrantRestDetails().getVulnerableAdultsChildrenWarrantRest().getVulnerableReasonText()
                : null;
    }

    @Override
    public String getFieldSuffix() {
        return FIELD_SUFFIX;
    }
}
