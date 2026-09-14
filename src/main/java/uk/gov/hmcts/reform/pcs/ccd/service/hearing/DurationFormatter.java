package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.springframework.stereotype.Component;

@Component
public class DurationFormatter {

    public String format(Integer days, Integer hours, Integer minutes) {

        String result = "";

        if (days != null && days > 0) {
            if (days == 1) {
                result = "1 day";
            } else {
                result = "%d days".formatted(days);
            }
        }

        if (hours != null) {
            if (!result.isEmpty()) {
                result += " ";
            }

            if (hours == 1) {
                result += "1 hour";
            } else {
                result += "%d hours".formatted(hours);
            }
        }

        if (minutes != null) {
            if (!result.isEmpty()) {
                result += " ";
            }

            if (minutes == 1) {
                result += "1 minute";
            } else {
                result += "%d minutes".formatted(minutes);
            }
        }

        return result;
    }

}
