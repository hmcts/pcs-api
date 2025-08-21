package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimEvent;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEventLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.roles.service.UserInfoService;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ClaimEventLogService {

    private final ClaimRepository claimRepository;
    private final UserInfoService userInfoService;

    public void writeEntry(UUID claimId, CounterClaimEvent counterClaimEvent, String notes) {
        ClaimEntity claimEntity = claimRepository.findById(claimId)
            .orElseThrow(() -> new ClaimNotFoundException(claimId));

        ClaimEventLogEntity claimEventLogEntity = new ClaimEventLogEntity();
        claimEventLogEntity.setEventName(counterClaimEvent != null ? counterClaimEvent.getLabel() : null);
        claimEventLogEntity.setNotes(notes);
        claimEventLogEntity.setCreated(Instant.now());

        String userEmail = userInfoService.getCurrentUserInfo().getSub();
        claimEventLogEntity.setInvokedBy(userEmail);

        claimEntity.addClaimEventLog(claimEventLogEntity);

        claimRepository.save(claimEntity);
    }


}
