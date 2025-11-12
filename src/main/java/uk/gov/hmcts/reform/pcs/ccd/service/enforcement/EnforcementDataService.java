package uk.gov.hmcts.reform.pcs.ccd.service.enforcement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcement.EnforcementDataRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.SubmittedEnforcementDataException;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EnforcementDataService {

    private final EnforcementDataRepository enforcementDataRepository;
    private final PcsCaseRepository pcsCaseRepository;
    private final ObjectMapper objectMapper;

    public EnforcementDataService(EnforcementDataRepository enforcementDataRepository,
                                  PcsCaseRepository pcsCaseRepository, ObjectMapper objectMapper) {
        this.enforcementDataRepository = enforcementDataRepository;
        this.pcsCaseRepository = pcsCaseRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<EnforcementOrder> retrieveSubmittedEnforcementData(UUID enforcementCaseId) {
        Optional<EnforcementOrder> enforcementData = enforcementDataRepository.findById(enforcementCaseId)
            .map(EnforcementDataEntity::getEnforcementData)
            .map(this::parseEnforcementDataJson);

        enforcementData.ifPresent(data ->
                log.debug("Found submitted Enforcement data for enforcementCaseId {}", enforcementCaseId));

        return enforcementData;
    }

    public void createEnforcementData(long caseReference, EnforcementOrder enforcementOrder) {

        String submittedEnfDataJson = writeSubmittedDataJson(enforcementOrder);

        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
                .orElseThrow(() -> new CaseNotFoundException(caseReference));
        EnforcementDataEntity enforcementDataEntity = new EnforcementDataEntity();
        enforcementDataEntity.setEnforcementData(submittedEnfDataJson);
        enforcementDataEntity.setPcsCase(pcsCaseEntity);

        enforcementDataRepository.save(enforcementDataEntity);
    }

    private EnforcementOrder parseEnforcementDataJson(String enforcementDataJson) {
        try {
            return objectMapper.readValue(enforcementDataJson, EnforcementOrder.class);
        } catch (JsonProcessingException e) {
            throw new SubmittedEnforcementDataException("Failed to read submitted Enforcement data JSON", e);
        }
    }

    private String writeSubmittedDataJson(EnforcementOrder enforcementData) {
        try {
            return objectMapper.writeValueAsString(enforcementData);
        } catch (JsonProcessingException e) {
            throw new SubmittedEnforcementDataException("Failed to write submitted Enforcement data");
        }
    }
}
