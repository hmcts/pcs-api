package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

/**
 * Field length limits applied to bulk claims. They match the make a claim journey's text area limits and
 * the database column sizes, and are checked in addition to the schema because a few schema limits are looser.
 */
public final class BulkLimits {

    public static final int NAME = TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT;
    public static final int EMAIL = TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT;
    public static final int PHONE = TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT;
    public static final int SIGNATORY = 100;
    public static final int SHORT_TEXT = TextAreaValidationService.SHORT_TEXT_LIMIT;
    public static final int MEDIUM_TEXT = TextAreaValidationService.MEDIUM_TEXT_LIMIT;
    public static final int LONG_TEXT = TextAreaValidationService.LONG_TEXT_LIMIT;

    private BulkLimits() {
    }
}
