package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PackRecipientResolverTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private ClaimPackSelector claimPackSelector;
    @Mock
    private DefencePackSelector defencePackSelector;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;
    @Mock
    private DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private PackRecipientResolver underTest;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity claimForm = DocumentEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity defenceForm = DocumentEntity.builder().id(UUID.randomUUID()).build();

    @Test
    @DisplayName("Returns no claim recipients when the case is not found")
    void shouldReturnNoClaimRecipientsWhenCaseNotFound() {
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.empty());

        assertThat(underTest.resolveClaimRecipients(CASE_ID)).isEmpty();
    }

    @Test
    @DisplayName("Resolves the claimant's name and own postal address for the claim pack")
    void shouldResolveClaimantNameAndAddress() {
        AddressEntity postalAddress = AddressEntity.builder().addressLine1("1 High Street").build();
        AddressUK addressUk = AddressUK.builder().addressLine1("1 High Street").build();
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(claimPackSelector.findClaimPackCandidates(pcsCase))
            .thenReturn(List.of(new ClaimPackCandidate(PartyRole.CLAIMANT, claimant, List.of(claimForm))));
        when(recipientAddressResolver.resolveDisplayName(claimant)).thenReturn("Acme Ltd");
        when(recipientAddressResolver.resolvePostalAddress(claimant, PartyRole.CLAIMANT, pcsCase.getPropertyAddress()))
            .thenReturn(postalAddress);
        when(addressMapper.toAddressUK(postalAddress)).thenReturn(addressUk);

        List<ResolvedRecipient> resolved = underTest.resolveClaimRecipients(CASE_ID);

        assertThat(resolved).singleElement().satisfies(recipient -> {
            assertThat(recipient.recipient()).isEqualTo(claimant);
            assertThat(recipient.letterType()).isEqualTo(LetterType.CLAIMANT_CLAIM_PACK);
            assertThat(recipient.recipientName()).isEqualTo("Acme Ltd");
            assertThat(recipient.address()).isEqualTo(addressUk);
            assertThat(recipient.documents()).containsExactly(claimForm);
        });
    }

    @Test
    @DisplayName("Resolves a defendant's defence pack via the correspondence address")
    void shouldResolveDefendantViaCorrespondenceAddress() {
        AddressUK addressUk = AddressUK.builder().addressLine1("42 Renters Way").build();
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(defencePackSelector.findDefencePackCandidates(pcsCase))
            .thenReturn(List.of(new DefencePackCandidate(PartyRole.DEFENDANT, defendant, List.of(defenceForm))));
        when(recipientAddressResolver.resolveDisplayName(defendant)).thenReturn("Bob Tenant");
        when(defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(defendant, pcsCase.getPropertyAddress()))
            .thenReturn(addressUk);

        List<ResolvedRecipient> resolved = underTest.resolveDefenceRecipients(CASE_ID);

        assertThat(resolved).singleElement().satisfies(recipient -> {
            assertThat(recipient.recipient()).isEqualTo(defendant);
            assertThat(recipient.letterType()).isEqualTo(LetterType.DEFENCE_PACK);
            assertThat(recipient.recipientName()).isEqualTo("Bob Tenant");
            assertThat(recipient.address()).isEqualTo(addressUk);
        });
    }

    @Test
    @DisplayName("Serves the counter-claim on the claimant using the claimant's own address")
    void shouldResolveClaimantForDefencePackViaOwnAddress() {
        AddressEntity postalAddress = AddressEntity.builder().addressLine1("1 Landlord Lane").build();
        AddressUK addressUk = AddressUK.builder().addressLine1("1 Landlord Lane").build();
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(defencePackSelector.findDefencePackCandidates(pcsCase))
            .thenReturn(List.of(new DefencePackCandidate(PartyRole.CLAIMANT, claimant, List.of(defenceForm))));
        when(recipientAddressResolver.resolveDisplayName(claimant)).thenReturn("Acme Ltd");
        when(recipientAddressResolver.resolvePostalAddress(claimant, PartyRole.CLAIMANT, pcsCase.getPropertyAddress()))
            .thenReturn(postalAddress);
        when(addressMapper.toAddressUK(postalAddress)).thenReturn(addressUk);

        List<ResolvedRecipient> resolved = underTest.resolveDefenceRecipients(CASE_ID);

        assertThat(resolved).singleElement().satisfies(recipient ->
            assertThat(recipient.address()).isEqualTo(addressUk));
    }
}
