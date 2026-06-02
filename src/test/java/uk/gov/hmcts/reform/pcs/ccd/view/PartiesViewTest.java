package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.LegalRepresentative;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyId;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartiesViewTest {

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Mock
    private UserInfo userInfo;

    private PCSCase pcsCase;
    private PartiesView underTest;

    @BeforeEach
    void setUp() {
        underTest = new PartiesView(securityContextService, modelMapper);
        pcsCase = PCSCase.builder().build();
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
    }

    @Test
    void shouldMapPartyForCitizenOwnParty()  {
        UUID currentUserId = UUID.randomUUID();
        stubCitizenUser(currentUserId);

        PartyEntity currentUserParty = buildParty(currentUserId, "Jane", "Doe", "Org A",
                                                  "jane@example.com", "07700000001");
        ClaimPartyEntity claimParty = buildClaimPartyEntity(currentUserParty, PartyRole.CLAIMANT);
        when(claimEntity.getClaimParties()).thenReturn(List.of(claimParty));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        List<ListValue<Party>> claimants = pcsCase.getAllClaimants();
        assertThat(claimants).hasSize(1);
        Party party = claimants.getFirst().getValue();
        assertThat(party.getFirstName()).isEqualTo("Jane");
        assertThat(party.getLastName()).isEqualTo("Doe");
        assertThat(party.getOrgName()).isEqualTo("Org A");
        assertThat(party.getEmailAddress()).isEqualTo("jane@example.com");
        assertThat(party.getPhoneNumber()).isEqualTo("07700000001");
    }

    @Test
    void shouldMapPartialPartyOfOtherCitizenUsers() {
        UUID currentUserId = UUID.randomUUID();
        stubCitizenUser(currentUserId);

        PartyEntity otherParty = buildParty(UUID.randomUUID(), "John", "Smith", "Org B",
                                            "john@example.com", "07700000002");
        ClaimPartyEntity claimParty = buildClaimPartyEntity(otherParty, PartyRole.DEFENDANT);
        when(claimEntity.getClaimParties()).thenReturn(List.of(claimParty));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        List<ListValue<Party>> defendants = pcsCase.getAllDefendants();
        assertThat(defendants).hasSize(1);
        Party party = defendants.getFirst().getValue();
        assertThat(party.getFirstName()).isEqualTo("John");
        assertThat(party.getLastName()).isEqualTo("Smith");
        assertThat(party.getOrgName()).isEqualTo("Org B");
        assertThat(party.getEmailAddress()).isNull();
        assertThat(party.getPhoneNumber()).isNull();
        assertThat(party.getDateOfBirth()).isNull();
        assertThat(party.getAddress()).isNull();
    }

    @Test
    void shouldMapAllParties() {
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of("caseworker-pcs"));

        PartyEntity claimant = buildParty(UUID.randomUUID(), "Alice", "A", null, null, null);
        PartyEntity defendant1 = buildParty(UUID.randomUUID(), "Bob", "B", null, null, null);
        PartyEntity defendant2 = buildParty(UUID.randomUUID(), "Carol", "C", null, null, null);
        PartyEntity underlessee1 = buildParty(UUID.randomUUID(), "Dave", "D", null, null, null);
        PartyEntity underlessee2 = buildParty(UUID.randomUUID(), "Eve", "E", null, null, null);

        when(claimEntity.getClaimParties()).thenReturn(List.of(
            buildClaimPartyEntity(claimant, PartyRole.CLAIMANT),
            buildClaimPartyEntity(defendant1, PartyRole.DEFENDANT),
            buildClaimPartyEntity(defendant2, PartyRole.DEFENDANT),
            buildClaimPartyEntity(underlessee1, PartyRole.UNDERLESSEE_OR_MORTGAGEE),
            buildClaimPartyEntity(underlessee2, PartyRole.UNDERLESSEE_OR_MORTGAGEE)
        ));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertThat(pcsCase.getAllClaimants()).hasSize(1);
        assertThat(pcsCase.getAllClaimants().getFirst().getId()).isEqualTo(claimant.getId().toString());
        assertThat(pcsCase.getAllClaimants().getFirst().getValue().getFirstName()).isEqualTo("Alice");

        assertThat(pcsCase.getAllDefendants()).hasSize(2);
        assertThat(pcsCase.getAllDefendants())
            .extracting(lv -> lv.getValue().getFirstName())
            .containsExactly("Bob", "Carol");

        assertThat(pcsCase.getAllDefendants())
            .extracting(lv -> lv.getValue().getLegalRepresentative())
            .containsExactly(null, null);

        assertThat(pcsCase.getAllUnderlesseeOrMortgagees()).hasSize(2);
        assertThat(pcsCase.getAllUnderlesseeOrMortgagees())
            .extracting(lv -> lv.getValue().getFirstName())
            .containsExactly("Dave", "Eve");
    }

    @Test
    void shouldMapLegalRepresentativeIfPresent() {
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of("caseworker-pcs"));

        AddressEntity addressEntity = AddressEntity.builder().build();
        LegalRepresentativeEntity legalRepresentativeEntity = LegalRepresentativeEntity.builder()
            .firstName("first")
            .lastName("last")
            .phone("phone")
            .email("email@test.com")
            .organisationName("org name")
            .address(addressEntity)
            .build();
        ClaimPartyLegalRepresentativeEntity claimPartyLegalRepresentative =
            ClaimPartyLegalRepresentativeEntity.builder()
                .legalRepresentative(legalRepresentativeEntity)
                .build();
        PartyEntity defendant = buildParty(UUID.randomUUID(), "Bob", "B", null, null, null);
        defendant.setClaimPartyLegalRepresentativeList(List.of(claimPartyLegalRepresentative));
        when(claimEntity.getClaimParties()).thenReturn(List.of(
            buildClaimPartyEntity(defendant, PartyRole.DEFENDANT)
        ));

        AddressUK address = AddressUK.builder().build();
        when(modelMapper.map(addressEntity, AddressUK.class)).thenReturn(address);

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertThat(pcsCase.getAllDefendants()).hasSize(1);
        LegalRepresentative legalRepresentative = pcsCase.getAllDefendants().getFirst()
            .getValue().getLegalRepresentative();
        assertThat(legalRepresentative.getFirstName()).isEqualTo("first");
        assertThat(legalRepresentative.getLastName()).isEqualTo("last");
        assertThat(legalRepresentative.getTelephoneNumber()).isEqualTo("phone");
        assertThat(legalRepresentative.getOrganisationName()).isEqualTo("org name");
        assertThat(legalRepresentative.getAddress()).isEqualTo(address);
    }

    @Test
    void shouldMapFullPartyDetailsWhenNonCitizenUser() {
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of("caseworker-pcs"));

        PartyEntity claimant = buildParty(UUID.randomUUID(), "Alice", "A", null, "alice@example.com", "07700000001");
        PartyEntity defendant = buildParty(UUID.randomUUID(), "Bob", "B", null, "bob@example.com", "07700000002");

        when(claimEntity.getClaimParties()).thenReturn(List.of(
            buildClaimPartyEntity(claimant, PartyRole.CLAIMANT),
            buildClaimPartyEntity(defendant, PartyRole.DEFENDANT)
        ));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party claimantParty = pcsCase.getAllClaimants().getFirst().getValue();
        assertThat(claimantParty.getEmailAddress()).isEqualTo("alice@example.com");
        assertThat(claimantParty.getPhoneNumber()).isEqualTo("07700000001");

        Party defendantParty = pcsCase.getAllDefendants().getFirst().getValue();
        assertThat(defendantParty.getEmailAddress()).isEqualTo("bob@example.com");
        assertThat(defendantParty.getPhoneNumber()).isEqualTo("07700000002");
    }

    private void stubCitizenUser(UUID userId) {
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
    }

    private PartyEntity buildParty(UUID idamId, String firstName, String lastName,
                                   String orgName, String email, String phone) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(idamId)
            .firstName(firstName)
            .lastName(lastName)
            .orgName(orgName)
            .emailAddress(email)
            .phoneNumber(phone)
            .build();
    }

    private ClaimPartyEntity buildClaimPartyEntity(PartyEntity partyEntity, PartyRole role) {
        ClaimPartyId id = new ClaimPartyId();
        id.setPartyId(partyEntity.getId());
        return ClaimPartyEntity.builder()
            .id(id)
            .party(partyEntity)
            .role(role)
            .build();
    }
}
