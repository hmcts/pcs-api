package uk.gov.hmcts.reform.pcs.bulkprint.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdditionalData {

    private final String caseReference;

    private final List<String> recipients;

}
