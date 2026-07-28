package uk.gov.hmcts.reform.pcs.bankholiday;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class BankHolidayService {

    private final BankHolidayApi bankHolidayApi;

    @Cacheable(value = "scottish_bank_holiday_cache", sync = true, cacheManager = "bankHolidayCacheManager")
    public BankHolidayResponse getBankHolidays() {
        log.info("Getting England and Wales bank holidays");
        return bankHolidayApi.getEnglandAndWalesHolidays();
    }
}
