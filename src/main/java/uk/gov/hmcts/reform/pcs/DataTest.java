package uk.gov.hmcts.reform.pcs;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.amazon.deequ.VerificationResult;
import com.amazon.deequ.VerificationSuite;
import com.amazon.deequ.analyzers.AnalyzerOptions;
import com.amazon.deequ.checks.Check;
import com.amazon.deequ.checks.CheckLevel;
import com.amazon.deequ.checks.CheckStatus;
import com.amazon.deequ.constraints.Constraint;

import scala.Option;
import scala.collection.Seq;
import scala.collection.immutable.Nil$;

import java.util.Properties;

public class DataTest {

    public static void main(String[] args) {

        // initialiase spark session
        SparkSession spark = SparkSession.builder()
            .appName("Cftlib-Deequ-POC")
            .master("local[*]")
            .config("spark.ui.enabled", "false")
            .getOrCreate();

        // db connection
        String host = System.getenv().getOrDefault("PCS_DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("PCS_DB_PORT", "6432");
        String dbName = System.getenv().getOrDefault("PCS_DB_NAME", "pcs");
        String dbOptions = System.getenv().getOrDefault("PCS_DB_OPTIONS", "");
        String user = System.getenv().getOrDefault("PCS_DB_USER_NAME", "postgres");
        String password = System.getenv().getOrDefault("PCS_DB_PASSWORD", "postgres");

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName + dbOptions;

        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", user);
        connectionProperties.setProperty("password", password);
        connectionProperties.setProperty("driver", "org.postgresql.Driver");

        boolean allChecksPassed = true;

        try {
            allChecksPassed &= runPcsCaseChecks(spark, jdbcUrl, connectionProperties);
            allChecksPassed &= runAddressChecks(spark, jdbcUrl, connectionProperties);

            if (allChecksPassed) {
                System.out.println("**********SUCCESS: all checks passed**********");
            } else {
                System.out.println("**********FAILURE: one or more checks failed**********");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("**********Operational Error: " + e.getMessage());
            System.exit(1);
        } finally {
            spark.stop();
        }
    }

    private static boolean runPcsCaseChecks(SparkSession spark, String jdbcUrl,
                                            Properties connectionProperties) {
        System.out.println("**********Running checks on public.pcs_case**********");

        Dataset<Row> df = spark.read()
            .jdbc(jdbcUrl, "public.pcs_case", connectionProperties)
            .select("id", "case_reference", "property_address_id", "legislative_country");

        @SuppressWarnings("unchecked")
        Seq<Constraint> emptyConstraints = (Seq<Constraint>) (Seq<?>) Nil$.MODULE$;

        VerificationResult result = new VerificationSuite()
            .onData(df)
            .addCheck(Check.apply(CheckLevel.Error(), "pcs_case checks", emptyConstraints)
                          .isUnique("id", Option.<String>empty(), Option.<AnalyzerOptions>empty())
                          .isUnique("case_reference", Option.<String>empty(), Option.<AnalyzerOptions>empty())
                          .isComplete("case_reference", Option.<String>empty(), Option.<AnalyzerOptions>empty())
                          .isComplete("property_address_id", Option.<String>empty(), Option.<AnalyzerOptions>empty())
                          .isContainedIn("legislative_country", new String[]{"ENGLAND", "WALES"})
            )
            .run();

        return printResult("pcs_case", result);
    }

    private static boolean runAddressChecks(SparkSession spark, String jdbcUrl,
                                            Properties connectionProperties) {
        System.out.println("**********Running checks on public.address**********");

        Dataset<Row> df = spark.read()
            .jdbc(jdbcUrl, "public.address", connectionProperties)
            .select("id", "address_line1", "postcode");

        @SuppressWarnings("unchecked")
        Seq<Constraint> emptyConstraints = (Seq<Constraint>) (Seq<?>) Nil$.MODULE$;

        VerificationResult result = new VerificationSuite()
            .onData(df)
            .addCheck(Check.apply(CheckLevel.Error(), "address checks", emptyConstraints)
                          .isUnique("id", Option.<String>empty(), Option.<AnalyzerOptions>empty())
                          .isComplete("address_line1", Option.<String>empty(), Option.<AnalyzerOptions>empty())
                          .isComplete("postcode", Option.<String>empty(), Option.<AnalyzerOptions>empty())
            )
            .run();

        return printResult("address", result);
    }

    private static boolean printResult(String tableName, VerificationResult result) {
        boolean passed = result.status() == CheckStatus.Success();

        if (passed) {
            System.out.println("*****PASS: " + tableName + " — all checks succeeded*****");
        } else {
            System.out.println("*****FAIL: " + tableName + " — one or more checks failed*****");
            // Print details of exactly which constraints failed
            result.checkResults().foreach(entry -> {
                System.out.println("  Check: " + entry._1().description() + " -> " + entry._2().status());
                return null;
            });
        }

        return passed;
    }
}
