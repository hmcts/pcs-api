package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PossessiveNameService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimantInformationPageTest extends BasePageTest {

    @Mock
    private PossessiveNameService possessiveNameService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ClaimantInformationPage(possessiveNameService));
    }

    @Test
    void shouldUseFallbackNameForPossessiveWhenOrgNameNotFound() {
        // Given
        String fallbackName = "fallback name";
        String expectedPossessiveName = "possessive fallback name";

        PCSCase caseData = PCSCase.builder()
            .claimantInformation(
                ClaimantInformation.builder()
                    .orgNameFound(YesOrNo.NO)
                    .fallbackClaimantName(fallbackName)
                    .overriddenClaimantName("should be ignored")
                    .claimantName("should be ignored")
                    .build()
            )
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        when(possessiveNameService.applyApostrophe(fallbackName)).thenReturn(expectedPossessiveName);

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo(expectedPossessiveName);
    }

    @Test
    void shouldUseOverriddenNameForPossessiveWhenOrgNameNotCorrect() {
        // Given
        String overriddenName = "overridden name";
        String expectedPossessiveName = "possessive overridden name";

        PCSCase caseData = PCSCase.builder()
            .claimantInformation(
                ClaimantInformation.builder()
                    .orgNameFound(YesOrNo.YES)
                    .isClaimantNameCorrect(VerticalYesNo.NO)
                    .overriddenClaimantName(overriddenName)
                    .fallbackClaimantName("should be ignored")
                    .claimantName("should be ignored")
                    .build()
            )
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        when(possessiveNameService.applyApostrophe(overriddenName)).thenReturn(expectedPossessiveName);

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo(expectedPossessiveName);
    }

    @Test
    void shouldUseClaimantOrgNameWhenCorrect() {
        // Given
        String orgName = "org name";
        String expectedPossessiveName = "possessive org name";

        PCSCase caseData = PCSCase.builder()
            .claimantInformation(
                ClaimantInformation.builder()
                    .orgNameFound(YesOrNo.YES)
                    .isClaimantNameCorrect(VerticalYesNo.YES)
                    .claimantName(orgName)
                    .overriddenClaimantName("should be ignored")
                    .fallbackClaimantName("should be ignored")
                    .build()
            )
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        when(possessiveNameService.applyApostrophe(orgName)).thenReturn(expectedPossessiveName);

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo(expectedPossessiveName);
    }

}
