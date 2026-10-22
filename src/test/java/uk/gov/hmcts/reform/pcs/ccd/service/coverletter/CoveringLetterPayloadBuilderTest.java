package uk.gov.hmcts.reform.pcs.ccd.service.coverletter;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.document.model.coverletter.CoveringLetterPayload;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.coverletter.CoveringLetterPayloadBuilder.YOURS_FAITHFULLY;
import static uk.gov.hmcts.reform.pcs.ccd.service.coverletter.CoveringLetterPayloadBuilder.YOURS_SINCERELY;

class CoveringLetterPayloadBuilderTest {

    private final CoveringLetterPayloadBuilder underTest = new CoveringLetterPayloadBuilder();

    @Test
    void shouldFillNamedPersonAndSplitCourtAddress() {
        PartyEntity recipient = PartyEntity.builder()
            .firstName("Jane").lastName("Doe").nameKnown(VerticalYesNo.YES).build();
        AddressUK address = AddressUK.builder()
            .addressLine1("Flat 2").addressLine2("1 High Street").addressLine3("Northside")
            .postTown("Leeds").county("West Yorkshire").postCode("LS1 1AA").build();

        CoveringLetterPayload payload = underTest.build(
            recipient, "Jane Doe", address, "1234-5678-9012-3456", "Sarah Freeman vs Jane Doe",
            court("Leeds County Court", "Y Lanfa, Trefechan, Aberystwyth", "LS1 3BG"),
            LocalDate.of(2026, 10, 15));

        assertThat(payload.getSignOff()).isEqualTo(YOURS_SINCERELY);
        assertThat(payload.getLetterDate()).isEqualTo("15 October 2026");
        assertThat(payload.getRecipientAddressLine2()).isEqualTo("1 High Street, Northside");
        assertThat(payload.getRecipientCounty()).isEqualTo("West Yorkshire");
        assertThat(payload.getCourtAddressLine1()).isEqualTo("Y Lanfa");
        assertThat(payload.getCourtAddressLine2()).isEqualTo("Trefechan, Aberystwyth");
    }

    @Test
    void shouldAddressOrganisationWithASingleCourtAddressLine() {
        PartyEntity recipient = PartyEntity.builder().orgName("Acme Ltd").build();
        AddressUK address = AddressUK.builder().addressLine1("1 High Street").postCode("LS1 1AA").build();

        CoveringLetterPayload payload = underTest.build(
            recipient, "Acme Ltd", address, "1234-5678-9012-3456", "Acme Ltd vs Jane Doe",
            court("Leeds County Court", "HAGLEY ROAD", "B16 8PE"), LocalDate.of(2026, 10, 15));

        assertThat(payload.getSignOff()).isEqualTo(YOURS_FAITHFULLY);
        assertThat(payload.getRecipientOrganisationName()).isEqualTo("Acme Ltd");
        assertThat(payload.getRecipientPersonalName()).isNull();
        assertThat(payload.getCourtAddressLine2()).isNull();
        assertThat(payload.getRecipientAddressLine2()).isNull();
    }

    private CourtVenue court(String courtName, String courtAddress, String postcode) {
        return new CourtVenue(
            null, null, null, null, null, null, null, null, null, null,
            courtAddress,
            postcode,
            null, null, null, null, null, null, null, null,
            courtName,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        );
    }
}
