package uk.gov.hmcts.reform.pcs.ccd.service.form;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import static org.assertj.core.api.Assertions.assertThat;

class RecipientAddressResolverTest {

    private final RecipientAddressResolver underTest = new RecipientAddressResolver();

    private final AddressEntity ownAddress = AddressEntity.builder().addressLine1("5 Tenant Road").build();
    private final AddressEntity propertyAddress = AddressEntity.builder().addressLine1("1 Property Street").build();

    @Test
    void shouldUseClaimantOwnAddress() {
        PartyEntity claimant = PartyEntity.builder().address(ownAddress).build();

        assertThat(underTest.resolvePostalAddress(claimant, PartyRole.CLAIMANT, propertyAddress))
            .isEqualTo(ownAddress);
    }

    @Test
    void shouldUseDefendantOwnAddressWhenKnownAndNotSameAsProperty() {
        PartyEntity defendant = PartyEntity.builder()
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.NO)
            .address(ownAddress)
            .build();

        assertThat(underTest.resolvePostalAddress(defendant, PartyRole.DEFENDANT, propertyAddress))
            .isEqualTo(ownAddress);
    }

    @Test
    void shouldFallBackToPropertyWhenDefendantAddressUnknown() {
        PartyEntity defendant = PartyEntity.builder().addressKnown(VerticalYesNo.NO).build();

        assertThat(underTest.resolvePostalAddress(defendant, PartyRole.DEFENDANT, propertyAddress))
            .isEqualTo(propertyAddress);
    }

    @Test
    void shouldFallBackToPropertyWhenDefendantAddressSameAsProperty() {
        PartyEntity defendant = PartyEntity.builder()
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.YES)
            .address(ownAddress)
            .build();

        assertThat(underTest.resolvePostalAddress(defendant, PartyRole.DEFENDANT, propertyAddress))
            .isEqualTo(propertyAddress);
    }

    @Test
    void shouldUseOrgNameForDisplayName() {
        PartyEntity party = PartyEntity.builder().orgName("Acme Housing Ltd").build();

        assertThat(underTest.resolveDisplayName(party)).isEqualTo("Acme Housing Ltd");
    }

    @Test
    void shouldUsePersonsUnknownWhenNameNotKnown() {
        PartyEntity party = PartyEntity.builder().nameKnown(VerticalYesNo.NO).build();

        assertThat(underTest.resolveDisplayName(party)).isEqualTo("Persons unknown");
    }

    @Test
    void shouldJoinFirstAndLastNameForDisplayName() {
        PartyEntity party = PartyEntity.builder()
            .nameKnown(VerticalYesNo.YES).firstName("Jane").lastName("Roe").build();

        assertThat(underTest.resolveDisplayName(party)).isEqualTo("Jane Roe");
    }
}
