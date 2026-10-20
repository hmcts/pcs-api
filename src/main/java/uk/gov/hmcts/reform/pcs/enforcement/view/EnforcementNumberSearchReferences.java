package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.view.globalsearch.OtherCaseReferenceProvider;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnforcementNumberSearchReferences implements OtherCaseReferenceProvider {

    private final EnforcementOrderRepository enforcementOrderRepository;

    @Override
    public List<String> otherCaseReferences(long caseReference) {
        return enforcementOrderRepository.findEnforcementNumbersByCaseReference(caseReference);
    }
}
