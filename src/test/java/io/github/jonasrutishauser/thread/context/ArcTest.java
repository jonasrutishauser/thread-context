package io.github.jonasrutishauser.thread.context;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ExtendWith(ArquillianExtension.class)
class ArcTest extends ThreadSafeScopedTest {

    @Deployment
    static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class) //
                .addPackages(true, ThreadSafeScoped.class.getPackage()) //
                .filter(path -> !path.get().contains("FieldProducer") && !path.get().contains("MethodProducer")) //
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    @Named("class")
    Instance<TestBean> testBean;

    @Test
    void testThreadSafty() throws InterruptedException {
        threadSafty(testBean);
    }

    @Test
    void testSameThread() throws InterruptedException {
        sameThread(testBean);
    }

    @Test
    void testFreeAfterUsage() throws InterruptedException {
        freeAfterUsage(testBean);
    }

    @Test
    void testNested() throws InterruptedException {
        nested(testBean);
    }
}
