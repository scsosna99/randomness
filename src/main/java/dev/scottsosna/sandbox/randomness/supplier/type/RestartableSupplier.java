package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.context.Lifecycle;

import java.time.Instant;

public interface RestartableSupplier extends Lifecycle {
    default void restart() {
        System.out.printf("%s Restarting %s\n", Instant.now(), this.getClass().getSimpleName());
        stop();
        start();
    };
}
