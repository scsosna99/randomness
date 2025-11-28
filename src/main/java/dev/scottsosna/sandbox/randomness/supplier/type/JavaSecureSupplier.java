package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
public class JavaSecureSupplier extends JavaRandomSupplier {
    private static final String supplierType = "secure";

    public String supplierType() {
        return supplierType;
    }

    public Random createRandom() {
        return new SecureRandom();
    }
}
