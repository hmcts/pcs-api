package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseListViewTest {

    private CaseListView caseListView;

    @BeforeEach
    void setUp() {
        caseListView = new CaseListView();
    }

    @Test
    void shouldSetClaimantNames() {
        // Given
        ListValue<Party> claimantListValue = ListValue.<Party>builder()
            .value(Party.builder().orgName("claimant name").build())
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(List.of(claimantListValue))
            .build();

        // When
        caseListView.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getClaimantNames()).isEqualTo("claimant name");
    }

    @Test
    void shouldSetOneDefendantName() {
        // Given
        ListValue<Party> defendant1ListValue = ListValue.<Party>builder()
            .value(Party.builder().nameKnown(VerticalYesNo.YES).lastName("One").build())
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(defendant1ListValue))
            .build();

        // When
        caseListView.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getDefendantNames()).isEqualTo("One");
    }

    @Test
    void shouldSetTwoDefendantNames() {
        // Given
        ListValue<Party> defendant1ListValue = ListValue.<Party>builder()
            .value(Party.builder().nameKnown(VerticalYesNo.YES).lastName("One").build())
            .build();
        ListValue<Party> defendant2ListValue = ListValue.<Party>builder()
            .value(Party.builder().nameKnown(VerticalYesNo.YES).lastName("Two").build())
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(defendant1ListValue, defendant2ListValue))
            .build();

        // When
        caseListView.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getDefendantNames()).isEqualTo("One, Two");
    }

    @Test
    void shouldSetOthersInDefendantNamesWhenMoreThanTwoDefendants() {
        // Given
        ListValue<Party> defendant1ListValue = ListValue.<Party>builder()
            .value(Party.builder().nameKnown(VerticalYesNo.YES).lastName("One").build())
            .build();
        ListValue<Party> defendant2ListValue = ListValue.<Party>builder()
            .value(Party.builder().nameKnown(VerticalYesNo.YES).lastName("Two").build())
            .build();
        ListValue<Party> defendant3ListValue = ListValue.<Party>builder()
            .value(Party.builder().nameKnown(VerticalYesNo.YES).lastName("Three").build())
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(defendant1ListValue, defendant2ListValue, defendant3ListValue))
            .build();

        // When
        caseListView.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getDefendantNames()).isEqualTo("One, Two and Others");
    }

    @Test
    void shouldPersonsUnknownWhenDefendantNameNotKnown() {
        // Given
        ListValue<Party> defendant1ListValue = ListValue.<Party>builder()
            .value(Party.builder().nameKnown(VerticalYesNo.NO).lastName("One").build())
            .build();
        ListValue<Party> defendant2ListValue = ListValue.<Party>builder()
            .value(Party.builder().nameKnown(VerticalYesNo.NO).lastName("Two").build())
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(defendant1ListValue, defendant2ListValue))
            .build();

        // When
        caseListView.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getDefendantNames()).isEqualTo("Persons Unknown, Persons Unknown");
    }

    @Test
    void shouldSetDateIssuedStringWhenDateIssuedIsPresent() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .dateIssued(LocalDateTime.of(2026, 2, 1, 9, 0, 0))
            .build();

        // When
        caseListView.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getDateIssuedString()).isEqualTo("01/02/2026");
    }

    @Test
    void shouldSetPostCode() {
        // Given
        AddressUK address = AddressUK.builder()
            .postCode("postCode")
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(address)
            .build();

        // When
        caseListView.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getPostCode()).isEqualTo("postCode");
    }

    @Test
    void shouldNotSetFieldsIfNotPresent() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        caseListView.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getClaimantNames()).isNull();
        assertThat(pcsCase.getDefendantNames()).isNull();
        assertThat(pcsCase.getDateIssuedString()).isNull();
        assertThat(pcsCase.getPostCode()).isNull();
    }
}
