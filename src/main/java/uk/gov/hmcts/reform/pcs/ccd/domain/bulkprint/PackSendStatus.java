package uk.gov.hmcts.reform.pcs.ccd.domain.bulkprint;

public enum PackSendStatus {

    PENDING,
    HELD,
    SENT // TODO: Will we keep old records or delete them? Add an index if filtering the query by this column

}
