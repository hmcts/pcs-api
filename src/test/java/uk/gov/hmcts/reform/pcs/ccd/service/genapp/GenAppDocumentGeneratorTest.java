package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.Party;
import uk.gov.hmcts.reform.pcs.document.model.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.document.model.genapp.GenAppFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class GenAppDocumentGeneratorTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final LocalDate FIXED_CURRENT_DATE = LocalDate.of(2026, 4, 27);

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private DocAssemblyService docAssemblyService;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private CaseNameFormatter caseNameFormatter;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private Clock ukClock;

    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private CitizenGenAppRequest citizenGenAppRequest;
    @Mock
    private GenAppEntity genAppEntity;
    @Mock
    private PartyEntity applicantPartyEntity;
    @Captor
    private ArgumentCaptor<FormPayload> formPayloadArgumentCaptor;

    private GenAppDocumentGenerator underTest;

    @BeforeEach
    void setUp() {
        stubUKClock();
        stubCaseData();

        underTest = new GenAppDocumentGenerator(pcsCaseService, partyService, securityContextService,
                                                docAssemblyService, addressMapper, addressFormatter,
                                                caseNameFormatter, modelMapper, ukClock);
    }

    private void stubUKClock() {
        when(ukClock.instant()).thenReturn(FIXED_CURRENT_DATE
                                               .atTime(10, 35)
                                               .atZone(UK_ZONE_ID)
                                               .toInstant());

        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);
    }

    private void stubCaseData() {
        ClaimEntity mainClaimEntity = mock(ClaimEntity.class);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));

        UUID applicantUserId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(applicantUserId);

        ClaimPartyEntity claimantClaimPartyEntity = mock(ClaimPartyEntity.class);
        PartyEntity claimantPartyEntity = mock(PartyEntity.class);
        when(claimantClaimPartyEntity.getRole()).thenReturn(PartyRole.CLAIMANT);
        when(claimantClaimPartyEntity.getParty()).thenReturn(claimantPartyEntity);

        ClaimPartyEntity applicantClaimPartyEntity = mock(ClaimPartyEntity.class);
        applicantPartyEntity = mock(PartyEntity.class);
        when(applicantPartyEntity.getIdamId()).thenReturn(applicantUserId);

        when(applicantClaimPartyEntity.getRole()).thenReturn(PartyRole.DEFENDANT);
        when(applicantClaimPartyEntity.getParty()).thenReturn(applicantPartyEntity);

        when(mainClaimEntity.getClaimParties())
            .thenReturn(List.of(claimantClaimPartyEntity, applicantClaimPartyEntity));
        when(partyService.getPartyEntityByIdamId(applicantUserId, CASE_REFERENCE)).thenReturn(applicantPartyEntity);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
    }

    @Test
    void shouldSetCaseReferenceInFormPayload() {
        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getCaseReference()).isEqualTo(Long.toString(CASE_REFERENCE));
    }

    @Test
    void shouldSetCaseNameInFormPayload() {
        // Given
        String expectedCaseName = "some case name";
        when(caseNameFormatter.formatCaseName(anyList(), anyList())).thenReturn(expectedCaseName);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getCaseName()).isEqualTo(expectedCaseName);
    }

    @Test
    void shouldSetPropertyAddress() {
        // Given
        String expectedPropertyAddress = "formatted property address";
        stubFormattedPropertyAddress(expectedPropertyAddress);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getPropertyAddress()).isEqualTo(expectedPropertyAddress);
    }

    @Test
    void shouldSetSubmittedOnDate() {
        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getSubmittedOn()).isEqualTo(FIXED_CURRENT_DATE);
    }

    @Test
    void shouldSetApplicantPartyDetails() {
        // Given
        String firstName = "some first name";
        String lastName = "some last name";
        String emailAddress = "some email address";
        String phoneNumber = "some phone number";

        when(applicantPartyEntity.getFirstName()).thenReturn(firstName);
        when(applicantPartyEntity.getLastName()).thenReturn(lastName);
        when(applicantPartyEntity.getEmailAddress()).thenReturn(emailAddress);
        when(applicantPartyEntity.getPhoneNumber()).thenReturn(phoneNumber);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        Party applicantParty = formPayload.getApplicant();
        assertThat(applicantParty.getName()).isEqualTo("%s %s", firstName, lastName);
        assertThat(applicantParty.getEmailAddress()).isEqualTo(emailAddress);
        assertThat(applicantParty.getTelephoneNumber()).isEqualTo(phoneNumber);
    }

    @Test
    void shouldSetApplicantPartyAddressIfKnownAndDifferentToProperty() {
        // Given
        when(applicantPartyEntity.getAddressKnown()).thenReturn(VerticalYesNo.YES);
        when(applicantPartyEntity.getAddressSameAsProperty()).thenReturn(VerticalYesNo.NO);

        stubFormattedPropertyAddress("some property address");

        String formattedApplicantAddress = "formatted applicant address";
        stubFormattedApplicantAddress(formattedApplicantAddress);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        Party applicantParty = formPayload.getApplicant();
        assertThat(applicantParty.getCorrespondenceAddress()).isEqualTo(formattedApplicantAddress);
    }

    @Test
    void shouldSetApplicantPartyAddressIfKnownAndSameAsPropertyAddress() {
        // Given
        when(applicantPartyEntity.getAddressKnown()).thenReturn(VerticalYesNo.YES);
        when(applicantPartyEntity.getAddressSameAsProperty()).thenReturn(VerticalYesNo.YES);

        String formattedPropertyAddress = "formatted property address";
        stubFormattedPropertyAddress(formattedPropertyAddress);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        Party applicantParty = formPayload.getApplicant();
        assertThat(applicantParty.getCorrespondenceAddress()).isEqualTo(formattedPropertyAddress);
    }

    @Test
    void shouldSetApplicantPartyAddressAsNullIfNotKnown() {
        // Given
        when(applicantPartyEntity.getAddressKnown()).thenReturn(VerticalYesNo.NO);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        Party applicantParty = formPayload.getApplicant();
        assertThat(applicantParty.getCorrespondenceAddress()).isNull();
    }

    @Test
    void shouldSetApplicationType() {
        // Given
        GenAppType expectedApplicationType = GenAppType.ADJOURN;
        when(citizenGenAppRequest.getApplicationType()).thenReturn(expectedApplicationType);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getApplicationType()).isEqualTo(expectedApplicationType);
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldSetWithin14DaysFlag(VerticalYesNo isWithin14Days) {
        // Given
        when(citizenGenAppRequest.getWithin14Days()).thenReturn(isWithin14Days);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getWithin14Days()).isEqualTo(isWithin14Days);
    }

    @Test
    void shouldSetOrderWanted() {
        // Given
        String expectedOrderWanted = "some order";
        when(citizenGenAppRequest.getWhatOrderWanted()).thenReturn(expectedOrderWanted);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getWhatOrderWanted()).isEqualTo(expectedOrderWanted);
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldSetOtherPartiesAgreedFlag(VerticalYesNo otherPartiesAgreed) {
        // Given
        when(citizenGenAppRequest.getOtherPartiesAgreed()).thenReturn(otherPartiesAgreed);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getOtherPartiesAgreed()).isEqualTo(otherPartiesAgreed);
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldSetWithoutNoticeFlag(VerticalYesNo isWithoutNotice) {
        // Given
        when(citizenGenAppRequest.getWithoutNotice()).thenReturn(isWithoutNotice);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getWithoutNotice()).isEqualTo(isWithoutNotice);
    }

    @Test
    void shouldSetWithoutNoticeReason() {
        // Given
        String expectedWithoutNoticeReason = "some reason";
        when(citizenGenAppRequest.getWithoutNoticeReason()).thenReturn(expectedWithoutNoticeReason);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        assertThat(formPayload.getWithoutNoticeReason()).isEqualTo(expectedWithoutNoticeReason);
    }

    @Test
    void shouldSetStatementOfTruth() {
        // Given
        String expectedSotFullName = "some full name";
        when(citizenGenAppRequest.getSotFullName()).thenReturn(expectedSotFullName);

        // When
        underTest.generateSubmissionDocument(CASE_REFERENCE, citizenGenAppRequest, genAppEntity);

        // Then
        GenAppFormPayload formPayload = getFormPayload();
        StatementOfTruth statementOfTruth = formPayload.getStatementOfTruth();
        assertThat(statementOfTruth.getSubmittedOn()).isEqualTo(FIXED_CURRENT_DATE);
        assertThat(statementOfTruth.getFullName()).isEqualTo(expectedSotFullName);
    }

    private void stubFormattedPropertyAddress(String expectedPropertyAddress) {
        AddressEntity propertyAddressEntity = mock(AddressEntity.class);
        AddressUK propertyAddressUK = mock(AddressUK.class);
        when(pcsCaseEntity.getPropertyAddress()).thenReturn(propertyAddressEntity);
        when(addressMapper.toAddressUK(propertyAddressEntity)).thenReturn(propertyAddressUK);
        when(addressFormatter.formatFullAddress(propertyAddressUK, "\n"))
            .thenReturn(expectedPropertyAddress);
    }

    private void stubFormattedApplicantAddress(String formattedApplicantAddress) {
        AddressEntity applicantAddressEntity = mock(AddressEntity.class);
        AddressUK applicantAddressUK = mock(AddressUK.class);
        when(applicantPartyEntity.getAddress()).thenReturn(applicantAddressEntity);
        when(addressMapper.toAddressUK(applicantAddressEntity)).thenReturn(applicantAddressUK);
        when(addressFormatter.formatFullAddress(applicantAddressUK, "\n"))
            .thenReturn(formattedApplicantAddress);
    }

    private GenAppFormPayload getFormPayload() {
        verify(docAssemblyService)
            .generateDocument(formPayloadArgumentCaptor.capture(), anyString(), any(OutputType.class), anyString());

        FormPayload formPayload = formPayloadArgumentCaptor.getValue();
        assertThat(formPayload).isInstanceOf(GenAppFormPayload.class);

        return (GenAppFormPayload) formPayload;
    }
}
