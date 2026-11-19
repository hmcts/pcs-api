package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.util.List;

/**
 * JSON for the staging row of a bulk claim. The PCSCase uses the draft case data mapper, as saved drafts do, so
 * a staged case is read back exactly as the claim event would see a draft.
 */
@Component
public class BulkClaimStagingSerializer {

    private final ObjectMapper draftMapper;
    private final ObjectMapper mapper;

    public BulkClaimStagingSerializer(@Qualifier("draftCaseDataObjectMapper") ObjectMapper draftMapper,
                                      ObjectMapper mapper) {
        this.draftMapper = draftMapper;
        this.mapper = mapper;
    }

    public String writeCase(PCSCase pcsCase) {
        return write(draftMapper, pcsCase);
    }

    public PCSCase readCase(String json) {
        return read(draftMapper, json, PCSCase.class);
    }

    public String writeExtras(BulkClaimExtras extras) {
        return write(mapper, extras);
    }

    public BulkClaimExtras readExtras(String json) {
        return read(mapper, json, BulkClaimExtras.class);
    }

    public String writeOrganisation(OrganisationDetailsResponse organisation) {
        return write(mapper, organisation);
    }

    public OrganisationDetailsResponse readOrganisation(String json) {
        return read(mapper, json, OrganisationDetailsResponse.class);
    }

    public String writeErrors(List<BulkValidationError> errors) {
        return errors == null || errors.isEmpty() ? null : write(mapper, errors);
    }

    public List<BulkValidationError> readErrors(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<BulkValidationError>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to read stored validation errors", e);
        }
    }

    private static String write(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to write bulk claim staging data", e);
        }
    }

    private static <T> T read(ObjectMapper objectMapper, String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to read bulk claim staging data", e);
        }
    }
}
