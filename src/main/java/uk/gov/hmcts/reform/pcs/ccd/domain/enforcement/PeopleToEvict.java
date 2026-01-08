package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

/**
 * Domain model for the "people who will be evicted" page.
 * Contains the selection for whether to evict everyone or specific people.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeopleToEvict {

    @CCD(
        label = "Do you want to evict everyone at the property?",
        hint = "This includes all of the defendants and anyone else living at the property"
    )
    private VerticalYesNo evictEveryone;

}

