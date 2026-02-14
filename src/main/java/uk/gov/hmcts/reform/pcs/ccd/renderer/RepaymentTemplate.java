package uk.gov.hmcts.reform.pcs.ccd.renderer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RepaymentTemplate {

    WARRANT("repaymentTableWarrant", "Warrant of possession fee"),
    WRIT("repaymentTableWrit", "Writ of possession fee");

    private final String templateName;
    private final String feeAmountLabel;
}
