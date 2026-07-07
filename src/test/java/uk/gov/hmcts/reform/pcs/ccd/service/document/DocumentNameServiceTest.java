package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentNameServiceTest {

    private static final String TEST_PARTY_LABEL = "Test Party Label";
    @Mock(strictness = LENIENT)
    private PartyService partyService;

    private DocumentNameService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentNameService(partyService);
    }

    @ParameterizedTest
    @MethodSource("genAppNamingScenarios")
    void shouldAddGenAppNumberAndPartyLabelToGenAppDocument(String originalFilename,
                                                            String expectedFilename) {
        // Given
        UUID applicantPartyId = UUID.randomUUID();
        GenAppEntity genAppEntity = GenAppEntity.builder()
            .rank(5)
            .build();

        ClaimEntity mainClaim = mock(ClaimEntity.class);
        when(partyService.getPartyLabel(mainClaim, applicantPartyId)).thenReturn(TEST_PARTY_LABEL);

        // When
        String updatedFilename
            = underTest.appendGenAppPostfix(originalFilename, genAppEntity, mainClaim, applicantPartyId);

        // Then
        assertThat(updatedFilename).isEqualTo(expectedFilename);
    }

    private static Stream<Arguments> genAppNamingScenarios() {
        return Stream.of(
            // Original filename, expected updated filename
            argumentSet("null filename", null, null),
            argumentSet("no extension", "sample", "sample GA5 - %s".formatted(TEST_PARTY_LABEL)),
            argumentSet("with extension", "sample.pdf", "sample GA5 - %s.pdf".formatted(TEST_PARTY_LABEL))
        );
    }

    @ParameterizedTest
    @MethodSource("partyNamingScenarios")
    void shouldAddPartyLabelWithoutGenAppNumber(String originalFilename,
                                                String expectedFilename) {
        // Given
        UUID applicantPartyId = UUID.randomUUID();
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        when(partyService.getPartyLabel(mainClaim, applicantPartyId)).thenReturn(TEST_PARTY_LABEL);

        // When
        String updatedFilename
            = underTest.appendPartyPostfix(originalFilename, mainClaim, applicantPartyId);

        // Then
        assertThat(updatedFilename).isEqualTo(expectedFilename);
    }

    @ParameterizedTest
    @MethodSource("defendantResponseNamingScenarios")
    void shouldAddPartyLabelToDefendantResponseDocument(String originalFilename,
                                                        String expectedFilename) {
        // Given
        UUID applicantPartyId = UUID.randomUUID();
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        when(partyService.getPartyLabel(mainClaim, applicantPartyId)).thenReturn(TEST_PARTY_LABEL);

        // When
        String updatedFilename
            = underTest.appendDefendantPostfix(originalFilename, mainClaim, applicantPartyId);

        // Then
        assertThat(updatedFilename).isEqualTo(expectedFilename);
    }

    private static Stream<Arguments> defendantResponseNamingScenarios() {
        return Stream.of(
            // original filename, expected updated filename
            argumentSet("null filename", null, null),
            argumentSet("no extension", "sample", "sample - %s".formatted(TEST_PARTY_LABEL)),
            argumentSet("with extension", "sample.pdf", "sample - %s.pdf".formatted(TEST_PARTY_LABEL))
        );
    }

    @ParameterizedTest
    @MethodSource("counterClaimNamingScenarios")
    void shouldAddPartyLabelToCounterClaimDocument(String originalFilename,
                                                   String expectedFilename) {
        // Given
        UUID partyId = UUID.randomUUID();
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn(TEST_PARTY_LABEL);

        // When
        String updatedFilename
            = underTest.appendCounterClaimPostfix(originalFilename, mainClaim, partyId);

        // Then
        assertThat(updatedFilename).isEqualTo(expectedFilename);
    }

    private static Stream<Arguments> counterClaimNamingScenarios() {
        return Stream.of(
            // Original filename, expected updated filename
            argumentSet("null filename", null, null),
            argumentSet("no extension", "sample", "sample - %s".formatted(TEST_PARTY_LABEL)),
            argumentSet("with extension", "sample.pdf", "sample - %s.pdf".formatted(TEST_PARTY_LABEL))
        );
    }

    private static Stream<Arguments> partyNamingScenarios() {
        return Stream.of(
            // Original filename, expected updated filename
            argumentSet("null filename", null, null),
            argumentSet("no extension", "sample", "sample - %s".formatted(TEST_PARTY_LABEL)),
            argumentSet("with extension", "sample.pdf", "sample - %s.pdf".formatted(TEST_PARTY_LABEL))
        );
    }

    @ParameterizedTest
    @MethodSource("dateNamingScenarios")
    void shouldAppendDateLabel(String originalFilename,
                               LocalDate localDate,
                               String expectedFilename) {
        // When
        String updatedFilename = underTest.appendDate(originalFilename, localDate);

        // Then
        assertThat(updatedFilename).isEqualTo(expectedFilename);
    }

    private static Stream<Arguments> dateNamingScenarios() {
        LocalDate testDate = LocalDate.of(2026, Month.MAY, 18);

        return Stream.of(
            // Original filename, date, expected updated filename
            argumentSet("null filename and date", null, null, null),
            argumentSet("null date", "sample.pdf", null, "sample.pdf"),
            argumentSet("with date, no extenstion", "sample", testDate, "sample 18052026"),
            argumentSet("with date and extenstion", "sample.pdf", testDate, "sample 18052026.pdf")
        );
    }

    @Test
    void shouldPropagatePartyNotFoundExceptionForPartyPostfix() {
        // Given
        UUID strayPartyId = UUID.randomUUID();

        PartyNotFoundException expectedException = mock(PartyNotFoundException.class);
        ClaimEntity mainClaim = ClaimEntity.builder().build();
        when(partyService.getPartyLabel(mainClaim, strayPartyId)).thenThrow(expectedException);


        // When / Then
        assertThatThrownBy(() -> underTest.appendPartyPostfix("statement.pdf", mainClaim, strayPartyId))
            .isEqualTo(expectedException);
    }

    @Test
    void shouldPropagatePartyNotFoundExceptionForGenApp() {
        PartyNotFoundException expectedException = mock(PartyNotFoundException.class);
        UUID applicantPartyId = UUID.randomUUID();

        ClaimEntity mainClaim = ClaimEntity.builder().build();
        GenAppEntity genAppEntity = GenAppEntity.builder().rank(1).build();
        when(partyService.getPartyLabel(mainClaim, applicantPartyId)).thenThrow(expectedException);

        assertThatThrownBy(() -> underTest.appendGenAppPostfix("file.pdf", genAppEntity, mainClaim, applicantPartyId))
            .isEqualTo(expectedException);
    }

    @Test
    void shouldPropagatePartyNotFoundExceptionForDefendantPostfix() {
        PartyNotFoundException expectedException = mock(PartyNotFoundException.class);
        UUID defendantPartyId = UUID.randomUUID();

        ClaimEntity mainClaim = ClaimEntity.builder().build();
        when(partyService.getPartyLabel(mainClaim, defendantPartyId)).thenThrow(expectedException);

        assertThatThrownBy(() -> underTest.appendDefendantPostfix("file.pdf", mainClaim, defendantPartyId))
            .isEqualTo(expectedException);
    }

    @Test
    void shouldThrowPartyNotFoundExceptionWhenPartyNotInClaimForCounterClaim() {
        PartyNotFoundException expectedException = mock(PartyNotFoundException.class);
        UUID partyId = UUID.randomUUID();

        ClaimEntity mainClaim = ClaimEntity.builder().build();
        when(partyService.getPartyLabel(mainClaim, partyId)).thenThrow(expectedException);

        assertThatThrownBy(() -> underTest.appendCounterClaimPostfix("file.pdf", mainClaim, partyId))
            .isEqualTo(expectedException);
    }
}
