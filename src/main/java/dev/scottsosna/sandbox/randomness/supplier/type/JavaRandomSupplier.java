package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
abstract public class JavaRandomSupplier extends AbstractRandomSupplier {

    protected Random random = null;

    @Override
    public long getAsLong() {
        return random.nextLong();
    }

    @Override
    public void start() {
        super.start();
        random = createRandom();
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    abstract protected Random createRandom();
}
