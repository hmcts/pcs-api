package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantPartyExtractor;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeSummaryServiceTest {

    @InjectMocks
    private LegalRepresentativeSummaryService legalRepresentativeSummaryService;

    @Mock
    private DefendantPartyExtractor defendantPartyExtractor;

    @Mock
    private FeatureToggleService featureToggleService;

    private static final String RESPOND_TO_CLAIM_MARKDOWN = """
        <h2 class="govuk-heading-m">What happens next</h2>
        <p>
        <a href="testUrl/case/${[CASE_REFERENCE]}/respond-to-claim/start-now"
        role="button"
        class="govuk-link govuk-link--no-visited-state">
        Respond to the claim</a>.
        </p>
        """;

    private static final String UPDATE_DETAILS_MARKDOWN = """
        <h2 class="govuk-heading-m">What happens next</h2>
        <p>You must
        <a href="/cases/case-details/${[CASE_REFERENCE]}/trigger/legalRepresentativeContactDetails"
        role="button"
        class="govuk-link govuk-link--no-visited-state">
        update the legal representative details for the case</a>
        before</p>
        <p>responding so you can receive updates and notifications
        about the case.
        </p>
        """;

    private static final String ORGANISATION_ID = "organisation";

    @BeforeEach
    void setUp() {
        legalRepresentativeSummaryService = new LegalRepresentativeSummaryService(defendantPartyExtractor,
                                                                                 featureToggleService);
        ReflectionTestUtils.setField(legalRepresentativeSummaryService, "frontendUrl",
                                     "testUrl");

        lenient().when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);
        lenient().when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
    }

    @Test
    void handleLegalRepresentativeSummary_WithLinkedAndActiveAndNotUpdatedDetails_ReturnsUpdateDetailsMarkDown() {
        // given
        Long caseRef = 1L;
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseRef)
            .build();
        OrganisationEntity organisation =
            OrganisationEntity.builder()
            .organisationId(ORGANISATION_ID)
                .claimPartyContactDetails(
                    List.of(
                        ClaimPartyContactDetailsEntity
                            .builder()
                            .pcsCase(pcsCaseEntity)
                            .contactDetailsCorrectConfirmation(YesOrNo.NO)
                            .build()
                    ))
            .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                            .claimPartyOrganisationList(List.of(
                                                ClaimPartyOrganisationEntity.builder()
                                                    .active(YesOrNo.YES)
                                                    .organisation(organisation)
                                                    .build()))
                                            .build());

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.CASE_ISSUED, ORGANISATION_ID);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEqualTo(UPDATE_DETAILS_MARKDOWN);
    }

    @Test
    void handleLegalRepresentativeSummary_WithLinkedAndActiveAndUpdatedDetails_ReturnsRespondMarkDown() {
        // given
        long caseRef = 1L;
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseRef)
            .build();

        OrganisationEntity organisation =
            OrganisationEntity.builder()
                .organisationId(ORGANISATION_ID)
                .claimPartyContactDetails(
                    List.of(
                        ClaimPartyContactDetailsEntity
                            .builder()
                            .pcsCase(pcsCaseEntity)
                            .contactDetailsCorrectConfirmation(YesOrNo.YES)
                            .build()
                    ))
                .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                              .claimPartyOrganisationList(List.of(
                                                  ClaimPartyOrganisationEntity.builder()
                                                      .active(YesOrNo.YES)
                                                      .organisation(organisation)
                                                      .build()))
                                              .build());

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.CASE_ISSUED, ORGANISATION_ID);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEqualTo(RESPOND_TO_CLAIM_MARKDOWN);
    }

    @Test
    void handleLegalRepresentativeSummary_WithLinkedAndActiveAndNotCaseIssued_ReturnsEmptyRespondMarkDown() {
        // given
        long caseRef = 1L;
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseRef)
            .build();

        OrganisationEntity legalRepresentativeOrg =
            OrganisationEntity.builder()
                .organisationId(ORGANISATION_ID)
                .claimPartyContactDetails(
                    List.of(
                        ClaimPartyContactDetailsEntity
                            .builder()
                            .pcsCase(pcsCaseEntity)
                            .contactDetailsCorrectConfirmation(YesOrNo.YES)
                            .build()
                    ))
                .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                                .claimPartyOrganisationList(List.of(
                                                    ClaimPartyOrganisationEntity.builder()
                                                        .active(YesOrNo.YES)
                                                        .organisation(legalRepresentativeOrg)
                                                        .build()))
                                                .build());

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.PENDING_CASE_ISSUED, ORGANISATION_ID);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithNonCaseLinkedState_ReturnsEmptyRespondMarkDown() {
        // given
        String organisationId = "org";
        OrganisationEntity organisation =
            OrganisationEntity.builder()
                .organisationId(organisationId)
                .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                                .claimPartyOrganisationList(List.of(
                                                    ClaimPartyOrganisationEntity.builder()
                                                        .active(YesOrNo.YES)
                                                        .organisation(organisation)
                                                        .build()))
                                                .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.PENDING_CASE_ISSUED, organisationId);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithLinkedAndNotActive_ReturnsEmptyMarkDown() {
        // given
        OrganisationEntity organisation =
            OrganisationEntity.builder()
            .organisationId(ORGANISATION_ID)
            .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                              .claimPartyOrganisationList(List.of(
                                                  ClaimPartyOrganisationEntity.builder()
                                                      .active(YesOrNo.NO)
                                                      .organisation(organisation)
                                                      .build()))
                                              .build());

        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();
        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.CASE_ISSUED, ORGANISATION_ID);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithNotLinkedAndActive_ReturnsEmptyMarkDown() {
        // given
        OrganisationEntity organisation =
            OrganisationEntity.builder()
            .organisationId(ORGANISATION_ID + "1")
            .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                              .claimPartyOrganisationList(List.of(
                                                  ClaimPartyOrganisationEntity.builder()
                                                      .active(YesOrNo.YES)
                                                      .organisation(organisation)
                                                      .build()))
                                              .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();
        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.CASE_ISSUED, ORGANISATION_ID);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithNotLinkedAndNotActive_ReturnsEmptyMarkDown() {
        // given
        OrganisationEntity organisation =
            OrganisationEntity.builder()
            .organisationId(ORGANISATION_ID)
            .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                              .claimPartyOrganisationList(List.of(
                                                  ClaimPartyOrganisationEntity.builder()
                                                      .active(YesOrNo.NO)
                                                      .organisation(organisation)
                                                      .build()))
                                              .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();
        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.CASE_ISSUED, ORGANISATION_ID);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithNoContactDetails_ReturnsUpdateDetailsMarkDown() {
        // given
        long caseRef = 1L;
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseRef)
            .build();

        OrganisationEntity organisation =
            OrganisationEntity.builder()
                .organisationId(ORGANISATION_ID)
                .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                                .claimPartyOrganisationList(List.of(
                                                    ClaimPartyOrganisationEntity.builder()
                                                        .active(YesOrNo.YES)
                                                        .organisation(organisation)
                                                        .build()))
                                                .build());

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.CASE_ISSUED, ORGANISATION_ID);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEqualTo(UPDATE_DETAILS_MARKDOWN);
    }

    @Test
    void handleLegalRepresentativeSummary_WithCuiRespondToClaimLrDisabled_ReturnsEmptyMarkDown() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(false);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        PCSCase pcsCase = PCSCase.builder().build();

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.CASE_ISSUED, "org");

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithRelease1dot2Disabled_ReturnsEmptyMarkDown() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(false);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        PCSCase pcsCase = PCSCase.builder().build();

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity,
                                                                           State.CASE_ISSUED, "org");

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

}
