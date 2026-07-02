package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.document.model.coversheet.CoversheetPayload;

import static org.assertj.core.api.Assertions.assertThat;

class CoversheetPayloadBuilderTest {

    private final CoversheetPayloadBuilder underTest = new CoversheetPayloadBuilder();

    @Test
    @DisplayName("Maps all address fields and flags")
    void shouldMapAllAddressFieldsAndFlags() {
        AddressUK address = AddressUK.builder()
            .addressLine1("Flat 2").addressLine2("1 High Street").addressLine3("Northside")
            .postTown("Leeds").county("West Yorkshire").postCode("LS1 1AA").build();

        CoversheetPayload payload = underTest.build("Jane Doe", address, "1234-5678-9012-3456");

        assertThat(payload.getRecipientName()).isEqualTo("Jane Doe");
        assertThat(payload.getCaseReference()).isEqualTo("1234-5678-9012-3456");
        assertThat(payload.getRecipientAddressLine1()).isEqualTo("Flat 2");
        assertThat(payload.getRecipientAddressLine2()).isEqualTo("1 High Street");
        assertThat(payload.getRecipientAddressLine3()).isEqualTo("Northside");
        assertThat(payload.getRecipientPostTown()).isEqualTo("Leeds");
        assertThat(payload.getRecipientCounty()).isEqualTo("West Yorkshire");
        assertThat(payload.getRecipientPostcode()).isEqualTo("LS1 1AA");
        assertThat(payload.isHasAddressLine2()).isTrue();
        assertThat(payload.isHasAddressLine3()).isTrue();
        assertThat(payload.isHasCounty()).isTrue();
    }

    @Test
    @DisplayName("Unsets optional flags when lines are blank")
    void shouldUnsetOptionalFlagsWhenLinesBlank() {
        AddressUK address = AddressUK.builder()
            .addressLine1("1 High Street").postTown("Leeds").postCode("LS1 1AA").build();

        CoversheetPayload payload = underTest.build("Jane Doe", address, "1234-5678-9012-3456");

        assertThat(payload.isHasAddressLine2()).isFalse();
        assertThat(payload.isHasAddressLine3()).isFalse();
        assertThat(payload.isHasCounty()).isFalse();
    }
}
