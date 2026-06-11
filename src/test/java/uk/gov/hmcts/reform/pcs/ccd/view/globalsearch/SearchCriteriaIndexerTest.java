package uk.gov.hmcts.reform.pcs.ccd.view.globalsearch;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.SearchCriteria;
import uk.gov.hmcts.ccd.sdk.type.SearchParty;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseTabView;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCriteriaIndexerTest {

    private final SearchCriteriaIndexer underTest = new SearchCriteriaIndexer();

    @Test
    void shouldIndexPartyDetailsAsSearchParty() {
        // Given
        LocalDate dateOfBirth = LocalDate.of(1990, 1, 2);
        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Doe")
            .emailAddress("jane@example.com")
            .dateOfBirth(dateOfBirth)
            .address(AddressUK.builder()
                         .addressLine1("1 Party Street")
                         .postCode("PA1 2RT")
                         .build())
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .singleElement()
            .satisfies(searchParty -> {
                assertThat(searchParty.getName()).isEqualTo("Jane Doe");
                assertThat(searchParty.getEmailAddress()).isEqualTo("jane@example.com");
                assertThat(searchParty.getAddressLine1()).isEqualTo("1 Party Street");
                assertThat(searchParty.getPostcode()).isEqualTo("PA1 2RT");
                assertThat(searchParty.getDateOfBirth()).isEqualTo(dateOfBirth);
            });
    }

    @Test
    void shouldOmitBlankNamePartsWhenBuildingPartyName() {
        // Given - only a last name is known
        Party party = Party.builder()
            .firstName("  ")
            .lastName("Doe")
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - blank parts are dropped, leaving just the known name
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .singleElement()
            .extracting(SearchParty::getName)
            .isEqualTo("Doe");
    }

    @Test
    void shouldIndexUnknownNameWhenNoNamePartsKnown() {
        // Given - neither first nor last name known
        Party party = Party.builder().build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - a placeholder name is indexed so the party remains searchable
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .singleElement()
            .extracting(SearchParty::getName)
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
    }

    @Test
    void shouldIndexUnknownNameWhenNameIsNotKnown() {
        // Given - a party explicitly flagged as having no known name
        Party party = Party.builder()
            .nameKnown(VerticalYesNo.NO)
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - the placeholder name is indexed so the unnamed party is searchable
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .singleElement()
            .extracting(SearchParty::getName)
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
    }

    @Test
    void shouldUseOrgNameAsSearchPartyNameWhenPresent() {
        // Given - an organisation party
        Party party = Party.builder().orgName("Acme Ltd").build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .singleElement()
            .extracting(SearchParty::getName)
            .isEqualTo("Acme Ltd");
    }

    @Test
    void shouldPreferOrgNameOverIndividualNameWhenBothPresent() {
        // Given - a party with both an org name and individual name parts
        Party party = Party.builder()
            .orgName("Acme Ltd")
            .firstName("Jane")
            .lastName("Doe")
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - the org name takes precedence
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .singleElement()
            .extracting(SearchParty::getName)
            .isEqualTo("Acme Ltd");
    }

    @Test
    void shouldFallBackToIndividualNameWhenOrgNameIsBlank() {
        // Given - a blank org name but a known individual name
        Party party = Party.builder()
            .orgName("  ")
            .firstName("Jane")
            .lastName("Doe")
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - the blank org name is ignored and the individual name is used
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .singleElement()
            .extracting(SearchParty::getName)
            .isEqualTo("Jane Doe");
    }

    @Test
    void shouldHandlePartyWithNoAddress() {
        // Given - a party with no address
        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Doe")
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - address-derived fields are left null
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .singleElement()
            .satisfies(searchParty -> {
                assertThat(searchParty.getAddressLine1()).isNull();
                assertThat(searchParty.getPostcode()).isNull();
            });
    }

    @Test
    void shouldUsePropertyAddressForPartyWhenAddressIsSameAsProperty() {
        // Given - a party whose address is the same as the property, so it carries no address of its
        // own (only the flag). The property address must be resolved onto the party's SearchParty so a
        // combined name + postcode search matches within a single object.
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .addressSameAsProperty(VerticalYesNo.YES)
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("2 Second Avenue")
                                 .postCode("W3 7RX")
                                 .build())
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - the party SearchParty carries both the name and the property postcode
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .anySatisfy(searchParty -> {
                assertThat(searchParty.getName()).isEqualTo("John Doe");
                assertThat(searchParty.getAddressLine1()).isEqualTo("2 Second Avenue");
                assertThat(searchParty.getPostcode()).isEqualTo("W3 7RX");
            });
    }

    @Test
    void shouldUsePartyOwnAddressWhenAddressIsNotSameAsProperty() {
        // Given - a party with its own address that differs from the property
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .addressSameAsProperty(VerticalYesNo.NO)
            .address(AddressUK.builder()
                         .addressLine1("1 Party Street")
                         .postCode("PA1 2RT")
                         .build())
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("2 Second Avenue")
                                 .postCode("W3 7RX")
                                 .build())
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - the party keeps its own address, not the property address
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .anySatisfy(searchParty -> {
                assertThat(searchParty.getName()).isEqualTo("John Doe");
                assertThat(searchParty.getAddressLine1()).isEqualTo("1 Party Street");
                assertThat(searchParty.getPostcode()).isEqualTo("PA1 2RT");
            });
    }

    @Test
    void shouldIndexPropertyAddressAsExtraSearchParty() {
        // Given - no parties, just a property to be repossessed
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("1 Property Street")
                                 .postCode("PR1 2PT")
                                 .build())
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - the property is added as a SearchParty so its postcode is searchable
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .singleElement()
            .satisfies(searchParty -> {
                assertThat(searchParty.getAddressLine1()).isEqualTo("1 Property Street");
                assertThat(searchParty.getPostcode()).isEqualTo("PR1 2PT");
            });
    }

    @Test
    void shouldIndexBothPartiesAndPropertyAddress() {
        // Given
        Party party = Party.builder().firstName("Jane").lastName("Doe").build();
        PCSCase pcsCase = PCSCase.builder()
            .parties(ListValueUtils.wrapListItems(List.of(party)))
            .propertyAddress(AddressUK.builder().postCode("PR1 2PT").build())
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then - both the party and the property appear as search parties
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .extracting(SearchParty::getPostcode)
            .containsExactlyInAnyOrder(null, "PR1 2PT");
    }

    @Test
    void shouldNotIndexPropertyWhenAddressIsNull() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then
        assertThat(searchCriteria.getParties()).isEmpty();
    }

    @Test
    void shouldNotIndexPropertyWhenPostcodeIsNull() {
        // Given - a property address with no postcode
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(AddressUK.builder().addressLine1("1 Property Street").build())
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then
        assertThat(searchCriteria.getParties()).isEmpty();
    }

    @Test
    void shouldNotIndexPropertyWhenPostcodeIsBlank() {
        // Given - a property address with a blank postcode
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(AddressUK.builder().postCode("   ").build())
            .build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then
        assertThat(searchCriteria.getParties()).isEmpty();
    }

    @Test
    void shouldReturnEmptySearchPartiesWhenNoPartiesOrProperty() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        SearchCriteria searchCriteria = underTest.buildSearchCriteria(pcsCase);

        // Then
        assertThat(searchCriteria.getParties()).isEmpty();
    }

    @Test
    void shouldNotAddPropertyToTheCasePartiesList() {
        // Given - a property address and an existing real party
        Party party = Party.builder().firstName("Jane").lastName("Doe").build();
        List<ListValue<Party>> originalParties = ListValueUtils.wrapListItems(List.of(party));
        PCSCase pcsCase = PCSCase.builder()
            .parties(originalParties)
            .propertyAddress(AddressUK.builder().postCode("PR1 2PT").build())
            .build();

        // When
        underTest.buildSearchCriteria(pcsCase);

        // Then - the property is only added to the search criteria, not the real parties list
        assertThat(pcsCase.getParties()).containsExactlyElementsOf(originalParties);
    }
}
