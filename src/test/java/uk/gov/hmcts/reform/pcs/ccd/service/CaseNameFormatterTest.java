package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.NO;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.YES;

@ExtendWith(MockitoExtension.class)
class CaseNameFormatterTest {

    private static final String CLAIMANT_NAME = "Freeman";
    private static final String DEFENDANT_LAST_NAME = "Jackson";
    private static final String CLAIMANT_ORGANISATION_NAME = "Treetops Housing";

    @Mock
    private PCSCase pcsCase;
    @Mock
    private ListValue<Party> defendantPartyListValue;
    @Mock
    private ListValue<Party> claimantListValue;
    @Mock
    private Party defendantParty;
    @Mock
    private Party claimantParty;

    private CaseNameFormatter underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseNameFormatter();
    }

    @Nested
    @DisplayName("Format case name from PcsCase")
    class FormatFromPcsCaseTests {

        @Test
        void shouldFormatCaseNameWhenClaimantIsOrgAndDefendantIsKnown() {
            // Given
            when(pcsCase.getAllClaimants()).thenReturn(List.of(claimantListValue));
            when(pcsCase.getAllDefendants()).thenReturn(List.of(defendantPartyListValue));
            when(defendantPartyListValue.getValue()).thenReturn(defendantParty);
            when(claimantListValue.getValue()).thenReturn(claimantParty);
            when(claimantParty.getOrgName()).thenReturn(CLAIMANT_ORGANISATION_NAME);
            when(defendantParty.getNameKnown()).thenReturn(YES);
            when(defendantParty.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

            // When
            String caseName = underTest.formatCaseName(pcsCase);

            // Then
            assertThat(caseName).isEqualTo("Treetops Housing vs Jackson");
        }

        @Test
        void shouldFormatCaseNameWhenClaimantIsCitizenAndDefendantIsKnown() {
            // Given
            when(pcsCase.getAllClaimants()).thenReturn(List.of(claimantListValue));
            when(pcsCase.getAllDefendants()).thenReturn(List.of(defendantPartyListValue));
            when(defendantPartyListValue.getValue()).thenReturn(defendantParty);
            when(claimantListValue.getValue()).thenReturn(claimantParty);
            when(claimantParty.getLastName()).thenReturn(CLAIMANT_NAME);
            when(defendantParty.getNameKnown()).thenReturn(YES);
            when(defendantParty.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

            // When
            String caseName = underTest.formatCaseName(pcsCase);

            // Then
            assertThat(caseName).isEqualTo("Freeman vs Jackson");
        }

        @Test
        void shouldFormatCaseNameWhenClaimantIsOrgAndMultipleDefendants() {
            // Given
            when(pcsCase.getAllClaimants()).thenReturn(List.of(claimantListValue));
            when(pcsCase.getAllDefendants()).thenReturn(List.of(defendantPartyListValue, defendantPartyListValue));
            when(defendantPartyListValue.getValue()).thenReturn(defendantParty);
            when(claimantListValue.getValue()).thenReturn(claimantParty);
            when(claimantParty.getOrgName()).thenReturn(CLAIMANT_ORGANISATION_NAME);
            when(defendantParty.getNameKnown()).thenReturn(YES);
            when(defendantParty.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

            // When
            String caseName = underTest.formatCaseName(pcsCase);

            // Then
            assertThat(caseName).isEqualTo("Treetops Housing vs Jackson and Others");
        }

        @Test
        void shouldFormatCaseNameWhenClaimantIsCitizenAndDefendantUnknown() {
            // Given
            when(pcsCase.getAllClaimants()).thenReturn(List.of(claimantListValue));
            when(pcsCase.getAllDefendants()).thenReturn(List.of(defendantPartyListValue));
            when(claimantListValue.getValue()).thenReturn(claimantParty);
            when(defendantPartyListValue.getValue()).thenReturn(defendantParty);
            when(claimantParty.getLastName()).thenReturn(CLAIMANT_NAME);
            when(defendantParty.getNameKnown()).thenReturn(NO);

            // When
            String caseName = underTest.formatCaseName(pcsCase);

            // Then
            assertThat(caseName).isEqualTo("Freeman vs persons unknown");
        }
    }

    @Nested
    @DisplayName("Format case name from lists of Party")
    class FormatFromListOfPartyTests {

        @Test
        void shouldFormatCaseNameWhenClaimantIsOrgAndDefendantIsKnown() {
            // Given
            when(claimantParty.getOrgName()).thenReturn(CLAIMANT_ORGANISATION_NAME);
            when(defendantParty.getNameKnown()).thenReturn(YES);
            when(defendantParty.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

            // When
            String caseName = underTest.formatCaseName(List.of(claimantParty), List.of(defendantParty));

            // Then
            assertThat(caseName).isEqualTo("Treetops Housing vs Jackson");
        }

        @Test
        void shouldFormatCaseNameWhenClaimantIsCitizenAndDefendantIsKnown() {
            //Given
            when(claimantParty.getLastName()).thenReturn(CLAIMANT_NAME);
            when(defendantParty.getNameKnown()).thenReturn(YES);
            when(defendantParty.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

            //When
            String caseName = underTest.formatCaseName(List.of(claimantParty), List.of(defendantParty));

            // Then
            assertThat(caseName).isEqualTo("Freeman vs Jackson");
        }

        @Test
        void shouldFormatCaseNameWhenClaimantIsOrgAndMultipleDefendants() {
            //Given
            when(claimantParty.getOrgName()).thenReturn(CLAIMANT_ORGANISATION_NAME);
            when(defendantParty.getNameKnown()).thenReturn(YES);
            when(defendantParty.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

            //When
            String caseName = underTest.formatCaseName(List.of(claimantParty), List.of(defendantParty, defendantParty));

            // Then
            assertThat(caseName).isEqualTo("Treetops Housing vs Jackson and Others");
        }

        @Test
        void shouldFormatCaseNameWhenClaimantIsCitizenAndDefendantUnknown() {
            //Given
            when(claimantParty.getLastName()).thenReturn(CLAIMANT_NAME);
            when(defendantParty.getNameKnown()).thenReturn(NO);

            //When
            String caseName = underTest.formatCaseName(List.of(claimantParty), List.of(defendantParty, defendantParty));

            // Then
            assertThat(caseName).isEqualTo("Freeman vs persons unknown");
        }
    }
}
