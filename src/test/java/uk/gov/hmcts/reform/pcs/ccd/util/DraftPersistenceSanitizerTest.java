package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DraftPersistenceSanitizer.
 * Verifies pass-through behavior - actual sanitisation is tested in integration tests.
 */
class DraftPersistenceSanitizerTest {

    private DraftPersistenceSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new DraftPersistenceSanitizer();
    }

    @Test
    void shouldReturnNullWhenCaseDataIsNull() {
        PCSCase result = sanitizer.sanitize(null);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnSameObjectWhenCaseDataProvided() {
        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = sanitizer.sanitize(caseData);

        assertThat(result).isSameAs(caseData);
    }

    @Test
    void shouldHandlePartyWithNullFields() {
        Party party = Party.builder()
            .firstName(null)
            .lastName(null)
            .phoneNumber(null)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase result = sanitizer.sanitize(caseData);

        // Sanitizer is pass-through; null removal happens during serialization
        assertThat(result).isSameAs(caseData);
        assertThat(result.getPossessionClaimResponse().getParty()).isSameAs(party);
    }

    @Test
    void shouldHandlePartyWithMixedData() {
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Main Street")
            .postCode("SW1A 1AA")
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName(null)
            .phoneNumber("07123456789")
            .address(address)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        PCSCase result = sanitizer.sanitize(caseData);

        // Sanitizer is pass-through; verification of actual null removal
        // happens in integration tests with draftCaseDataObjectMapper
        assertThat(result).isSameAs(caseData);
        assertThat(result.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(result.getPossessionClaimResponse().getParty().getPhoneNumber()).isEqualTo("07123456789");
    }
}
