package uk.gov.hmcts.reform.pcs.ccd.pebble;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class CurrencyFilter implements Filter {

    @Override
    public List<String> getArgumentNames() {
        return null;
    }

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self,
                        EvaluationContext context, int lineNumber) {
        if (input == null || input == "0") {
            return "£0";
        }

        BigDecimal amount = input instanceof BigDecimal ? (BigDecimal) input
            : new BigDecimal(input.toString());

        // If < £1, always show 2 decimals (0.50)
        if (amount.compareTo(BigDecimal.ONE) < 0) {
            return new DecimalFormat("£0.00").format(amount);
        }

        // If whole pound, no decimals (150)
        BigDecimal stripped = amount.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            return new DecimalFormat("£#,##0").format(stripped);
        }

        return new DecimalFormat("£#,##0.00").format(amount);
    }
}
