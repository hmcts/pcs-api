package uk.gov.hmcts.reform.pcs.functional.config;

public enum CaseTypeEvents {
    CREATE_POSSESSION_CLAIM("createPossessionClaim"),
    RESUME_POSSESSION_CLAIM("resumePossessionClaim"),
    RESPOND_POSSESSION_CLAIM("respondPossessionClaim");

    private final String eventId;

    CaseTypeEvents(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}