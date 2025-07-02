package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;

@ExtendWith(MockitoExtension.class)
class PcsCaseServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PCSCaseRepository pcsCaseRepository;
    @Mock
    private ModelMapper modelMapper;

    private PCSCaseService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PCSCaseService(modelMapper, pcsCaseRepository);
    }

    @Test
    void shouldCreateCaseWithNoData() {
    }

}
