package uk.gov.hmcts.reform.pcs.testingsupport.model;

import java.util.UUID;

public record TestingSupportPin(UUID partyId, String plaintextCode) {
}
