package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.StatementOfTruthMapper;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@RequiredArgsConstructor
public class EnforcementStatementOfTruthService {

    private final StatementOfTruthMapper statementOfTruthMapper;
    private final StatementOfTruthRepository statementOfTruthRepository;

    /**
     * Persists the statement of truth the claimant signed, if the application captured one. Its answers are
     * unwrapped into the application's own fields, so an application submitted without reaching the statement
     * of truth page carries answers that are all null rather than no statement of truth at all: nothing is
     * stored for it and enf_case.sot_id stays null.
     */
    @Transactional
    public void processStatementOfTruth(StatementOfTruthDetailsEnforcement statementOfTruthDetailsEnforcement,
                                        EnforcementOrderEntity enforcementOrderEntity) {
        if (statementOfTruthDetailsEnforcement == null) {
            return;
        }
        StatementOfTruthEntity statementOfTruthEntity =
                statementOfTruthMapper.toEntity(statementOfTruthDetailsEnforcement);
        if (isBlank(statementOfTruthEntity.getFullName())) {
            return;
        }
        enforcementOrderEntity.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruthEntity));
    }
}
