package uk.gov.hmcts.reform.pcs.ccd.service.preissuechecklist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PreIssueChecklistEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PreIssueChecklistRepository;

@Service
@RequiredArgsConstructor
public class PreIssueChecklistService {

    private final PreIssueChecklistRepository preIssueChecklistRepository;

    public void save(PreIssueChecklistEntity preIssueChecklistEntity) {
        preIssueChecklistRepository.save(preIssueChecklistEntity);
    }

}