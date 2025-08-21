package uk.gov.hmcts.reform.pcs.ccd.utils;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;

public class DynamicStringListBuilder {

    public static <I extends Enum<I> & HasLabel> DynamicStringList build(List<I> items) {
        List<DynamicStringListElement> listItems = items.stream()
            .map(value -> DynamicStringListElement.builder().code(value.name()).label(value.getLabel()).build())
            .toList();

        return DynamicStringList.builder()
            .listItems(listItems)
            .build();
    }

}
