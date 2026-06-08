package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchingPartyAccessCodeHashingServiceTest {

    @Mock
    private SimplePartyAccessCodeService cleartextImpl;
    @Mock
    private HashingPartyAccessCodeService hashedImpl;
    @Mock
    private FeatureToggleService featureToggle;

    @InjectMocks
    private DispatchingPartyAccessCodeHashingService underTest;

    @Test
    void shouldRouteEncodeToCleartextWhenFlagOff() {
        when(featureToggle.isAccessCodeHashingEnabled()).thenReturn(false);
        when(cleartextImpl.encodeForStorage("CODE")).thenReturn("CODE");

        assertThat(underTest.encodeForStorage("CODE")).isEqualTo("CODE");
        verifyNoInteractions(hashedImpl);
    }

    @Test
    void shouldRouteEncodeToHashedWhenFlagOn() {
        when(featureToggle.isAccessCodeHashingEnabled()).thenReturn(true);
        when(hashedImpl.encodeForStorage("CODE")).thenReturn("$2a$hashed");

        assertThat(underTest.encodeForStorage("CODE")).isEqualTo("$2a$hashed");
        verifyNoInteractions(cleartextImpl);
    }

    @Test
    void shouldAlwaysVerifyViaHashedImplAndIgnoreFlag() {
        PartyAccessCodeRepository repository = mock(PartyAccessCodeRepository.class);
        UUID caseId = UUID.randomUUID();
        PartyAccessCodeEntity entity = PartyAccessCodeEntity.builder().build();
        when(hashedImpl.findMatchingAccessCode(repository, caseId, "CODE")).thenReturn(Optional.of(entity));

        // Reads never consult the flag, so codes minted under either scheme keep verifying after a
        // flip in either direction. verifyNoInteractions(featureToggle) is the rollback-safety guard.
        assertThat(underTest.findMatchingAccessCode(repository, caseId, "CODE")).contains(entity);
        verify(hashedImpl).findMatchingAccessCode(repository, caseId, "CODE");
        verifyNoInteractions(cleartextImpl, featureToggle);
    }
}
