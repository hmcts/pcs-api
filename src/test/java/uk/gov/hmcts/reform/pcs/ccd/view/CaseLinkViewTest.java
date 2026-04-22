package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkReasonEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkViewTest {

    private PCSCase pcsCase;

    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock(strictness = LENIENT)
    private CaseLinkEntity caseLinkEntity;
    private CaseLinkView underTest;

    @BeforeEach
    void setUp() {
        pcsCase = PCSCase.builder().build();
        underTest = new CaseLinkView();
    }


    @Test
    void shouldMapAndWrapCaseLinks() {
        // Given
        CaseLinkReasonEntity caseLinkReasonEntity1 = createCaseLinkReasonEntity(UUID.randomUUID(), "CLR003");
        CaseLinkReasonEntity caseLinkReasonEntity2 = createCaseLinkReasonEntity(UUID.randomUUID(), "CLR010");
        when(pcsCaseEntity.getCaseLinks()).thenReturn(List.of(caseLinkEntity));
        when(caseLinkEntity.getLinkedCaseReference()).thenReturn(1234L);
        when(caseLinkEntity.getReasons()).thenReturn(List.of(caseLinkReasonEntity1, caseLinkReasonEntity2));
        when(caseLinkEntity.getCcdListId()).thenReturn("PCS");

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<CaseLink>> mappedCaseLinks = pcsCase.getCaseLinks();
        CaseLink firstMappedCaseLink  = mappedCaseLinks.getFirst().getValue();
        assertThat(mappedCaseLinks).hasSize(1);
        assertThat(firstMappedCaseLink.getCaseReference()).isEqualTo("1234");
        assertThat(firstMappedCaseLink.getCaseType()).isEqualTo("PCS");
        assertThat(firstMappedCaseLink.getReasonForLink().getFirst().getValue().getReason())
                        .isEqualTo("CLR003");
    }

    private CaseLinkReasonEntity createCaseLinkReasonEntity(UUID id, String reasonCode) {

        return CaseLinkReasonEntity.builder()
            .id(id)
            .reasonCode(reasonCode)
            .caseLink(caseLinkEntity)
            .build();
    }
}
