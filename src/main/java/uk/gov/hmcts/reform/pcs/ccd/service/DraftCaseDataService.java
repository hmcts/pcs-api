package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;

import java.util.Optional;

@Service
@Slf4j
public class DraftCaseDataService {

    private final DraftCaseDataRepository draftCaseDataRepository;
    private final ObjectMapper objectMapper;

    public DraftCaseDataService(DraftCaseDataRepository draftCaseDataRepository,
                                @Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper) {
        this.draftCaseDataRepository = draftCaseDataRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<PCSCase> getUnsubmittedCaseData(long caseReference) {
        Optional<PCSCase> optionalCaseData = draftCaseDataRepository.findByCaseReference(caseReference)
            .map(DraftCaseDataEntity::getCaseData)
            .map(this::parseCaseDataJson)
            .map(this::setUnsubmittedDataFlag);

        optionalCaseData.ifPresent(x -> log.debug("Found draft case data for reference {}", caseReference));

        return optionalCaseData;
    }

    public boolean hasUnsubmittedCaseData(long caseReference) {
        return draftCaseDataRepository.existsByCaseReference(caseReference);
    }

    public void saveUnsubmittedCaseData(long caseReference, PCSCase caseData) {

        String caseDataJson = writeCaseDataJson(caseData);

        DraftCaseDataEntity draftCaseDataEntity = draftCaseDataRepository.findByCaseReference(
                caseReference)
            .map(existingDraft -> {
                existingDraft.setCaseData(caseDataJson);
                return existingDraft;
            }).orElseGet(() -> {
                DraftCaseDataEntity newDraft = new DraftCaseDataEntity();
                newDraft.setCaseReference(caseReference);
                newDraft.setCaseData(caseDataJson);
                return newDraft;
            });

        draftCaseDataRepository.save(draftCaseDataEntity);
    }

    public void deleteUnsubmittedCaseData(long caseReference) {
        draftCaseDataRepository.deleteByCaseReference(caseReference);
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
