package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.ccd.sdk.type.LinkReason;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;



@ExtendWith(MockitoExtension.class)
class PcsCaseMergeServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private TenancyLicenceService tenancyLicenceService;

    private PcsCaseMergeService underTest;

    @BeforeEach
    void setUp() {

        underTest = new PcsCaseMergeService(
            securityContextService,
            modelMapper,
            tenancyLicenceService
        );
    }


    @Test
    void shouldMergeCaseData() {

        // Given
        final PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        PCSCase caseData = PCSCase.builder().build();

        List<ListValue<CaseLink>> caseLinks = createCaseLinks();

        caseData.setPropertyAddress(new AddressUK(
            "123 Great House", "Nice Street", "bla", "London",
            "Greater London","SW18 1QT", "UK"));

        caseData.setCaseLinks(caseLinks);

        // When
        underTest.mergeCaseData(pcsCaseEntity, caseData);

        // Then
        assertThat(pcsCaseEntity.getCaseLinks().size()).isEqualTo(1);
        assertThat(pcsCaseEntity.getCaseLinks().getFirst().getCcdListId()).isEqualTo("CCD");

    }


    private List<ListValue<CaseLink>> createCaseLinks() {
        LinkReason linkReason = createLinkReason("CLR003",
                                                 "");
        List<ListValue<LinkReason>> linkReasons = List.of(
            ListValue.<LinkReason>builder()
                .id(UUID.randomUUID().toString())
                .value(linkReason)
                .build());

        CaseLink caseLink = CaseLink.builder()
            .caseReference(String.valueOf(CASE_REFERENCE))
            .caseType("CCD")
            .reasonForLink(linkReasons)
            .build();

        return List.of(
            ListValue.<CaseLink>builder()
                .id(UUID.randomUUID().toString())
                .value(caseLink)
                .build());
    }

    private LinkReason createLinkReason(String reason, String description) {

        return LinkReason.builder()
            .reason(reason)
            .description(description)
            .build();
    }
}
