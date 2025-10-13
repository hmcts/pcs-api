package uk.gov.hmcts.reform.pcs.ccd.domain;

/**
 * Enumerates all risk categories selectable on the enforcement risks selection page.
 * This includes the full set of 7 categories even though only the first three
 * currently lead to detail pages. Persisting the complete set allows us to
 * future‑proof navigation and data storage.
 */
public enum RiskCategory {
    VIOLENT_OR_AGGRESSIVE,
    FIREARMS_POSSESSION,
    CRIMINAL_OR_ANTISOCIAL,
    VERBAL_OR_WRITTEN_THREATS,
    PROTEST_GROUP_MEMBER,
    AGENCY_VISITS,
    AGGRESSIVE_ANIMALS
}


