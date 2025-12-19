package uk.gov.hmcts.reform.pcs.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimantPartyFactoryTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "user@example.com";
    private static final String CLAIMANT_NAME = "Original Claimant Name";
    private static final String CLAIMANT_CONTACT_EMAIL = "original@example.com";
    private static final String CLAIMANT_CONTACT_PHONE_NUMBER = "1234567890";
    private static final String OVERRIDDEN_EMAIL = "overridden@example.com";
    private static final String OVERRIDDEN_CLAIMANT_NAME = "Overridden Claimant Name";
    private static final String ORGANISATION_NAME = "Original Organisation";

    private static final AddressUK OVERRIDDEN_ADDRESS = mock(AddressUK.class);

    @InjectMocks
    private ClaimantPartyFactory underTest;

    @Mock
    private PartyService partyService;

    private ClaimantPartyFactory.ClaimantPartyContext claimantPartyContext;

    @BeforeEach
    void beforeEach() {
        claimantPartyContext = new ClaimantPartyFactory.ClaimantPartyContext(USER_ID, USER_EMAIL);
    }

    @ParameterizedTest
    @MethodSource("provideClaimantNamingScenarios")
    void shouldCreatePartyWithCorrectNames(String overriddenName, String orgName, String expectedName,
                                           String expectedOrgName) {
        // Given
        ClaimantInformation claimantInfo = ClaimantInformation.builder().claimantName(CLAIMANT_NAME)
            .overriddenClaimantName(overriddenName).organisationName(orgName).build();
        ClaimantContactPreferences contactPreferences = ClaimantContactPreferences.builder()
            .overriddenClaimantContactAddress(OVERRIDDEN_ADDRESS)
            .overriddenClaimantContactEmail(OVERRIDDEN_EMAIL)
            .claimantContactEmail(CLAIMANT_CONTACT_EMAIL)
            .claimantContactPhoneNumber(CLAIMANT_CONTACT_PHONE_NUMBER)
            .build();

        PCSCase pcsCase = mock(PCSCase.class);
        when(pcsCase.getClaimantInformation()).thenReturn(claimantInfo);
        when(pcsCase.getClaimantContactPreferences()).thenReturn(contactPreferences);

        PartyEntity expectedPartyEntity = mock(PartyEntity.class);
        when(partyService.createPartyEntity(
            USER_ID, expectedName, null,
            expectedOrgName, OVERRIDDEN_EMAIL, OVERRIDDEN_ADDRESS,
            CLAIMANT_CONTACT_PHONE_NUMBER
        )).thenReturn(expectedPartyEntity);

        // When
        PartyEntity result = underTest.createAndPersistClaimantParty(pcsCase, claimantPartyContext);

        // Then
        assertThat(result).isSameAs(expectedPartyEntity);
        verify(partyService).createPartyEntity(
            USER_ID, expectedName, null, expectedOrgName,
            OVERRIDDEN_EMAIL, OVERRIDDEN_ADDRESS, CLAIMANT_CONTACT_PHONE_NUMBER
        );
    }

    private static Stream<Arguments> provideClaimantNamingScenarios() {
        return Stream.of(
            Arguments.of(
                OVERRIDDEN_CLAIMANT_NAME, ORGANISATION_NAME, OVERRIDDEN_CLAIMANT_NAME,
                OVERRIDDEN_CLAIMANT_NAME
            ),
            Arguments.of("", ORGANISATION_NAME, CLAIMANT_NAME, ORGANISATION_NAME),
            Arguments.of(null, ORGANISATION_NAME, CLAIMANT_NAME, ORGANISATION_NAME),
            Arguments.of("", "", CLAIMANT_NAME, CLAIMANT_NAME)
        );
    }

}


