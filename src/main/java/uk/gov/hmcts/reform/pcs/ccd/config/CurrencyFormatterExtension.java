package uk.gov.hmcts.reform.pcs.ccd.config;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import uk.gov.hmcts.reform.pcs.ccd.pebble.CurrencyFilter;
import java.util.HashMap;
import java.util.Map;

public class CurrencyFormatterExtension extends AbstractExtension {

    @Override
    public Map<String, Filter> getFilters() {
        Map<String, Filter> filters = new HashMap<>();
        filters.put("currency", new CurrencyFilter());
        return filters;
    }
}
