package uk.gov.hmcts.reform.pcs.ccd.service;

import java.util.Collection;
import java.util.UUID;

public record UserRoles(UUID userId, Collection<String> roles) {
}
