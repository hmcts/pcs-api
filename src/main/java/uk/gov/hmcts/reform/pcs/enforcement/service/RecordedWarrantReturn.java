package uk.gov.hmcts.reform.pcs.enforcement.service;

import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;

public record RecordedWarrantReturn(WarrantReturnName returnName, String warrantNumber) {
}
