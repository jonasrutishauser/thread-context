package io.github.jonasrutishauser.thread.context;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

abstract class ThreadSafeScopedTest {

    void threadSafty(Provider<TestBean> testBean) throws InterruptedException {
        CountDownLatch await = new CountDownLatch(1);
        CountDownLatch countDown = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> testBean.get().await(await, countDown));
        executor.submit(() -> testBean.get().await(await, countDown));
        assertTrue(countDown.await(1, SECONDS));
        await.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, SECONDS));

        assertEquals(2, TestBean.getInstances());
    }

    void sameThread(Provider<TestBean> testBean) throws InterruptedException {
        CountDownLatch await = new CountDownLatch(1);
        CountDownLatch countDown = new CountDownLatch(4);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> {
            testBean.get().await(new CountDownLatch(0), countDown);
            testBean.get().await(await, countDown);
        });
        executor.submit(() -> {
            testBean.get().await(new CountDownLatch(0), countDown);
            testBean.get().await(await, countDown);
        });
        assertTrue(countDown.await(1, SECONDS));
        await.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, SECONDS));

        assertEquals(2, TestBean.getInstances());
    }

    void freeAfterUsage(Provider<TestBean> testBean) throws InterruptedException {
        threadSafty(testBean);
        nested(testBean);
        threadSafty(testBean);
    }

    void nested(Provider<TestBean> testBean) throws InterruptedException {
        CountDownLatch await = new CountDownLatch(1);
        CountDownLatch countDown = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> testBean.get().callNested(await, countDown));
        executor.submit(() -> testBean.get().callNested(await, countDown));
        assertTrue(countDown.await(1, SECONDS));
        await.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, SECONDS));

        assertEquals(2, TestBean.getInstances());
    }

    @Dependent
    static class FieldProducer {
        @Produces
        @Named("field")
        @ThreadSafeScoped
        final TestBean testBean;

        @Inject
        public FieldProducer(@Named("field") TestBean testBean) {
            this.testBean = new TestBean(testBean);
        }
    }

    @Dependent
    static class MethodProducer {
        @Produces
        @Named("method")
        @ThreadSafeScoped
        TestBean createTestBean(@Named("method") TestBean testBean) {
            return new TestBean(testBean);
        }
    }

    @Named("class")
    @ThreadSafeScoped
    static class TestBean {

        private static AtomicLong instances = new AtomicLong();

        private final TestBean testBean;
        private final long id;

        TestBean() {
            this(null);
        }

        @Inject
        public TestBean(@Named("class") TestBean testBean) {
            if (testBean == null) {
                this.id = -1;
            } else {
                this.id = instances.incrementAndGet();
            }
            this.testBean = testBean;
        }

        static void resetInstancesCount(@Observes @Initialized(ApplicationScoped.class) Object event) {
            instances.set(0);
        }

        public static long getInstances() {
            return instances.longValue();
        }

        public void callNested(CountDownLatch await, CountDownLatch countDown) {
            System.out.println("callNested: " + id);
            testBean.await(await, countDown);
        }

        public void await(CountDownLatch await, CountDownLatch countDown) {
            System.out.println("await: " + id);
            countDown.countDown();
            try {
                await.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
