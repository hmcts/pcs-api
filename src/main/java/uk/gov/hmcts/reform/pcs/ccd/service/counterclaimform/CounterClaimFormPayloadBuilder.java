package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Builds the counter claim form payload from the {@code counter_claim} entity.
 *
 * <p>Smoke-test scaffold: maps only the title-block fields so the template renders end to end. The
 * remaining counter_claim mapping is added once the pipeline is proven.</p>
 */
@Service
public class CounterClaimFormPayloadBuilder {

    public CounterClaimFormPayload build(CounterClaimEntity counterClaim) {
        return CounterClaimFormPayload.builder()
            .referenceNumber(String.valueOf(counterClaim.getPcsCase().getCaseReference()))
            .issueDateSealed(toLocalDate(counterClaim.getClaimIssuedDate()))
            .submittedOn(toLocalDate(counterClaim.getClaimSubmittedDate()))
            .build();
    }

    private static LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }
}
