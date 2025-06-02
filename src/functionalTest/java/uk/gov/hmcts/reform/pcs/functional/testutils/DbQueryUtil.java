package uk.gov.hmcts.reform.pcs.functional.testutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbQueryUtil {

    private final JdbcTemplate jdbcTemplate;

    public DbQueryUtil(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Autowired
    private Environment environment;

    public String getRegionByPostcode(String postcode) {
        String sql = "SELECT epimid FROM public.postcode_court_mapping where postcode = ?";

        return jdbcTemplate.query(sql, ps -> {
            ps.setString(1, postcode);
        }, rs -> rs.next() ? rs.getString("epimid") : null);
    }

    // You can add more utility methods for other queries here
}
