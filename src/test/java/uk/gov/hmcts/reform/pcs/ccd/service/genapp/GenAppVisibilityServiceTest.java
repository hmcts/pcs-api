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
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;

@ExtendWith(MockitoExtension.class)
class GenAppVisibilityServiceTest {

    private static final String ORG_ID = "org";

    @Mock(strictness = Mock.Strictness.LENIENT)
    private LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    private GenAppVisibilityService underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppVisibilityService(legalRepresentativeOrganisationRepository);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = GenAppState.class, mode = EnumSource.Mode.EXCLUDE, names = "GEN_APP_ISSUED")
    void shouldTreatGenAppsWithNoticeAsVisible(GenAppState state) {
        // Given
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getState()).thenReturn(state);

        // When
        boolean genAppVisibleToUser = underTest.isGenAppVisibleToUser(genAppEntity, ORG_ID);

        // Then
        assertThat(genAppVisibleToUser).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = VerticalYesNo.class, names = {"NO"})
    void shouldTreatGenAppsWithNoticeAsVisible(VerticalYesNo isWithoutNotice) {
        // Given
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getState()).thenReturn(GEN_APP_ISSUED);
        when(genAppEntity.getWithoutNotice()).thenReturn(isWithoutNotice);

        // When
        boolean genAppVisibleToUser = underTest.isGenAppVisibleToUser(genAppEntity, ORG_ID);

        // Then
        assertThat(genAppVisibleToUser).isTrue();
    }

    @ParameterizedTest
    @MethodSource("withoutNoticeScenarios")
    void shouldBaseVisibilityOfWithoutNoticeGenAppsOnUserIds(String organisationId,
                                                             boolean isLegalRepresentativeLinkedToPartyAndActive,
                                                             boolean expectedIsVisible) {
        // Given
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        PartyEntity applicantParty = mock(PartyEntity.class, withSettings().strictness(Strictness.LENIENT));
        when(genAppEntity.getState()).thenReturn(GEN_APP_ISSUED);
        when(genAppEntity.getWithoutNotice()).thenReturn(VerticalYesNo.YES);
        when(genAppEntity.getParty()).thenReturn(applicantParty);

        UUID applicantPartyId = UUID.randomUUID();
        when(applicantParty.getId()).thenReturn(applicantPartyId);
        when(applicantParty.getOrganisationId()).thenReturn(organisationId);

        when(legalRepresentativeOrganisationRepository
                 .isRepresentativeOrganisationLinkedToPartyAndActive(ORG_ID, applicantPartyId))
                .thenReturn(isLegalRepresentativeLinkedToPartyAndActive);

        // When
        boolean genAppVisibleToUser = underTest.isGenAppVisibleToUser(genAppEntity, ORG_ID);

        // Then
        assertThat(genAppVisibleToUser).isEqualTo(expectedIsVisible);
    }

    private static Stream<Arguments> withoutNoticeScenarios() {
        String differentOrganisationId = UUID.randomUUID().toString();

        return Stream.of(
            Arguments.argumentSet(
                "current user is applicant",
                ORG_ID,
                false, // isLegalRepresentativeLinkedToPartyAndActive
                true
            ),
            Arguments.argumentSet(
                "current user is LR of applicant",
                differentOrganisationId,
                true, // isLegalRepresentativeLinkedToPartyAndActive
                true
            ),
            Arguments.argumentSet(
                "current user is not LR of applicant",
                differentOrganisationId,
                false, // isLegalRepresentativeLinkedToPartyAndActive
                false
            )
        );
    }

}
