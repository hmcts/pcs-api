package uk.gov.hmcts.reform.pcs.ccd.config;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import uk.gov.hmcts.reform.pcs.ccd.pebble.CurrencyFilter;
import uk.gov.hmcts.reform.pcs.ccd.pebble.PlainTextFilter;
import java.util.HashMap;
import java.util.Map;

/** The filters the app's Pebble templates use: currency amounts, and text shown as typed. */
public class PebbleFiltersExtension extends AbstractExtension {

    @Override
    public Map<String, Filter> getFilters() {
        Map<String, Filter> filters = new HashMap<>();
        filters.put("currency", new CurrencyFilter());
        filters.put("plaintext", new PlainTextFilter());
        return filters;
    }
}
