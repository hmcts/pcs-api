package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessCodeServiceTest {

    @Mock
    private PartyAccessCodeRepository partyAccessCodeRepo;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    private AccessCodeService underTest;

    @Captor
    private ArgumentCaptor<PartyAccessCodeEntity> argumentCaptor;

    @BeforeEach
    void setUp() {
        underTest = new AccessCodeService(partyAccessCodeRepo, pcsCaseRepository);
    }

    @Test
    void shouldCreatePartyAccessCodeEntity() {
        //Given
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);

        //When
        underTest.createPartyAccessCodeEntity(caseEntity, partyId);

        //Then
        verify(partyAccessCodeRepo).save(argumentCaptor.capture());
        PartyAccessCodeEntity savedEntity = argumentCaptor.getValue();

        assertThat(savedEntity.getPartyId()).isEqualTo(partyId);
        assertThat(savedEntity.getRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(savedEntity.getCode()).isNotNull().hasSize(12);
        assertThat(savedEntity.getCode()).matches("[ABCDEFGHJKLMNPRSTVWXYZ23456789]{12}");
    }

}
