package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class JavaOriginalSupplier extends JavaRandomSupplier {
    private static final String supplierType = "original";

    public String supplierType() {
        return supplierType;
    }

    public Random createRandom() {
        return new Random();
    }
}
