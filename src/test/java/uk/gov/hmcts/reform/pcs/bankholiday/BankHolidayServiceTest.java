package uk.gov.hmcts.reform.pcs.bankholiday;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BankHolidayServiceTest {

    @Mock
    private BankHolidayApi bankHolidayApi;

    @InjectMocks
    private BankHolidayService bankHolidayService;

    @Test
    void shouldCallBankHolidayApi() {
        // Given
        BankHolidayResponse expectedResponse = BankHolidayResponse.builder().build();
        when(bankHolidayApi.getEnglandAndWalesHolidays()).thenReturn(expectedResponse);

        // When
        BankHolidayResponse actualResponse = bankHolidayService.getBankHolidays();

        // Then
        assertThat(actualResponse).isSameAs(expectedResponse);
        verify(bankHolidayApi).getEnglandAndWalesHolidays();
        verifyNoMoreInteractions(bankHolidayApi);
    }
}
