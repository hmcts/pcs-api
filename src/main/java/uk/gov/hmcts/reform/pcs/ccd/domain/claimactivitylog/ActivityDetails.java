package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

/**
 * Structured detail serialised into {@code claim_activity_log.details}. The shape is discriminated by the
 * row's {@link ClaimActivityType}: generation rows carry {@link GenerationDetails}, pack dispatch rows carry
 * {@link PackDetails}.
 */
public sealed interface ActivityDetails permits GenerationDetails, PackDetails {
}
