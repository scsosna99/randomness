package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NetworkTrafficSupplier extends AbstractStdinSupplier {

    /**
     * Specify network interface on which to listen when more than one
     * available and you need to specify.  Default works in most cases.
     */
    @Value("${supplier.tcpdump.interface:}")
    private String networkInterfaces;

    private static final String supplierType = "network";

    public String supplierType() {
        return supplierType;
    }

    @Override
    protected String[] getCommandLine() {
        if (networkInterfaces == null || networkInterfaces.isEmpty()) {
            return new String[] {"tcpdump", "-x"};
        } else {
            return new String[] {
                    "tcpdump",
                    "-x",
                    "-i",
                    networkInterfaces
            };
        }
    }
}
