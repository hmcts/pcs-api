package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enumerates all risk categories selectable on the enforcement risks selection page.
 * This includes the full set of 7 categories even though only the first three
 * currently lead to detail pages. Persisting the complete set allows us to
 * future‑proof navigation and data storage.
 */
@AllArgsConstructor
@Getter
public enum RiskCategory implements HasLabel {
    VIOLENT_OR_AGGRESSIVE("Violent or aggressive behaviour"),
    FIREARMS_POSSESSION("History of firearm possession"),
    CRIMINAL_OR_ANTISOCIAL("Criminal or antisocial behaviour"),
    VERBAL_OR_WRITTEN_THREATS("Verbal or written threats"),
    PROTEST_GROUP_MEMBER("Member of a group that protests evictions"),
    AGENCY_VISITS("Police or social services visits to the property"),
    AGGRESSIVE_ANIMALS("Aggressive dogs or other animals");

    private final String label;
}


