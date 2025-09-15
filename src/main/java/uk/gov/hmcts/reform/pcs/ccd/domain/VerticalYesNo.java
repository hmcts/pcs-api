package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * A YesNo enum that is not treated in a special way in ExUI
 * like {@link uk.gov.hmcts.ccd.sdk.type.YesOrNo} so it is
 * presented with the options vertically instead of horizontally.
 */
public enum VerticalYesNo implements HasLabel {

    YES("Yes"),
    NO("No");

    private final String label;

    VerticalYesNo(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public static VerticalYesNo from(Boolean booleanValue) {
        if (booleanValue == null) {
            return null;
        }
        return booleanValue ? YES : NO;
    }

    public boolean toBoolean() {
        return this == YES;
    }

}
<<<<<<< HEAD

=======
>>>>>>> 236678fa6a4f938f5565ade7af0e11e2cafd4ec7
