package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

class RiskCategoryTestUtil {

    static String expectedCharacterLimitErrorMessage(RiskCategory riskCategory) {
        return String.format("In '%s', you have entered more than the maximum number of characters (%d)",
                             riskCategory.getText(),
                             TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
        );
    }
}
