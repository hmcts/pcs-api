package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;

@Service
public class PossessiveNameService {

    public String applyApostrophe(String name) {
        if (name == null) {
            return null;
        }

        String trimmed = name.trim();

        if (trimmed.isEmpty()) {
            return "";
        }

        if (trimmed.endsWith("’") || trimmed.endsWith("’s") || trimmed.endsWith("’S")) {
            return trimmed;
        }

        return trimmed.endsWith("s") || trimmed.endsWith("S") ? trimmed + "’" : trimmed + "’s";
    }

}
