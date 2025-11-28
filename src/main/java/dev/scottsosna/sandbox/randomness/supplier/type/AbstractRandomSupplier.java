package dev.scottsosna.sandbox.randomness.supplier.type;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

abstract public class AbstractRandomSupplier implements LongSupplier, Lifecycle {

    /**
     * Avoids generating non-random data by ensuring buffer filled at least once.
     */
    private final AtomicBoolean ready = new AtomicBoolean(false);

    /**
     * Each supplier registers itself to the factory.
     */
    @Autowired
    protected RandomSupplierFactory factory;

    /**
     * Spring lifecycle: start the supplier.
     */
    public void start() {
        System.out.printf("%s Starting supplier '%s'\n", Instant.now(), supplierType());
    }

    /**
     * Spring lifecycle: stop the supplier.
     */
    public void stop() {
        System.out.printf ("%s Stopping supplier '%s'\n", Instant.now(), supplierType());
    }

    public boolean isReady() {
        return ready.get();
    }

    public void setReady(boolean ready) {
        this.ready.set(ready);
    }

    /**
     * Each entropy supplier is registered with a self-designated code which is used
     * to change between supplies, as needed.
     */
    public abstract String supplierType();

    @PostConstruct
    void init() {
        factory.register(this);
    }
}
