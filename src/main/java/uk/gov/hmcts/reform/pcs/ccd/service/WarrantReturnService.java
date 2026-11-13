package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.WarrantReturn;
import uk.gov.hmcts.reform.pcs.ccd.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@AllArgsConstructor
@Service
public class WarrantReturnService {

    private final WarrantRepository warrantRepository;

    public void addWarrantReturn(WarrantEntity warrantEntity, WarrantReturn warrantReturn) {
        WarrantReturnEntity warrantReturnEntity = createWarrantReturnEntity(warrantReturn);
        warrantEntity.addWarrantReturn(warrantReturnEntity);
        warrantRepository.save(warrantEntity);
    }

    private WarrantReturnEntity createWarrantReturnEntity(WarrantReturn warrantReturn) {
        return WarrantReturnEntity
            .builder()
            .returnType(warrantReturn.getReturnType())
            .outcome(warrantReturn.getEvictionOutcome())
            .returnName(warrantReturn.getReturnName())
            .additionalDetails(warrantReturn.getAdditionalDetails())
            .outcomeDate(toInstant(warrantReturn.getOutcomeDate()))
            .build();
    }

    private static Instant toInstant(LocalDateTime utcDateTime) {
        return utcDateTime == null ? null : utcDateTime.toInstant(ZoneOffset.UTC);
    }
}
