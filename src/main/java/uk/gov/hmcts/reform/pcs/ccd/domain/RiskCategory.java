package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.CCD;

public enum RiskCategory {

    @CCD(label = "Violent or aggressive behaviour")
    VIOLENT_OR_AGGRESSIVE,

    @CCD(label = "History of firearm possession")
    FIREARMS_POSSESSION,

    @CCD(label = "Criminal or antisocial behaviour")
    CRIMINAL_OR_ANTISOCIAL,

    @CCD(label = "Verbal or written threats")
    VERBAL_OR_WRITTEN_THREATS,

    @CCD(label = "Member of a group that protests evictions")
    PROTEST_GROUP_MEMBER,

    @CCD(label = "Police or social services visits to the property")
    AGENCY_VISITS,

    @CCD(label = "Aggressive dogs or other animals")
    AGGRESSIVE_ANIMALS
}


