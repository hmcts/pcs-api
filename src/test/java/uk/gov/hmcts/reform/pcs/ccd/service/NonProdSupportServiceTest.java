package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import uk.gov.hmcts.reform.pcs.ccd.service.nonprod.NonProdSupportService;

class NonProdSupportServiceTest {

    @InjectMocks
    private NonProdSupportService underTest;

    @Mock
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {

    }

}
