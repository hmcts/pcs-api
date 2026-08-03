package uk.gov.hmcts.reform.pcs.testingsupport.model;

import java.util.UUID;

public record TestingSupportAccessCode(UUID partyId, String plaintextCode) {
}
