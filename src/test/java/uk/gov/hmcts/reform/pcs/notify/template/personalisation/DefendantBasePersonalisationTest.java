package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefendantBasePersonalisation Tests")
class DefendantBasePersonalisationTest {

    @Test
    @DisplayName("Should map all fields and default null recipient names to empty string")
    void shouldMapFieldsAndDefaultNames() {
        Map<String, Object> map = DefendantBasePersonalisation.builder()
            .firstName(null)
            .lastName(null)
            .caseNumber("1234-5678-90")
            .claimantName("JANE SMITH")
            .primaryDefendantName("PERSONS UNKNOWN")
            .build()
            .toMap();

        assertThat(map)
            .containsEntry("firstName", "")
            .containsEntry("lastName", "")
            .containsEntry("caseNumber", "1234-5678-90")
            .containsEntry("claimantName", "JANE SMITH")
            .containsEntry("primaryDefendantName", "PERSONS UNKNOWN");
    }

    @Test
    @DisplayName("Should throw when a required field is null")
    void shouldThrowWhenRequiredFieldNull() {
        DefendantBasePersonalisation personalisation = DefendantBasePersonalisation.builder()
            .firstName("John")
            .lastName("Doe")
            .caseNumber(null)
            .claimantName("JANE SMITH")
            .primaryDefendantName("PERSONS UNKNOWN")
            .build();

        assertThatThrownBy(personalisation::toMap)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("caseNumber");
    }
}
