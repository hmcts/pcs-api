package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mockito.InOrder;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.isA;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageConfigurerHelper {

    public static void verifyAndCount(InOrder inOrder, PageBuilder pageBuilder,
                                Class<? extends CcdPageConfiguration> pageClass, AtomicInteger counter) {
        inOrder.verify(pageBuilder).add(isA(pageClass));
        counter.incrementAndGet();
    }

    public static void verifyAndCount(InOrder inOrder, PageBuilder pageBuilder,
                                CcdPageConfiguration specificInstance, AtomicInteger counter) {
        inOrder.verify(pageBuilder).add(specificInstance);
        counter.incrementAndGet();
    }

}
