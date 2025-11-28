package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.stereotype.Component;

@Component
public class Dump1090Supplier extends AbstractStdinSupplier {

    private static final String supplierType = "adsb";

    public String supplierType() {
        return supplierType;
    }

    @Override
    protected String[] getCommandLine() {
        return new String[] {"dump1090"};
    }
}
