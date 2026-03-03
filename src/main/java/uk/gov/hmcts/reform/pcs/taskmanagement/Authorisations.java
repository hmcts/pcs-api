package uk.gov.hmcts.reform.pcs.taskmanagement;

import lombok.Getter;

@Getter
public enum Authorisations {
    JUDICIAL("328"), // TODO: Where does this value come from?
    NONE("");

    private final String authorisation;

    Authorisations(String authorisation) {
        this.authorisation = authorisation;
    }
}




