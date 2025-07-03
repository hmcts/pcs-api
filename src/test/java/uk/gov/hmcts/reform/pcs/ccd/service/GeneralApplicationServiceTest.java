package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.entity.GACaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralApplicationServiceTest {

    @Mock
    private CoreCaseDataService coreCaseDataService;

    @Mock
    private GeneralApplicationRepository genAppRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private GeneralApplicationService service;

    @Test
    void shouldFindByCaseReferenceWhenExists() {
        GACaseEntity entity = new GACaseEntity();
        when(genAppRepository.findByCaseReference(123L)).thenReturn(Optional.of(entity));

        GACaseEntity result = service.findByCaseReference(123L);

        assertThat(result).isEqualTo(entity);
    }

    @Test
    void shouldThrowWhenCaseReferenceNotFound() {
        when(genAppRepository.findByCaseReference(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCaseReference(123L))
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessageContaining("123");
    }

    @Test
    void shouldConvertToGAEntity() {
        GACase gaCase = new GACase();
        GACaseEntity entity = new GACaseEntity();
        when(modelMapper.map(gaCase, GACaseEntity.class)).thenReturn(entity);

        GACaseEntity result = service.convertToGAEntity(gaCase);

        assertThat(result).isEqualTo(entity);
    }

    @Test
    void shouldConvertToGA() {
        GACaseEntity entity = new GACaseEntity();
        entity.setParentCaseReference(456L);
        GACase gaCase = new GACase();

        when(modelMapper.map(entity, GACase.class)).thenReturn(gaCase);

        GACase result = service.convertToGA(entity);

        assertThat(result).isEqualTo(gaCase);
        assertThat(result.getCaseLink()).isNotNull();
        assertThat(result.getCaseLink().getCaseReference()).isEqualTo("456");
    }


    @Test
    void shouldCreateGeneralApplicationInCCD() {
        GACase gaCase = new GACase();
        StartEventResponse startEvent = StartEventResponse.builder().token("token123").build();
        CaseDetails caseDetails = CaseDetails.builder().id(789L).build();

        when(coreCaseDataService.startCase(any(), any())).thenReturn(startEvent);
        when(coreCaseDataService.submitCaseCreation(any(), any())).thenReturn(caseDetails);

        Long id = service.createGeneralApplicationInCCD(gaCase, "createEvent");

        assertThat(id).isEqualTo(789L);
    }

    @Test
    void shouldUpdateGeneralApplicationInCCD() {
        GACase gaCase = new GACase();
        StartEventResponse eventResponse = StartEventResponse.builder().token("token456").build();
        CaseResource updated = new CaseResource();

        when(coreCaseDataService.startEvent(any(), any())).thenReturn(eventResponse);
        when(coreCaseDataService.submitEvent(any(), any())).thenReturn(updated);

        CaseResource result = service.updateGeneralApplicationInCCD("case123", "updateEvent", gaCase);

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void shouldDeleteGenApp() {
        service.deleteGenApp(123L);
        verify(genAppRepository).deleteByCaseReference(123L);
    }

    @Test
    void shouldSaveGaApp() {
        GACaseEntity entity = new GACaseEntity();

        service.saveGaApp(entity);

        verify(genAppRepository).save(entity);
    }
}

