package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RtlSdrSupplier extends AbstractStdinSupplier {

    /**
     * Tune the software-defined radio to this frequency.
     */
    @Value("${supplier.rtlfm.frequency:92.3e6}")
    private String frequency;

    /**
     * What broadcast mode, e.g., essentially how to interpret the frequency:
     * -fm: normal FM
     * -wbfm: wide-band FM
     * -am: normal AM
     * -usb: upper-side band
     * -lsb: lower-side band
     */
    @Value("${supplier.rtlfm.mode:fm}")
    private String mode;

    private static final String supplierType = "radio";

    public String supplierType() {
        return supplierType;
    }

    @Override
    protected String[] getCommandLine() {
        return new String[] {
                "rtl_fm",
                "-f",
                frequency,
                "-M",
                mode,
                "-s",
                "200000",
                "-r",
                "48000",
                "-"
        };
    }
}
