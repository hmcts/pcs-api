package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BasePersonalisation Tests")
class BasePersonalisationTest {

    private static BasePersonalisation.BasePersonalisationBuilder validBuilder() {
        return BasePersonalisation.builder()
            .firstName("John")
            .lastName("Doe")
            .caseNumber("1234-5678-90")
            .claimantName("JANE SMITH")
            .primaryDefendantName("JOHN DOE");
    }

    @Test
    @DisplayName("Should map all fields when present")
    void shouldMapAllFields() {
        Map<String, Object> map = validBuilder().build().toMap();

        assertThat(map)
            .containsEntry("firstName", "John")
            .containsEntry("lastName", "Doe")
            .containsEntry("caseNumber", "1234-5678-90")
            .containsEntry("claimantName", "JANE SMITH")
            .containsEntry("primaryDefendantName", "JOHN DOE");
    }

    @Test
    @DisplayName("Should default recipient name fields to empty string when null")
    void shouldDefaultNameFieldsToEmptyString() {
        Map<String, Object> map = validBuilder()
            .firstName(null)
            .lastName(null)
            .build()
            .toMap();

        assertThat(map)
            .containsEntry("firstName", "")
            .containsEntry("lastName", "");
    }

    @Test
    @DisplayName("Should throw when caseNumber is null")
    void shouldThrowWhenCaseNumberNull() {
        BasePersonalisation personalisation = validBuilder().caseNumber(null).build();

        assertThatThrownBy(personalisation::toMap)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("caseNumber");
    }

    @Test
    @DisplayName("Should throw when claimantName is null")
    void shouldThrowWhenClaimantNameNull() {
        BasePersonalisation personalisation = validBuilder().claimantName(null).build();

        assertThatThrownBy(personalisation::toMap)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("claimantName");
    }

    @Test
    @DisplayName("Should throw when primaryDefendantName is null")
    void shouldThrowWhenPrimaryDefendantNameNull() {
        BasePersonalisation personalisation = validBuilder().primaryDefendantName(null).build();

        assertThatThrownBy(personalisation::toMap)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("primaryDefendantName");
    }
}
