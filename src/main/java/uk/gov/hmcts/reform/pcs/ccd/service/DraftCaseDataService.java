package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;

import java.io.IOException;
import java.util.Optional;

@Service
@Slf4j
public class DraftCaseDataService {

    private final DraftCaseDataRepository draftCaseDataRepository;
    private final ObjectMapper objectMapper;
    private final DraftCaseJsonMerger draftCaseJsonMerger;

    public DraftCaseDataService(DraftCaseDataRepository draftCaseDataRepository,
                                @Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper,
                                DraftCaseJsonMerger draftCaseJsonMerger) {
        this.draftCaseDataRepository = draftCaseDataRepository;
        this.objectMapper = objectMapper;
        this.draftCaseJsonMerger = draftCaseJsonMerger;
    }

    public Optional<PCSCase> getUnsubmittedCaseData(long caseReference, EventId eventId) {
        Optional<PCSCase> optionalCaseData = draftCaseDataRepository
            .findByCaseReferenceAndEventId(caseReference, eventId)
                .map(DraftCaseDataEntity::getCaseData)
                .map(this::parseCaseDataJson)
                .map(this::setUnsubmittedDataFlag);

        optionalCaseData.ifPresent(x -> log.debug("Found draft case data for reference {}", caseReference));

        return optionalCaseData;
    }

    public boolean hasUnsubmittedCaseData(long caseReference, EventId eventId) {
        return draftCaseDataRepository.existsByCaseReferenceAndEventId(caseReference, eventId);
    }

    public <T> void patchUnsubmittedEventData(long caseReference, T eventData, EventId eventId) {

        String patchEventDataJson = writeCaseDataJson(eventData);

        DraftCaseDataEntity draftCaseDataEntity = draftCaseDataRepository.findByCaseReferenceAndEventId(
                caseReference, eventId)
            .map(existingDraft -> {
                existingDraft.setCaseData(mergeCaseDataJson(existingDraft.getCaseData(), patchEventDataJson));
                return existingDraft;
            }).orElseGet(() -> {
                DraftCaseDataEntity newDraft = new DraftCaseDataEntity();
                newDraft.setCaseReference(caseReference);
                newDraft.setCaseData(patchEventDataJson);
                newDraft.setEventId(eventId);
                return newDraft;
            });

        draftCaseDataRepository.save(draftCaseDataEntity);
    }

    private String mergeCaseDataJson(String baseCaseDataJson, String patchCaseDataJson) {
        try {
            return draftCaseJsonMerger.mergeJson(baseCaseDataJson, patchCaseDataJson);
        } catch (IOException e) {
            log.error("Unable to merge case data patch JSON", e);
            throw new UnsubmittedDataException("Failed to update draft case data", e);
        }
    }

    @Transactional
    public void deleteUnsubmittedCaseData(long caseReference, EventId eventId) {
        draftCaseDataRepository.deleteByCaseReferenceAndEventId(caseReference, eventId);
    }

    private PCSCase parseCaseDataJson(String caseDataJson) {
        try {
            return objectMapper.readValue(caseDataJson, PCSCase.class);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse draft case data JSON", e);
            throw new UnsubmittedDataException("Failed to read saved answers", e);
        }
    }

    private <T> String writeCaseDataJson(T caseData) {
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
