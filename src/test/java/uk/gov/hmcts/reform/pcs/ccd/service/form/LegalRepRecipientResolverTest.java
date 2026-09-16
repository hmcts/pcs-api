package uk.gov.hmcts.reform.pcs.ccd.service.form;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LegalRepRecipientResolverTest {

    private static final long CASE_REFERENCE = 1789561427702903L;
    private static final String ORGANISATION_ID = "XXUW0T9";
    private static final String FIRM_NAME = "Possession Claim Service Org1";

    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private ClaimPartyContactDetailsRepository claimPartyContactDetailsRepository;
    @Mock
    private FeatureToggleService featureToggleService;

    private LegalRepRecipientResolver underTest;

    private final PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final OrganisationEntity firm = OrganisationEntity.builder()
        .organisationId(ORGANISATION_ID)
        .organisationName(FIRM_NAME)
        .build();

    @BeforeEach
    void setUp() {
        underTest = new LegalRepRecipientResolver(organisationRepository, claimPartyContactDetailsRepository,
            new AddressMapper(new ModelMapper()), featureToggleService);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
    }

    @Test
    @DisplayName("Resolves the active firm's name and its contact address for the case")
    void shouldResolveFirmNameAndContactAddress() {
        stubActiveFirm();
        stubContactAddress(AddressEntity.builder()
            .addressLine1("Seventh Floor 102 Petty France")
            .postTown("London")
            .postcode("SW1H 9AJ")
            .build());

        Optional<LegalRepRecipientResolver.LegalRepAddressee> result = underTest.resolve(defendant, CASE_REFERENCE);

        assertThat(result).hasValueSatisfying(addressee -> {
            assertThat(addressee.name()).isEqualTo(FIRM_NAME);
            assertThat(addressee.address().getAddressLine1()).isEqualTo("Seventh Floor 102 Petty France");
            assertThat(addressee.address().getPostTown()).isEqualTo("London");
            assertThat(addressee.address().getPostCode()).isEqualTo("SW1H 9AJ");
        });
    }

    @Test
    @DisplayName("Resolves nothing when the defendant has no active legal representative")
    void shouldResolveNothingWhenNoActiveFirm() {
        when(organisationRepository.findByPartyLinkedToOrganisationAndCaseAndActive(defendant.getId(), CASE_REFERENCE))
            .thenReturn(Optional.empty());

        assertThat(underTest.resolve(defendant, CASE_REFERENCE)).isEmpty();
        verifyNoInteractions(claimPartyContactDetailsRepository);
    }

    @Test
    @DisplayName("Resolves nothing when the firm has no contact details for the case")
    void shouldResolveNothingWhenNoContactDetails() {
        stubActiveFirm();
        when(claimPartyContactDetailsRepository
            .findFirstByOrganisationOrganisationIdAndPcsCaseCaseReferenceOrderByIdDesc(ORGANISATION_ID, CASE_REFERENCE))
            .thenReturn(Optional.empty());

        assertThat(underTest.resolve(defendant, CASE_REFERENCE)).isEmpty();
    }

    @Test
    @DisplayName("Resolves nothing when the firm's contact details have no address")
    void shouldResolveNothingWhenContactDetailsHaveNoAddress() {
        stubActiveFirm();
        stubContactAddress(null);

        assertThat(underTest.resolve(defendant, CASE_REFERENCE)).isEmpty();
    }

    @Test
    @DisplayName("Resolves nothing when release 1.3 is disabled")
    void shouldResolveNothingWhenReleaseFlagDisabled() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);
        stubActiveFirm();

        assertThat(underTest.resolve(defendant, CASE_REFERENCE)).isEmpty();
        verifyNoInteractions(organisationRepository);
    }

    @Test
    @DisplayName("Resolves nothing when the legal representative journey is disabled")
    void shouldResolveNothingWhenLegalRepJourneyDisabled() {
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(false);
        when(organisationRepository.findByPartyLinkedToOrganisationAndCaseAndActive(any(), anyLong()))
            .thenReturn(Optional.of(firm));

        assertThat(underTest.resolve(defendant, CASE_REFERENCE)).isEmpty();
        verifyNoInteractions(organisationRepository);
    }

    private void stubActiveFirm() {
        when(organisationRepository.findByPartyLinkedToOrganisationAndCaseAndActive(defendant.getId(), CASE_REFERENCE))
            .thenReturn(Optional.of(firm));
    }

    private void stubContactAddress(AddressEntity address) {
        when(claimPartyContactDetailsRepository
            .findFirstByOrganisationOrganisationIdAndPcsCaseCaseReferenceOrderByIdDesc(ORGANISATION_ID, CASE_REFERENCE))
            .thenReturn(Optional.of(ClaimPartyContactDetailsEntity.builder().address(address).build()));
    }
}
