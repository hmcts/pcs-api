package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
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
class CounterClaimDetailsHydratorTest {

    private static final String CURRENT_ORG_ID = "currentOrgId";
    private static final String OTHER_ORG_ID = "otherOrgId";

    @InjectMocks
    private CounterClaimDetailsHydrator underTest;

    private LegalRepDocumentUploadDetails details;

    @BeforeEach
    void setUp() {
        details = new LegalRepDocumentUploadDetails();
    }

    @Nested
    @DisplayName("hydrate Tests")
    class HydrateTests {

        @Test
        void shouldSetShowCounterclaimPageToNoWhenCounterclaimsNull() {
            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(null)
                .build();

            underTest.hydrate(caseEntity, details, CURRENT_ORG_ID);

            assertThat(details.getShowCounterclaimPage()).isEqualTo(VerticalYesNo.NO);
            assertThat(details.getCounterclaimDocumentLinks()).isNull();
            assertThat(details.getValidCounterclaims()).isNull();
        }

        @Test
        void shouldSetShowCounterclaimPageToNoWhenCounterclaimsEmpty() {
            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(List.of())
                .build();

            underTest.hydrate(caseEntity, details, CURRENT_ORG_ID);

            assertThat(details.getShowCounterclaimPage()).isEqualTo(VerticalYesNo.NO);
            assertThat(details.getCounterclaimDocumentLinks()).isNull();
            assertThat(details.getValidCounterclaims()).isNull();
        }

        @Test
        void shouldHydrateDetailsForOwnCounterclaim() {
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
                .type(DocumentType.COUNTERCLAIM)
                .binaryUrl("http://dm-store/documents/123/binary")
                .build();

            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(List.of(counterClaim))
                .documents(List.of(document))
                .build();

            underTest.hydrate(caseEntity, details, CURRENT_ORG_ID);

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
        void shouldHydrateDetailsForOtherDefendantCounterclaimWithHtmlEscaping() {
            UUID ccId = UUID.randomUUID();
            PartyEntity otherParty = PartyEntity.builder()
                .organisationId(OTHER_ORG_ID)
                .firstName("John <Script>")
                .lastName("Doe & Co")
                .build();

            CounterClaimEntity counterClaim = CounterClaimEntity.builder()
                .id(ccId)
                .party(otherParty)
                .claimSubmittedDate(LocalDateTime.of(2026, 9, 22, 10, 0))
                .build();

            DocumentEntity document = DocumentEntity.builder()
                .counterClaim(counterClaim)
                .type(DocumentType.COUNTERCLAIM)
                .url("http://dm-store/documents/456")
                .build();

            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(List.of(counterClaim))
                .documents(List.of(document))
                .build();

            underTest.hydrate(caseEntity, details, CURRENT_ORG_ID);

            assertThat(details.getShowCounterclaimPage()).isEqualTo(VerticalYesNo.YES);
            assertThat(details.getCounterclaimDocumentLinks())
                .contains("Counterclaim CC1 - John &lt;Script&gt; Doe &amp; Co.pdf")
                .contains("/documents/456");

            DynamicStringList validCCs = details.getValidCounterclaims();
            assertThat(validCCs.getListItems().get(0).getLabel())
                .contains("Yes, the documents I'm uploading relate to the counterclaim made by "
                    + "John &lt;Script&gt; Doe &amp; Co on Tuesday 22 September 2026");
        }

        @Test
        void shouldHandleNullSubmittedDateWithoutTrailingOn() {
            UUID ccId = UUID.randomUUID();
            PartyEntity ownParty = PartyEntity.builder()
                .organisationId(CURRENT_ORG_ID)
                .orgName("My Firm")
                .build();

            CounterClaimEntity counterClaim = CounterClaimEntity.builder()
                .id(ccId)
                .party(ownParty)
                .claimSubmittedDate(null)
                .build();

            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(List.of(counterClaim))
                .documents(null)
                .build();

            underTest.hydrate(caseEntity, details, CURRENT_ORG_ID);

            DynamicStringList validCCs = details.getValidCounterclaims();
            assertThat(validCCs.getListItems().get(0).getLabel())
                .isEqualTo("Yes, the documents I'm uploading relate to the counterclaim I made");
        }

        @Test
        void shouldPrioritizeCounterclaimDocumentType() {
            UUID ccId = UUID.randomUUID();
            CounterClaimEntity counterClaim = CounterClaimEntity.builder()
                .id(ccId)
                .build();

            DocumentEntity supportingDoc = DocumentEntity.builder()
                .counterClaim(counterClaim)
                .type(DocumentType.DOCUMENTS_SUPPORTING_A_COUNTERCLAIM)
                .binaryUrl("http://dm-store/documents/supporting/binary")
                .build();

            DocumentEntity ccSummaryDoc = DocumentEntity.builder()
                .counterClaim(counterClaim)
                .type(DocumentType.COUNTERCLAIM)
                .binaryUrl("http://dm-store/documents/summary/binary")
                .build();

            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(List.of(counterClaim))
                .documents(List.of(supportingDoc, ccSummaryDoc))
                .build();

            underTest.hydrate(caseEntity, details, CURRENT_ORG_ID);

            assertThat(details.getCounterclaimDocumentLinks())
                .contains("/documents/summary/binary");
        }

        @Test
        void shouldReturnHashWhenDocumentsNullOrCcIdNull() {
            CounterClaimEntity ccWithoutId = CounterClaimEntity.builder().id(null).build();
            PcsCaseEntity caseNullDocs = PcsCaseEntity.builder()
                .counterClaims(List.of(ccWithoutId))
                .documents(null)
                .build();

            underTest.hydrate(caseNullDocs, details, CURRENT_ORG_ID);

            assertThat(details.getCounterclaimDocumentLinks()).contains("href=\"#\"");
        }

        @Test
        void shouldHandleUrlWithoutDocumentsPathInFormatDocumentUrl() {
            UUID ccId = UUID.randomUUID();
            CounterClaimEntity counterClaim = CounterClaimEntity.builder().id(ccId).build();

            DocumentEntity doc = DocumentEntity.builder()
                .counterClaim(counterClaim)
                .type(DocumentType.COUNTERCLAIM)
                .url("http://other-service/custom/path")
                .build();

            PcsCaseEntity caseEntity = PcsCaseEntity.builder()
                .counterClaims(List.of(counterClaim))
                .documents(List.of(doc))
                .build();

            underTest.hydrate(caseEntity, details, CURRENT_ORG_ID);

            assertThat(details.getCounterclaimDocumentLinks())
                .contains("href=\"http://other-service/custom/path\"");
        }
    }
}
