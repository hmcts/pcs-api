package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleRequest;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleDocumentSelector;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobContext;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.util.UUID;

/**
 * Compiles a case bundle job's request when the worker runs it, not when it was queued, so the
 * bundle reflects every document on the case at that moment, including ones added while the job
 * waited.
 */
@Component
@ConditionalOnProperty(prefix = "ccd.bundling.job", name = "enabled")
public class CaseBundleSelector implements BundleDocumentSelector {

    private final PcsCaseRepository pcsCaseRepository;
    private final CaseBundleRequestFactory requestFactory;
    private final TransactionTemplate readOnly;

    public CaseBundleSelector(PcsCaseRepository pcsCaseRepository,
                              CaseBundleRequestFactory requestFactory,
                              PlatformTransactionManager transactionManager) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.requestFactory = requestFactory;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    @Override
    public BundleRequest select(BundleJobContext context) {
        UUID caseId = CaseBundleScope.caseId(context.parameters());
        return readOnly.execute(status -> requestFactory.build(
            context.externalId(),
            pcsCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalStateException("No case " + caseId + " to bundle"))
        ));
    }
}
