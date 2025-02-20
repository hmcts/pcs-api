package uk.gov.hmcts.reform.pcs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ApplicationTest {

    @Test
    void shouldInvokeSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mockedSpringApplication = Mockito.mockStatic(SpringApplication.class)) {
            Application.main(new String[]{});
            mockedSpringApplication.verify(() -> SpringApplication.run(Application.class, new String[]{}));
        }
    }
}
