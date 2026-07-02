package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefencePackSenderTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private DefencePackSelector defencePackSelector;
    @Mock
    private DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private AccessCodeActivityLogService accessCodeActivityLogService;

    @InjectMocks
    private DefencePackSender underTest;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity defenceForm = DocumentEntity.builder().documentId(UUID.randomUUID()).build();

    @Test
    void doesNothingWhenCaseNotFound() {
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.empty());

        underTest.sendDefencePacks(CASE_ID);

        verifyNoInteractions(bulkPrintService, defencePackSelector);
    }

    @Test
    void sendsDefencePackAndRecordsTargetStatus() {
        AddressUK address = AddressUK.builder().addressLine1("42 Renters Way").build();
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(defencePackSelector.findDefencePackCandidates(pcsCase)).thenReturn(List.of(
            new DefencePackCandidate(defendant, List.of(defenceForm), ClaimActivityType.DEFENCE_PACK_PARTIALLY_SENT)));
        when(recipientAddressResolver.resolveDisplayName(defendant)).thenReturn("Bob Tenant");
        when(defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(defendant, pcsCase.getPropertyAddress()))
            .thenReturn(address);
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());

        underTest.sendDefencePacks(CASE_ID);

        verify(bulkPrintService).sendPack(
            pcsCase, defendant, LetterType.DEFENCE_PACK, "Bob Tenant", address, List.of(defenceForm));
        verify(accessCodeActivityLogService)
            .logSuccess(pcsCase, defendant, ClaimActivityType.DEFENCE_PACK_PARTIALLY_SENT);
    }

    @Test
    void recordsFailureWhenSendThrowsMissingAddress() {
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(defencePackSelector.findDefencePackCandidates(pcsCase)).thenReturn(List.of(
            new DefencePackCandidate(defendant, List.of(defenceForm), ClaimActivityType.DEFENCE_PACK_SENT)));
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any()))
            .thenThrow(new MissingPostalAddressException("no address"));

        underTest.sendDefencePacks(CASE_ID);

        verify(accessCodeActivityLogService).logFailure(pcsCase, defendant, ClaimActivityType.DEFENCE_PACK_SENT);
    }
}
