package uk.gov.hmcts.reform.pcs.ccd.retention;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.RetainAndDisposePolicy;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.repository.CcdCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.Set;

/**
 * Marks inactive unissued claims for disposal (HDPI-3404 AC01/AC02) and cleans PCS-owned data
 * after central CCD has deleted the case pointer (SDK retain-and-dispose step 6).
 */
@Slf4j
@Component
public class PcsRetainAndDisposePolicy implements RetainAndDisposePolicy {

    private final CcdCaseRepository ccdCaseRepository;
    private final CaseDeletionService caseDeletionService;
    private final FeatureToggleService featureToggleService;
    private final int discardAfterDays;
    private final int sqlLimit;

    public PcsRetainAndDisposePolicy(
        CcdCaseRepository ccdCaseRepository,
        CaseDeletionService caseDeletionService,
        FeatureToggleService featureToggleService,
        @Value("${expired-case-deletion.discard-after-days:30}") int discardAfterDays,
        @Value("${expired-case-deletion.sql-limit:200}") int sqlLimit
    ) {
        this.ccdCaseRepository = ccdCaseRepository;
        this.caseDeletionService = caseDeletionService;
        this.featureToggleService = featureToggleService;
        this.discardAfterDays = discardAfterDays;
        this.sqlLimit = sqlLimit;
    }

    @Override
    public Set<String> caseTypes() {
        return Set.of(CaseType.getCaseType());
    }

    @Override
    public List<Long> findCandidatesForDisposal() {
        if (!featureToggleService.isEnabled(FeatureFlag.DELETE_UNISSUED_CLAIM)) {
            log.debug("Retain-and-dispose candidates skipped; delete-unissued-claim flag is off");
            return List.of();
        }
        return ccdCaseRepository.findExpiredDraftCases(discardAfterDays, sqlLimit);
    }

    @Override
    public void dispose(long caseReference) {
        caseDeletionService.deleteServiceOwnedData(caseReference);
    }
}
