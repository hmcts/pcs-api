package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativeService;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class GenAppVisibilityServiceTest {

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();

    @Mock(strictness = Mock.Strictness.LENIENT)
    private LegalRepresentativeService legalRepresentativeService;

    private GenAppVisibilityService underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppVisibilityService(legalRepresentativeService);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = VerticalYesNo.class, names = {"NO"})
    void shouldTreatGenAppsWithNoticeAsVisible(VerticalYesNo isWithoutNotice) {
        // Given
        GenAppEntity genAppEntity1 = mock(GenAppEntity.class);
        when(genAppEntity1.getWithoutNotice()).thenReturn(isWithoutNotice);

        // When
        boolean genAppVisibleToUser = underTest.isGenAppVisibleToUser(genAppEntity1, CURRENT_USER_ID);

        // Then
        assertThat(genAppVisibleToUser).isTrue();
    }

    @ParameterizedTest
    @MethodSource("withoutNoticeScenarios")
    void shouldBaseVisibilityOfWithoutNoticeGenAppsOnUserIds(UUID applicantUserId,
                                                             LegalRepresentativeEntity legalRepresentativeEntity,
                                                             boolean expectedIsVisible) {
        // Given
        GenAppEntity genAppEntity1 = mock(GenAppEntity.class);
        PartyEntity applicantParty = mock(PartyEntity.class, withSettings().strictness(Strictness.LENIENT));
        when(genAppEntity1.getWithoutNotice()).thenReturn(VerticalYesNo.YES);
        when(genAppEntity1.getParty()).thenReturn(applicantParty);

        UUID applicantPartyId = UUID.randomUUID();
        when(applicantParty.getId()).thenReturn(applicantPartyId);
        when(applicantParty.getIdamId()).thenReturn(applicantUserId);

        when(legalRepresentativeService.getLegalRepresentativeForParty(applicantPartyId))
            .thenReturn(Optional.ofNullable(legalRepresentativeEntity));

        // When
        boolean genAppVisibleToUser = underTest.isGenAppVisibleToUser(genAppEntity1, CURRENT_USER_ID);

        // Then
        assertThat(genAppVisibleToUser).isEqualTo(expectedIsVisible);
    }

    private static Stream<Arguments> withoutNoticeScenarios() {
        UUID differentApplicantUserId = UUID.randomUUID();
        UUID legalRepUserIdForDifferentApplicant = UUID.randomUUID();

        return Stream.of(
            Arguments.argumentSet(
                "current user is applicant",
                CURRENT_USER_ID,
                null,
                true
            ),
            Arguments.argumentSet(
                "current user is LR of applicant",
                differentApplicantUserId,
                LegalRepresentativeEntity.builder().idamId(CURRENT_USER_ID).build(),
                true
            ),
            Arguments.argumentSet(
                "current user is not LR of applicant",
                differentApplicantUserId,
                LegalRepresentativeEntity.builder().idamId(legalRepUserIdForDifferentApplicant).build(),
                false
            )
        );
    }

}
