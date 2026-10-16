package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.StatementOfTruthMapper;

@Service
@RequiredArgsConstructor
public class EnforcementStatementOfTruthService {

    private final StatementOfTruthMapper statementOfTruthMapper;
    private final StatementOfTruthRepository statementOfTruthRepository;

    @Transactional
    public void processStatementOfTruth(StatementOfTruthDetailsEnforcement statementOfTruthDetailsEnforcement,
                                        EnforcementOrderEntity enforcementOrderEntity) {

        StatementOfTruthEntity statementOfTruthEntity =
                statementOfTruthMapper.toEntity(statementOfTruthDetailsEnforcement);
        enforcementOrderEntity.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruthEntity));
    }
}
