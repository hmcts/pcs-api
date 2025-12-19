package uk.gov.hmcts.reform.pcs.ccd.pebble;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class CurrencyFilter implements Filter {

    private static final DecimalFormat STANDARD_DECIMAL_FORMAT = new DecimalFormat("£#,##0.00");
    private static final DecimalFormat WHOLE_POUND_FORMAT = new DecimalFormat("£#,##0");

    @Override
    public List<String> getArgumentNames() {
        return null;
    }

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context,
                        int lineNumber) {
        if (input == null) {
            return null;
        }

        if (!(input instanceof BigDecimal amount)) {
            throw new IllegalArgumentException(
                "CurrencyFilter expects a BigDecimal but received: " + input.getClass().getSimpleName()
            );
        }

        if (hasZeroPence(amount)) {
            return WHOLE_POUND_FORMAT.format(amount.stripTrailingZeros());
        }

        // Default: £#,##0.00
        return STANDARD_DECIMAL_FORMAT.format(amount);
    }

    private boolean hasZeroPence(BigDecimal amount) {
        BigDecimal stripped = amount.stripTrailingZeros();
        return stripped.scale() <= 0;
    }
}

