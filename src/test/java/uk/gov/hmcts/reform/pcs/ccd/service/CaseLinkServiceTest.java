package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.ccd.sdk.type.LinkReason;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkReasonEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

class CaseLinkServiceTest {

    private PcsCaseEntity pcsCaseEntity;
    private CaseLinkService underTest;

    @BeforeEach
    void setUp() {
        pcsCaseEntity = PcsCaseEntity.builder().build();
        underTest = new CaseLinkService();
    }

    @Test
    void shouldMergeCaseLinks() {
        //Given
        CaseLinkReasonEntity caseLinkReasonEntity = createCaseLinkReasonEntity();

        CaseLinkEntity caseLinkEntity = createCaseLinkEntity(List.of(caseLinkReasonEntity));

        LinkReason linkReason = createLinkReason(caseLinkReasonEntity.getReasonCode());
        List<ListValue<LinkReason>> linkReasons = List.of(
            ListValue.<LinkReason>builder()
                .id(caseLinkReasonEntity.getId().toString())
                .value(linkReason)
                .build());
        CaseLink caseLink = creatCaseLink(String.valueOf(caseLinkEntity.getLinkedCaseReference()),
                                          caseLinkEntity.getCcdListId(), linkReasons);

        List<ListValue<CaseLink>> caseLists = List.of(
            ListValue.<CaseLink>builder()
                .id(UUID.randomUUID().toString())
                .value(caseLink)
                .build());

        // When
        underTest.mergeCaseLinks(caseLists, pcsCaseEntity);

        // Then
        assertThat(pcsCaseEntity.getCaseLinks()).hasSize(1);
        assertThat(pcsCaseEntity.getCaseLinks().getFirst().getCcdListId()).isEqualTo("PCS");
        assertThat(pcsCaseEntity.getCaseLinks().getFirst().getReasons().getFirst().getReasonCode()).isEqualTo("CLR003");
    }

    private CaseLinkReasonEntity createCaseLinkReasonEntity() {

        CaseLinkEntity caseLinkEntity = mock(CaseLinkEntity.class);

        return CaseLinkReasonEntity.builder()
            .id(UUID.randomUUID())
            .reasonCode("CLR003")
            .caseLink(caseLinkEntity)
            .build();
    }

    private CaseLinkEntity createCaseLinkEntity(List<CaseLinkReasonEntity> linkReasonEntities) {

        return CaseLinkEntity.builder()
            .id(UUID.randomUUID())
            .linkedCaseReference(1234L)
            .ccdListId("PCS")
            .reasons(linkReasonEntities)
            .build();
    }

    private CaseLink creatCaseLink(String ref, String caseType, List<ListValue<LinkReason>> reasons) {

        return CaseLink.builder()
            .caseReference(ref)
            .caseType(caseType)
            .reasonForLink(reasons)
            .build();
    }

    private LinkReason createLinkReason(String reason) {

        return LinkReason.builder()
            .reason(reason)
            .build();
    }

}
