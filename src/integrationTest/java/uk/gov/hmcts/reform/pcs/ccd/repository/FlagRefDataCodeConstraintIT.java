package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("flag_ref_data - one row per flag code")
class FlagRefDataCodeConstraintIT extends AbstractPostgresContainerIT {

    @Autowired
    private FlagRefDataRepository flagRefDataRepository;

    @Test
    @DisplayName("rejects a second row for a flag code already held")
    void rejectsDuplicateFlagCode() {
        flagRefDataRepository.saveAndFlush(flagRefData("RA0035", "Video hearing"));

        assertThatThrownBy(() ->
            flagRefDataRepository.saveAndFlush(flagRefData("RA0035", "Video hearing (duplicate)")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("allows a row for each distinct flag code")
    void allowsDistinctFlagCodes() {
        flagRefDataRepository.saveAndFlush(flagRefData("RA0012", "Braille documents"));
        flagRefDataRepository.saveAndFlush(flagRefData("PF0015", "Language Interpreter"));

        assertThat(flagRefDataRepository.findByFlagCode("RA0012")).isPresent();
        assertThat(flagRefDataRepository.findByFlagCode("PF0015")).isPresent();
    }

    private FlagRefDataEntity flagRefData(String flagCode, String flagName) {
        return FlagRefDataEntity.builder()
            .flagCode(flagCode)
            .flagName(flagName)
            .hearingRelevant(true)
            .availableExternally(true)
            .build();
    }
}
