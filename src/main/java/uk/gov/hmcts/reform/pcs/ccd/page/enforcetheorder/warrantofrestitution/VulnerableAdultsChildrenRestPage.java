package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.AbstractVulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@Component
public class VulnerableAdultsChildrenRestPage extends AbstractVulnerableAdultsChildrenPage
        implements CcdPageConfiguration {

    public VulnerableAdultsChildrenRestPage(TextAreaValidationService textAreaValidationService) {
        super(textAreaValidationService);
    }

    @Override
    public String getVulnerablePeoplePresentShowCondition() {
        return "vulnerablePeoplePresentRest=\"YES\" "
                + "AND (vulnerableAdultsChildrenRest.vulnerableCategory=\"VULNERABLE_ADULTS\" "
                + "OR vulnerableAdultsChildrenRest.vulnerableCategory=\"VULNERABLE_CHILDREN\" "
                + "OR vulnerableAdultsChildrenRest.vulnerableCategory=\"VULNERABLE_ADULTS_AND_CHILDREN\")";
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(getPageId(), this::midEvent)
            .pageLabel(PAGE_LABEL)
            .showCondition(ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW)
            .label(getPageId() + "-line-separator", "---")
            .label(getPageId() + "-information-text", INFO_MARKUP)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getRawWarrantRestDetails)
            .mandatory(RawWarrantRestDetails::getVulnerablePeoplePresentRest)
            .complex(RawWarrantRestDetails::getVulnerableAdultsChildrenRest,
                    "vulnerablePeoplePresentRest=\"YES\"")
            .mandatory(VulnerableAdultsChildren::getVulnerableCategory)
            .mandatory(VulnerableAdultsChildren::getVulnerableReasonText, getVulnerablePeoplePresentShowCondition())
            .done()
            .done()
            .done()
            .label(getPageId() + "-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    public List<String> performValidation(PCSCase data) {
        List<String> errors = new ArrayList<>();

        if (vulnerablePeoplePresent.test(data.getEnforcementOrder().getRawWarrantRestDetails()
                .getVulnerablePeoplePresentRest())) {
            String txt = data.getEnforcementOrder()
                    .getRawWarrantRestDetails().getVulnerableAdultsChildrenRest() != null
                    ? data.getEnforcementOrder()
                    .getRawWarrantRestDetails().getVulnerableAdultsChildrenRest().getVulnerableReasonText()
                    : null;
            errors.addAll(getValidationErrors(txt));
        }

        return errors;
    }
}
