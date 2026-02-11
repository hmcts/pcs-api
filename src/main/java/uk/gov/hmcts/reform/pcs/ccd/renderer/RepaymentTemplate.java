package uk.gov.hmcts.reform.pcs.ccd.renderer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RepaymentTemplate {

    WARRANT("repaymentTableWarrant"),
    WRIT("repaymentTableWrit");

    private final String templateName;
}
