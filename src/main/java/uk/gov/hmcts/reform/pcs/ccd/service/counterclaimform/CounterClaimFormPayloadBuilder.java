package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

import java.time.Clock;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatUkDate;

@Service
public class CounterClaimFormPayloadBuilder {
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final Clock ukClock;

    public CounterClaimFormPayloadBuilder(CaseReferenceFormatter caseReferenceFormatter,
                                          @Qualifier("ukClock") Clock ukClock) {
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.ukClock = ukClock;
    }

    public CounterClaimFormPayload build(CounterClaimEntity counterClaim) {
        return CounterClaimFormPayload.builder()
            .referenceNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(
                counterClaim.getPcsCase().getCaseReference()))
            .issueDateSealed(formatUkDate(counterClaim.getClaimIssuedDate(), ukClock))
            .submittedOn(formatUkDate(counterClaim.getClaimSubmittedDate(), ukClock))
            .build();
    }
}
