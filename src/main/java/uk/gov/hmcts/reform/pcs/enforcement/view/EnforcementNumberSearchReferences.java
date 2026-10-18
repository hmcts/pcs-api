package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.view.globalsearch.OtherCaseReferenceProvider;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.util.List;

/**
 * Makes a case findable in global search by any warrant or writ number given on it (HDPI-7677), whatever the
 * enforcement's status now. Numbers are given when a caseworker approves the application, in the same event
 * that re-indexes the case.
 */
@Component
@RequiredArgsConstructor
public class EnforcementNumberSearchReferences implements OtherCaseReferenceProvider {

    private final EnforcementOrderRepository enforcementOrderRepository;

    @Override
    public List<String> otherCaseReferences(long caseReference) {
        return enforcementOrderRepository.findEnforcementNumbersByCaseReference(caseReference);
    }
}
