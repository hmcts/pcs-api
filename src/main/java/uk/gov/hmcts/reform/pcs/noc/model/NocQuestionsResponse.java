package uk.gov.hmcts.reform.pcs.noc.model;

import java.util.List;

public record NocQuestionsResponse(
    List<NocQuestion> questions
) {
}
