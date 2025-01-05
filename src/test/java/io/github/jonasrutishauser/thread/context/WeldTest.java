package io.github.jonasrutishauser.thread.context;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddEnabledInterceptors;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.jonasrutishauser.thread.context.ThreadSafeScopedTest.FieldProducer;
import io.github.jonasrutishauser.thread.context.ThreadSafeScopedTest.MethodProducer;
import io.github.jonasrutishauser.thread.context.ThreadSafeScopedTest.TestBean;
import io.github.jonasrutishauser.thread.context.impl.ThreadSafeContextInterceptor;
import io.github.jonasrutishauser.thread.context.impl.ThreadSafeExtension;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;

@EnableAutoWeld
@AddEnabledInterceptors(ThreadSafeContextInterceptor.class)
@AddBeanClasses({TestBean.class, FieldProducer.class, MethodProducer.class})
@AddExtensions(ThreadSafeExtension.class)
class WeldTest extends ThreadSafeScopedTest {

    @Any
    @Inject
    private Instance<TestBean> testBean;

    @ParameterizedTest(name = "{0}")
    @CsvSource({"class", "method", "field"})
    @Timeout(value = 1, unit = SECONDS)
    void testThreadSafty(String name) throws InterruptedException {
        threadSafty(testBean.select(NamedLiteral.of(name)));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"class", "method", "field"})
    @Timeout(value = 1, unit = SECONDS)
    void testSameThread(String name) throws InterruptedException {
        sameThread(testBean.select(NamedLiteral.of(name)));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"class", "method", "field"})
    @Timeout(value = 1, unit = SECONDS)
    void testFreeAfterUsage(String name) throws InterruptedException {
        freeAfterUsage(testBean.select(NamedLiteral.of(name)));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"class", "method", "field"})
    @Timeout(value = 1, unit = SECONDS)
    void testNested(String name) throws InterruptedException {
        nested(testBean.select(NamedLiteral.of(name)));
    }
}
