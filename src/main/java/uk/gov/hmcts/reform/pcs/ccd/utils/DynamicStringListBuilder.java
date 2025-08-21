package uk.gov.hmcts.reform.pcs.ccd.utils;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.type.poc.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.type.poc.DynamicStringListElement;

import java.util.List;

public class DynamicStringListBuilder {

    public static <I extends Enum<I> & HasLabel> DynamicList build(List<I> items) {
        List<DynamicStringListElement> listItems = items.stream()
            .map(value -> DynamicStringListElement.builder().code(value.name()).label(value.getLabel()).build())
            .toList();

        return DynamicList.builder()
            .listItems(listItems)
            .build();
    }

}
