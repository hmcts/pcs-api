package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import org.mockito.InOrder;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.isA;

public class PageConfigurerHelper {

    public static void verifyAndCount(InOrder inOrder, SavingPageBuilder pageBuilder,
                                Class<? extends CcdPageConfiguration> pageClass, AtomicInteger counter) {
        inOrder.verify(pageBuilder).add(isA(pageClass));
        counter.incrementAndGet();
    }

    public static void verifyAndCount(InOrder inOrder, SavingPageBuilder pageBuilder,
                                CcdPageConfiguration specificInstance, AtomicInteger counter) {
        inOrder.verify(pageBuilder).add(specificInstance);
        counter.incrementAndGet();
    }

}
