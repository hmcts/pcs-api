package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CounterclaimDetailsSetupServiceTest {

    private static final String CURRENT_ORG_ID = "currentOrgId";
    private static final String OTHER_ORG_ID = "otherOrgId";

    @InjectMocks
    private CounterclaimDetailsSetupService underTest;

    private LegalRepDocumentUploadDetails details;

    @BeforeEach
    void setUp() {
        details = new LegalRepDocumentUploadDetails();
    }

    @Nested
    @DisplayName("setupCounterclaimDetails Tests")
    class SetupCounterclaimDetailsTests {

        @Test
        void shouldSetShowCounterclaimPageToNoWhenCounterclaimsNull() {
            // Given
            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(null)
                .build();

            // When
            underTest.setupCounterclaimDetails(caseEntity, details, CURRENT_ORG_ID);

            // Then
            assertThat(details.getShowCounterclaimPage()).isEqualTo(VerticalYesNo.NO);
            assertThat(details.getCounterclaimDocumentLinks()).isNull();
            assertThat(details.getValidCounterclaims()).isNull();
        }

        @Test
        void shouldSetShowCounterclaimPageToNoWhenCounterclaimsEmpty() {
            // Given
            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(List.of())
                .build();

            // When
            underTest.setupCounterclaimDetails(caseEntity, details, CURRENT_ORG_ID);

            // Then
            assertThat(details.getShowCounterclaimPage()).isEqualTo(VerticalYesNo.NO);
            assertThat(details.getCounterclaimDocumentLinks()).isNull();
            assertThat(details.getValidCounterclaims()).isNull();
        }

        @Test
        void shouldSetupCounterclaimDetailsForOwnCounterclaim() {
            // Given
            UUID ccId = UUID.randomUUID();
            PartyEntity ownParty = PartyEntity.builder()
                .organisationId(CURRENT_ORG_ID)
                .orgName("My Firm")
                .build();

            CounterClaimEntity counterClaim = CounterClaimEntity.builder()
                .id(ccId)
                .party(ownParty)
                .claimSubmittedDate(LocalDateTime.of(2026, 9, 22, 10, 0))
                .build();

            DocumentEntity document = DocumentEntity.builder()
                .counterClaim(counterClaim)
                .binaryUrl("http://dm-store/documents/123/binary")
                .build();

            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(List.of(counterClaim))
                .documents(List.of(document))
                .build();

            // When
            underTest.setupCounterclaimDetails(caseEntity, details, CURRENT_ORG_ID);

            // Then
            assertThat(details.getShowCounterclaimPage()).isEqualTo(VerticalYesNo.YES);
            assertThat(details.getCounterclaimDocumentLinks())
                .contains("govuk-inset-text")
                .contains("Counterclaim CC1 - My Firm.pdf")
                .contains("/documents/123/binary");

            DynamicStringList validCCs = details.getValidCounterclaims();
            assertThat(validCCs).isNotNull();
            assertThat(validCCs.getListItems()).hasSize(2);
            assertThat(validCCs.getListItems().get(0).getCode()).isEqualTo(ccId.toString());
            assertThat(validCCs.getListItems().get(0).getLabel())
                .contains("Yes, the documents I'm uploading relate to the counterclaim I made on "
                    + "Tuesday 22 September 2026");
            assertThat(validCCs.getListItems().get(1).getCode()).isEqualTo("MAIN_CLAIM");
        }

        @Test
        void shouldSetupCounterclaimDetailsForOtherDefendantCounterclaim() {
            // Given
            UUID ccId = UUID.randomUUID();
            PartyEntity otherParty = PartyEntity.builder()
                .organisationId(OTHER_ORG_ID)
                .firstName("John")
                .lastName("Doe")
                .build();

            CounterClaimEntity counterClaim = CounterClaimEntity.builder()
                .id(ccId)
                .party(otherParty)
                .claimSubmittedDate(LocalDateTime.of(2026, 9, 22, 10, 0))
                .build();

            DocumentEntity document = DocumentEntity.builder()
                .counterClaim(counterClaim)
                .url("http://dm-store/documents/456")
                .build();

            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(List.of(counterClaim))
                .documents(List.of(document))
                .build();

            // When
            underTest.setupCounterclaimDetails(caseEntity, details, CURRENT_ORG_ID);

            // Then
            assertThat(details.getShowCounterclaimPage()).isEqualTo(VerticalYesNo.YES);
            assertThat(details.getCounterclaimDocumentLinks())
                .contains("Counterclaim CC1 - John Doe.pdf")
                .contains("/documents/456");

            DynamicStringList validCCs = details.getValidCounterclaims();
            assertThat(validCCs.getListItems().get(0).getLabel())
                .contains("Yes, the documents I'm uploading relate to the counterclaim made by John Doe on "
                    + "Tuesday 22 September 2026");
        }
    }
}
