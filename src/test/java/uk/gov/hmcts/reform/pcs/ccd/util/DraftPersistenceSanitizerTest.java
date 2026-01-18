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
 * Verifies null field removal behavior before draft persistence.
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
    void shouldReturnNewObjectWhenCaseDataProvided() {
        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = sanitizer.sanitize(caseData);

        assertThat(result).isNotNull();
        assertThat(result).isNotSameAs(caseData);
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

        // Sanitizer creates new object without null fields
        assertThat(result).isNotSameAs(caseData);
        assertThat(result.getPossessionClaimResponse()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty()).isNotNull();
    }

    @Test
    void shouldPreserveNonNullFieldsAndStructure() {
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

        // Verify non-null fields are preserved
        assertThat(result.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(result.getPossessionClaimResponse().getParty().getPhoneNumber()).isEqualTo("07123456789");
        assertThat(result.getPossessionClaimResponse().getParty().getAddress()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty().getAddress().getAddressLine1())
            .isEqualTo("123 Main Street");
        assertThat(result.getPossessionClaimResponse().getParty().getAddress().getPostCode())
            .isEqualTo("SW1A 1AA");
    }
}
