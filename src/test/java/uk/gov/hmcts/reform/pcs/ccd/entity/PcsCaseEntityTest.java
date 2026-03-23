package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.ccd.sdk.type.LinkReason;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

class PcsCaseEntityTest {

    private PcsCaseEntity underTest;

    @BeforeEach
    void setUp() {
        underTest = new PcsCaseEntity();
    }

    @Test
    void shouldUpdateCaseOnTenancyLicenceWhenSet() {
        // Given
        TenancyLicenceEntity existingTenancyLicence = mock(TenancyLicenceEntity.class);
        TenancyLicenceEntity updatedTenancyLicence = mock(TenancyLicenceEntity.class);
        underTest.setTenancyLicence(existingTenancyLicence);

        // When
        underTest.setTenancyLicence(updatedTenancyLicence);

        // Then
        verify(existingTenancyLicence).setPcsCase(null);
        verify(updatedTenancyLicence).setPcsCase(underTest);
    }

    @Test
    void shouldMergeCaseLinks() {
        //Given
        CaseLinkReasonEntity caseLinkReasonEntity = createCaseLinkReasonEntity(UUID.randomUUID(),
                                                                                "CLR003", "Same Party");

        CaseLinkEntity caseLinkEntity = createCaseLinkEntity(UUID.randomUUID(), List.of(caseLinkReasonEntity),
                                                              1234L, "CCD1");

        LinkReason linkReason = createLinkReason(caseLinkReasonEntity.getReasonCode(),
                                                  caseLinkReasonEntity.getReasonText());
        List<ListValue<LinkReason>> linkReasons = List.of(
            ListValue.<LinkReason>builder()
                .id(caseLinkReasonEntity.getId().toString())
                .value(linkReason)
                .build());
        CaseLink caseLink = creatCaseLink(String.valueOf(caseLinkEntity.getLinkedCaseReference()),
                                          caseLinkEntity.getCcdListId(), linkReasons, null);

        List<ListValue<CaseLink>> caseLists = List.of(
            ListValue.<CaseLink>builder()
                .id(UUID.randomUUID().toString())
                .value(caseLink)
                .build());

        // When
        underTest.mergeCaseLinks(caseLists);

        // Then
        assertThat(underTest.getCaseLinks()).hasSize(1);
    }


    private CaseLinkReasonEntity createCaseLinkReasonEntity(UUID id, String reasonCode, String reasonText) {

        CaseLinkEntity caseLinkEntity = mock(CaseLinkEntity.class);

        return CaseLinkReasonEntity.builder()
            .id(id)
            .reasonCode(reasonCode)
            .reasonText(reasonText)
            .caseLink(caseLinkEntity)
            .build();
    }

    private CaseLinkEntity createCaseLinkEntity(UUID id, List<CaseLinkReasonEntity> linkReasonEntities,
                                                Long linkedCaseRef, String ccdId) {

        return CaseLinkEntity.builder()
            .id(id)
            .linkedCaseReference(linkedCaseRef)
            .ccdListId(ccdId)
            .reasons(linkReasonEntities)
            .build();
    }

    private CaseLink creatCaseLink(String ref, String caseType, List<ListValue<LinkReason>> reasons,
                                   LocalDateTime time) {

        return CaseLink.builder()
            .caseReference(ref)
            .caseType(caseType)
            .reasonForLink(reasons)
            .createdDateTime(time)
            .build();
    }

    private LinkReason createLinkReason(String reason, String description) {

        return LinkReason.builder()
            .reason(reason)
            .description(description)
            .build();
    }

}
