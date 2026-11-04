package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarrantReturnNotificationService {

    private final EnforcementNotificationSender sender;

    public boolean notifyClaimant(long caseReference, WarrantReturnName returnName, String additionalDetails) {
        // TODO HDPI-8424: "Warrant withdrawn" must also notify each defendant by their contact preference,
        // once the notice of eviction has been served on them.
        Optional<String> body = WarrantReturnClaimantEmails.body(returnName, additionalDetails);
        if (body.isEmpty()) {
            log.info("Warrant return {} on case {} does not notify the claimant", returnName, caseReference);
            return false;
        }
        return sender.send(caseReference, EnforcementEmail.WARRANT_RETURN, Map.of(
            EnforcementEmail.RETURN_NAME, returnName.getLabel(),
            EnforcementEmail.WARRANT_RETURN_BODY, body.get()));
    }
}
