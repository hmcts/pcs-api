package uk.gov.hmcts.reform.pcs.roles.model.enums;

public enum Classification {
        PUBLIC, PRIVATE, RESTRICTED;

    public boolean isAtLeast(Classification that) {
        boolean result = false;
        switch (this) {
            case PUBLIC:
                result = that.equals(PUBLIC);
                break;
            case PRIVATE:
                result = that.equals(PUBLIC) || that.equals(PRIVATE);
                break;
            case RESTRICTED:
                result = that.equals(PUBLIC) || that.equals(PRIVATE) || that.equals(RESTRICTED);
                break;
        }
        return result;
    }
}
