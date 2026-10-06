package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.ApplicationOutcome;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;

import java.util.Map;
import java.util.UUID;

/**
 * Records what an issued order on a general application decided about it, from the decision on the
 * order's form as court staff issued it. The application keeps the outcome and the order that decided it.
 */
@Component
@AllArgsConstructor
class ApplicationDecisions {

    /** The decision the make order form records for an application, in the form's values. */
    static final String DECISION = "application-decision";

    private static final Map<String, ApplicationOutcome> OUTCOMES = Map.of(
        "grant", ApplicationOutcome.GRANTED,
        "refuse", ApplicationOutcome.REFUSED,
        "strike-out", ApplicationOutcome.STRUCK_OUT,
        "list", ApplicationOutcome.LISTED,
        "other", ApplicationOutcome.OTHER
    );

    private final GenAppRepository genAppRepository;

    void record(PcsCaseEntity pcsCase, UUID genAppId, Map<String, Object> formData, UUID issuedOrderId) {
        GenAppEntity genApp = pcsCase.findGenApp(genAppId)
            .orElseThrow(() -> ExternalRejection.because("The order's application is no longer on the case"));
        genApp.setOutcome(outcome(formData));
        genApp.setDecidedByOrderId(issuedOrderId);
        genAppRepository.saveAndFlush(genApp);
    }

    static ApplicationOutcome outcome(Map<String, Object> formData) {
        ApplicationOutcome outcome = OUTCOMES.get(String.valueOf(formData.get(DECISION)));
        if (outcome == null) {
            throw ExternalRejection.because("Select what the order decides about the application");
        }
        return outcome;
    }
}
