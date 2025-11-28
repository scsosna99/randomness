package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.stereotype.Component;

@Component
public class DoNothingSupplier extends AbstractStdinSupplier implements RestartableSupplier {

    private static final String supplierType = "no-op";

    private static final String[] CMD_LINE = new String[] {"sleep", String.valueOf(Integer.MAX_VALUE)};

    public String supplierType() {
        return supplierType;
    }

    @Override
    protected String[] getCommandLine() {
        return CMD_LINE;
    }
}
