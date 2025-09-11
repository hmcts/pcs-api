package uk.gov.hmcts.reform.pcs.ccd3.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.entity.UnsubmittedCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd3.repository.UnsubmittedCaseDataRepository;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;

import java.util.Optional;

@Service
@Slf4j
public class UnsubmittedCaseDataService {

    private final UnsubmittedCaseDataRepository unsubmittedCaseDataRepository;
    private final ObjectMapper objectMapper;

    public UnsubmittedCaseDataService(UnsubmittedCaseDataRepository unsubmittedCaseDataRepository,
                                      @Qualifier("unsubmittedCaseDataObjectMapper") ObjectMapper objectMapper) {
        this.unsubmittedCaseDataRepository = unsubmittedCaseDataRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<PCSCase> getUnsubmittedCaseData(long caseReference) {
        Optional<PCSCase> optionalCaseData = unsubmittedCaseDataRepository.findByCaseReference(caseReference)
            .map(UnsubmittedCaseDataEntity::getCaseData)
            .map(this::parseCaseDataJson)
            .map(this::setUnsubmittedDataFlag);

        optionalCaseData.ifPresent((x) -> log.debug("Found unsubmitted case data for reference {}", caseReference));

        return optionalCaseData;
    }

    public boolean hasUnsubmittedCaseData(long caseReference) {
        return unsubmittedCaseDataRepository.existsByCaseReference(caseReference);
    }

    public void saveUnsubmittedCaseData(long caseReference, PCSCase caseData) {

        String caseDataJson = writeCaseDataJson(caseData);

        UnsubmittedCaseDataEntity unsubmittedCaseDataEntity = unsubmittedCaseDataRepository.findByCaseReference(
                caseReference)
            .map(existingDraft -> {
                existingDraft.setCaseData(caseDataJson);
                return existingDraft;
            }).orElseGet(() -> {
                UnsubmittedCaseDataEntity newDraft = new UnsubmittedCaseDataEntity();
                newDraft.setCaseReference(caseReference);
                newDraft.setCaseData(caseDataJson);
                return newDraft;
            });

        unsubmittedCaseDataRepository.save(unsubmittedCaseDataEntity);
    }

    public void deleteUnsubmittedCaseData(long caseReference) {
        unsubmittedCaseDataRepository.deleteByCaseReference(caseReference);
    }

    private PCSCase parseCaseDataJson(String caseDataJson) {
        try {
            return objectMapper.readValue(caseDataJson, PCSCase.class);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse draft case data JSON", e);
            throw new UnsubmittedDataException("Failed to read saved answers", e);
        }
    }

    private String writeCaseDataJson(PCSCase caseData) {
        try {
            return objectMapper.writeValueAsString(caseData);
        } catch (JsonProcessingException e) {
            log.error("Unable to write draft case data JSON", e);
            throw new UnsubmittedDataException("Failed to save answers", e);
        }
    }

    private PCSCase setUnsubmittedDataFlag(PCSCase pcsCase) {
        pcsCase.setHasUnsubmittedCaseData(YesOrNo.YES);
        return pcsCase;
    }

}
