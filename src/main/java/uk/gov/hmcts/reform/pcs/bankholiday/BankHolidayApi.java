package uk.gov.hmcts.reform.pcs.bankholiday;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "bankHoliday", url = "${bankHoliday.url}")
public interface BankHolidayApi {
    @GetMapping(
        value = "england-and-wales.json",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    BankHolidayResponse getEnglandAndWalesHolidays();
}
