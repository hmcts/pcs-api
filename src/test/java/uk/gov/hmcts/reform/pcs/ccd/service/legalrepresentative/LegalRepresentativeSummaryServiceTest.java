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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantPartyExtractor;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeSummaryServiceTest {

    @InjectMocks
    private LegalRepresentativeSummaryService legalRepresentativeSummaryService;

    @Mock
    private OrganisationService organisationService;

    @Mock
    private DefendantPartyExtractor defendantPartyExtractor;

    private static final String RESPOND_TO_CLAIM_MARKDOWN = """
        <h2 class="govuk-heading-m">What happens next</h2>
        <p>
        <a href="testUrl/case/${[CASE_REFERENCE]}/respond-to-claim/start-now"
        role="button"
        class="govuk-link govuk-link--no-visited-state">
        Respond to the claim</a>
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

    @BeforeEach
    void setUp() {
        legalRepresentativeSummaryService = new LegalRepresentativeSummaryService(organisationService,
                                                                                  defendantPartyExtractor);
        ReflectionTestUtils.setField(legalRepresentativeSummaryService, "frontendUrl",
                                     "testUrl");
    }

    @Test
    void handleLegalRepresentativeSummary_WithLinkedAndActiveAndNotUpdatedDetails_ReturnsUpdateDetailsMarkDown() {
        // given
        String organisationId = "org";
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation =
            LegalRepresentativeOrganisationEntity.builder()
            .organisationId(organisationId)
            .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                            .partyLegalRepresentativeOrganisationList(List.of(
                                                PartyLegalRepresentativeOrganisationEntity.builder()
                                                    .active(YesOrNo.YES)
                                                    .legalRepresentativeOrganisation(legalRepresentativeOrganisation)
                                                    .build()))
                                            .build());


        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEqualTo(UPDATE_DETAILS_MARKDOWN);
    }

    @Test
    void handleLegalRepresentativeSummary_WithLinkedAndActiveAndUpdatedDetails_ReturnsRespondMarkDown() {
        // given
        String organisationId = "org";
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation =
            LegalRepresentativeOrganisationEntity.builder()
                .organisationId(organisationId)
                .hasAmendedContactDetails(YesOrNo.YES)
                .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                              .partyLegalRepresentativeOrganisationList(List.of(
                                                  PartyLegalRepresentativeOrganisationEntity.builder()
                                                      .active(YesOrNo.YES)
                                                      .legalRepresentativeOrganisation(legalRepresentativeOrganisation)
                                                      .build()))
                                              .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEqualTo(RESPOND_TO_CLAIM_MARKDOWN);
    }

    @Test
    void handleLegalRepresentativeSummary_WithLinkedAndNotActive_ReturnsEmptyMarkDown() {
        // given
        String organisationId = "org";
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation =
            LegalRepresentativeOrganisationEntity.builder()
            .organisationId(organisationId)
            .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                              .partyLegalRepresentativeOrganisationList(List.of(
                                                  PartyLegalRepresentativeOrganisationEntity.builder()
                                                      .active(YesOrNo.NO)
                                                      .legalRepresentativeOrganisation(legalRepresentativeOrganisation)
                                                      .build()))
                                              .build());

        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithNotLinkedAndActive_ReturnsEmptyMarkDown() {
        // given
        String organisationId = "org";
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation =
            LegalRepresentativeOrganisationEntity.builder()
            .organisationId(organisationId + "1")
            .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                              .partyLegalRepresentativeOrganisationList(List.of(
                                                  PartyLegalRepresentativeOrganisationEntity.builder()
                                                      .active(YesOrNo.YES)
                                                      .legalRepresentativeOrganisation(legalRepresentativeOrganisation)
                                                      .build()))
                                              .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithNotLinkedAndNotActive_ReturnsEmptyMarkDown() {
        // given
        String organisationId = "org";
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation =
            LegalRepresentativeOrganisationEntity.builder()
            .organisationId(organisationId)
            .build();
        List<PartyEntity> parties = List.of(PartyEntity.builder()
                                              .partyLegalRepresentativeOrganisationList(List.of(
                                                  PartyLegalRepresentativeOrganisationEntity.builder()
                                                      .active(YesOrNo.NO)
                                                      .legalRepresentativeOrganisation(legalRepresentativeOrganisation)
                                                      .build()))
                                              .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();

        when(defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity)).thenReturn(parties);

        PCSCase pcsCase = PCSCase.builder().build();

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

}
