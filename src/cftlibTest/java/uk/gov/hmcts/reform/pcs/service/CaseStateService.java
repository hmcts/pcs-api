package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.awaitility.Awaitility;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.client.CcdClient;

import java.time.Duration;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CaseStateService {

    private final CcdClient ccdClient;

    public void waitForCaseState(long caseReference, State state, String authorisation) {
        Awaitility.await("Case state is " + state)
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofMillis(1000))
            .ignoreExceptions()
            .until(() -> caseStateMatches(caseReference, state, authorisation));
    }

    private boolean caseStateMatches(long caseReference, State state, String authorisaion) {
        CaseDetails caseDetails = ccdClient.getCaseDetails(caseReference, authorisaion);
        return Objects.equals(caseDetails.getState(), state.name());
    }

}
