package uk.gov.hmcts.reform.pcs.ccd.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Sanitizes draft data before persistence to prevent null fields from overwriting existing data.
 *
 * <p>Removes null fields by converting through a NON_NULL ObjectMapper. This ensures that
 * null fields are physically absent from the object before merge, preventing them from
 * overwriting existing draft values during Jackson merge operations.</p>
 */
@Component
@Slf4j
public class DraftPersistenceSanitizer {

    private final ObjectMapper nonNullMapper;

    public DraftPersistenceSanitizer() {
        this.nonNullMapper = JsonMapper.builder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

        // Register modules for Java 8 types and Lombok builders
        nonNullMapper.registerModules(
            new Jdk8Module(),
            new JavaTimeModule(),
            new ParameterNamesModule()
        );
    }

    /**
     * Sanitizes case data by removing null fields before draft persistence.
     * Converts the object through a NON_NULL mapper to strip out null fields.
     *
     * @param caseData the case data to sanitize
     * @return new PCSCase object without null fields, or null if input is null
     */
    public PCSCase sanitize(PCSCase caseData) {
        if (caseData == null) {
            return null;
        }

        log.debug("Sanitizing draft data - stripping null fields before merge");
        return nonNullMapper.convertValue(caseData, PCSCase.class);
    }
}
