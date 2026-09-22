package uk.gov.hmcts.reform.pcs.ccd.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

class CurrentUserViewTest {

    private static final String CLAIMANT_ORG = "TCBS16B";
    private static final String SOLICITOR_ORG = "V9CJHGJ";

    private CurrentUserView underTest;
    private PCSCase pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new CurrentUserView();
        pcsCase = PCSCase.builder().build();
    }

    @Test
    void shouldNameTheClaimantRoleForTheClaimantsOwnOrganisation() {
        PcsCaseEntity caseEntity = caseWith(claimant(CLAIMANT_ORG, OrganisationProfile.LOCALAUTH_PROFILE));

        underTest.setCaseFields(pcsCase, caseEntity, CLAIMANT_ORG);

        assertThat(pcsCase.getCurrentUserGroupRole()).isEqualTo("claimant");
    }

    /**
     * A solicitor firm that brought the claim is on the claimant side, so it must not be named
     * the defendant's representative. pcs-frontend treats only {@code defendant-solicitor} as the
     * legal-rep journey, so this role has to stay distinct for it to fail closed.
     */
    @Test
    void shouldNameTheClaimantSolicitorRoleForASolicitorOrganisationThatBroughtTheClaim() {
        PcsCaseEntity caseEntity = caseWith(claimant(SOLICITOR_ORG, OrganisationProfile.SOLICITOR_PROFILE));

        underTest.setCaseFields(pcsCase, caseEntity, SOLICITOR_ORG);

        assertThat(pcsCase.getCurrentUserGroupRole()).isEqualTo("claimant-solicitor");
    }

    @Test
    void shouldNameTheDefendantSolicitorRoleForAnActiveRepresentative() {
        PcsCaseEntity caseEntity = caseWith(
            claimant(CLAIMANT_ORG, OrganisationProfile.LOCALAUTH_PROFILE),
            defendantRepresentedBy(SOLICITOR_ORG, YesOrNo.YES));

        underTest.setCaseFields(pcsCase, caseEntity, SOLICITOR_ORG);

        assertThat(pcsCase.getCurrentUserGroupRole()).isEqualTo("defendant-solicitor");
    }

    /**
     * A notice of change deactivates the outgoing firm's link. Treating that as current would keep
     * the previous representative in the defendant's journey.
     */
    @Test
    void shouldNotNameARoleForAnEndedRepresentation() {
        PcsCaseEntity caseEntity = caseWith(
            claimant(CLAIMANT_ORG, OrganisationProfile.LOCALAUTH_PROFILE),
            defendantRepresentedBy(SOLICITOR_ORG, YesOrNo.NO));

        underTest.setCaseFields(pcsCase, caseEntity, SOLICITOR_ORG);

        assertThat(pcsCase.getCurrentUserGroupRole()).isNull();
    }

    @Test
    void shouldNotNameARoleForAnUnrelatedOrganisation() {
        PcsCaseEntity caseEntity = caseWith(
            claimant(CLAIMANT_ORG, OrganisationProfile.LOCALAUTH_PROFILE),
            defendantRepresentedBy(SOLICITOR_ORG, YesOrNo.YES));

        underTest.setCaseFields(pcsCase, caseEntity, "SOMEONE-ELSE");

        assertThat(pcsCase.getCurrentUserGroupRole()).isNull();
    }

    @Test
    void shouldNotNameARoleForACallerWithNoOrganisation() {
        PcsCaseEntity caseEntity = caseWith(claimant(CLAIMANT_ORG, OrganisationProfile.LOCALAUTH_PROFILE));

        underTest.setCaseFields(pcsCase, caseEntity, null);

        assertThat(pcsCase.getCurrentUserGroupRole()).isNull();
    }

    private PcsCaseEntity caseWith(PartyEntity... parties) {
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        when(caseEntity.getParties()).thenReturn(new LinkedHashSet<>(List.of(parties)));
        return caseEntity;
    }

    private PartyEntity claimant(String organisationId, OrganisationProfile profile) {
        PartyEntity party = mock(PartyEntity.class);
        when(party.isClaimCreator()).thenReturn(true);
        when(party.getOrganisationId()).thenReturn(organisationId);
        when(party.getOrganisationProfileId()).thenReturn(profile.getId());
        when(party.getClaimPartyOrganisationList()).thenReturn(List.of());
        return party;
    }

    private PartyEntity defendantRepresentedBy(String organisationId, YesOrNo active) {
        OrganisationEntity organisation = mock(OrganisationEntity.class);
        when(organisation.getOrganisationId()).thenReturn(organisationId);
        when(organisation.getOrganisationProfileId())
            .thenReturn(OrganisationProfile.SOLICITOR_PROFILE.getId());

        ClaimPartyOrganisationEntity link = mock(ClaimPartyOrganisationEntity.class);
        when(link.getActive()).thenReturn(active);
        when(link.getOrganisation()).thenReturn(organisation);

        PartyEntity party = mock(PartyEntity.class);
        when(party.isClaimCreator()).thenReturn(false);
        when(party.getClaimPartyOrganisationList()).thenReturn(List.of(link));
        return party;
    }
}
