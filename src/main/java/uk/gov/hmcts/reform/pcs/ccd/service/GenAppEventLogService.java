package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppEvent;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEventLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.roles.service.UserInfoService;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class GenAppEventLogService {

    private final GenAppRepository genAppRepository;
    private final UserInfoService userInfoService;

    public void writeEntry(UUID genAppId, GenAppEvent genAppEvent, String notes) {
        GenAppEventLogEntity genAppEventLogEntity = new GenAppEventLogEntity();
        genAppEventLogEntity.setEventName(genAppEvent != null ? genAppEvent.getLabel() : null);
        genAppEventLogEntity.setNotes(notes);
        genAppEventLogEntity.setCreated(Instant.now());

        String userEmail = userInfoService.getCurrentUserInfo().getSub();
        genAppEventLogEntity.setInvokedBy(userEmail);

        GenAppEntity genAppEntity = genAppRepository.findById(genAppId)
            .orElseThrow(() -> new ClaimNotFoundException(genAppId));

        genAppEntity.addClaimEventLog(genAppEventLogEntity);

        genAppRepository.save(genAppEntity);
    }


}
