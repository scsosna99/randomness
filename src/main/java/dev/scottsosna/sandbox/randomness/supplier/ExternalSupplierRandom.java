package dev.scottsosna.sandbox.randomness.supplier;

import com.sun.jdi.request.InvalidRequestStateException;
import dev.scottsosna.sandbox.randomness.supplier.type.AbstractRandomSupplier;
import dev.scottsosna.sandbox.randomness.supplier.type.AbstractStdinSupplier;
import dev.scottsosna.sandbox.randomness.supplier.type.RandomSupplierFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Instant;
import java.util.Random;

@Component
@EnableScheduling
public class ExternalSupplierRandom extends Random {

    /**
     * Provides the next seed value to use based on its implementation.
     */
    private AbstractRandomSupplier supplier;

    /**
     * Provides a supplier by type
     */
    private final RandomSupplierFactory factory;

    /**
     * The supplier type as registered by the supplier
     */
    @Value("${supplier.supplier.type:no-op}")
    private String supplierType;


    /**
     * Constructor
     * @param factory suppliers
     */
    public ExternalSupplierRandom(@Autowired RandomSupplierFactory factory) {
        this.factory = factory;
    }

    /**
     * Start the supplier specified.
     * @param supplierType the supplier key as registered in the factory
     */
    public void initialize(String supplierType) {
        //  Do nothing is request type is same as existing
        if (this.supplier != null && this.supplier.supplierType().equals(supplierType)) {
            return;
        }

        //  Get the requested supplier.
        AbstractRandomSupplier supplier = getSupplier(supplierType);
        this.supplierType = supplierType;

        //  Stop existing supplier to cleanup  and release resource
        if (this.supplier != null) {
            this.supplier.stop();
        }

        //  Start the new supplier.
        this.supplier = supplier;
        this.supplier.start();
    }

    /**
     * Using the requested supplier, write binary data to the output stream as generated from the supplier
     * for statistical evaluation of the supplier.
     * @param supplierType key of the requested supplier
     * @param os output stream to which the binary data is written
     * @param longsCount how many longs to generate
     */
    public void dump(String supplierType,
                     OutputStream os,
                     int longsCount) {

        //  Get the supplier/
        AbstractRandomSupplier dumpSupplier = getSupplier(supplierType);

        //  A currently-running supplier can retrieve its longs directly without
        //  any specific stdin-process or whatever.  Suggested approach for non-stdin
        //  suppliers which aren't filling circular buffer.
        if (supplier.supplierType().equals(dumpSupplier.supplierType()) && dumpSupplier.isRunning()) {
            dumpSupplierRunning(os, longsCount);
        } else {
            //  Determine type of supplier - stdin or not - and call appropriate method.
            if (dumpSupplier instanceof AbstractStdinSupplier stdinSupplier) {
                dumpSupplierStdin(stdinSupplier, os, longsCount);
            } else {
                throw new InvalidRequestStateException("Requested supplier type must be already running.");
            }
        }


    }

    /**
     * Using the requested supplier, write binary data to the output stream as generated from the supplier
     * for statistical evaluation of the supplier.
     * @param dumpSupplier the specific supplier from which to generate data
     * @param os output stream to which the binary data is written
     * @param longsCount how many longs to generate
     */
    private void dumpSupplierStdin (AbstractStdinSupplier dumpSupplier,
                                    OutputStream os,
                                    int longsCount) {

        //  By directly consuming from stdin removes risk that random longs are generated faster than the
        //  circular buffer is filled, which reduces usefulness for statistical evaluation.
        try {
            System.out.printf("%s Starting supplier '%s for dump.'\n", Instant.now(), dumpSupplier.supplierType());
            Process p = dumpSupplier.startExternalProcess();

            byte[] data = new byte[128 * 1024];
            long totalRead = 0;
            try (InputStream is = p.getInputStream();
                 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os))) {

                //  Read data from the supplier's external process, converting each byte to a binary
                //  string and dumping to output stream.  For whatever reason, it's faster to
                //  convert/write bytes individually than bulk writing of bytes to output stream.
                while (totalRead < longsCount) {
                    int readCount = is.read(data, 0, (int) Math.min(data.length, (longsCount - totalRead)));
                    for (int i = 0; i < readCount; i++) {
                        bw.write(byteIntoBinaryTable[data[i] & 0xFF].toCharArray());
                    }
                    totalRead += readCount;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to dump data from " + dumpSupplier.supplierType(), e);
        } finally {
            System.out.printf("%s Stopping supplier '%s for dump.'\n", Instant.now(), dumpSupplier.supplierType());
            this.supplier.stop();
        }
    }

    /**
     * Using the requested supplier, create a stream of longs to convert into binary and write
     * to the output stream, usually for statistical evaluation.
     * @param os output stream to which the binary data is written
     * @param longsCount how many longs to generate
     */
    private void dumpSupplierRunning(OutputStream os,
                                     int longsCount) {

        System.out.println("Start: dump for running Supplier " + supplier.supplierType());
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os))) {
            longs(longsCount).forEach(l -> {
                try {
                    //  NOTE: Long.toBinaryString() doesn't provide leading zeros, so need to explicitly
                    //  add them to string written to output stream.
                    bw.write("%64s".formatted(Long.toBinaryString(l)).replace(' ','0'));
                } catch (IOException e) {
                    throw new RuntimeException("Exception dumping from " + supplier.supplierType(), e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Exception dumping from " + supplier.supplierType(), e);
        } finally {
            System.out.println("Stop: dump for running Supplier " + supplier.supplierType());
        }
    }

    /**
     * All methods in java.util.Random call this method to get a series of bits, from which the next int, long,
     * double, whatever is generated.  Based on <a href="https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/Random.java">...</a>
     * @param bits  the number of random bits to generate, in the range 1..32
     * @return the next random value
     */
    @Override
    protected int next (int bits) {
        long l = supplier.getAsLong() & ((1L << 48) - 1);
        return (int) (l >>> (48 - bits));
    }

    /**
     * Retrieve supplier from factory or throw exception if not found.
     * @param supplierType the supplier key as registered in the factory
     * @return the supplier instance as requested.
     */
    private AbstractRandomSupplier getSupplier(String supplierType) {
        //  Request new supplier from factory, really bad if it doesn't exist.
        AbstractRandomSupplier supplier = factory.get(supplierType);
        if (supplier == null) {
            throw new RuntimeException("Supplier " + supplierType + " not found");
        }

        return supplier;
    }

    /**
     * The supplier self-registration with the factory means that Spring can't determine the
     * bean dependency and (often) this component completes construction before the factory
     * is fully populated ... so instead schedule a one-off task for after factory is ready.
     */
    @Scheduled(initialDelay=2000L)
    private void doInit() {
        initialize(supplierType);
    }

    /**
     * Converts a byte value into its corresponding binary string.  More efficient to statically define values than
     * calculating just-in-time.
     */
    String[] byteIntoBinaryTable = new String[] {
        "00000000", "00000001", "00000010", "00000011", "00000100", "00000101", "00000110", "00000111", "00001000", "00001001", "00001010", "00001011", "00001100", "00001101", "00001110", "00001111",
        "00010000", "00010001", "00010010", "00010011", "00010100", "00010101", "00010110", "00010111", "00011000", "00011001", "00011010", "00011011", "00011100", "00011101", "00011110", "00011111",
        "00100000", "00100001", "00100010", "00100011", "00100100", "00100101", "00100110", "00100111", "00101000", "00101001", "00101010", "00101011", "00101100", "00101101", "00101110", "00101111",
        "00110000", "00110001", "00110010", "00110011", "00110100", "00110101", "00110110", "00110111", "00111000", "00111001", "00111010", "00111011", "00111100", "00111101", "00111110", "00111111",
        "01000000", "01000001", "01000010", "01000011", "01000100", "01000101", "01000110", "01000111", "01001000", "01001001", "01001010", "01001011", "01001100", "01001101", "01001110", "01001111",
        "01010000", "01010001", "01010010", "01010011", "01010100", "01010101", "01010110", "01010111", "01011000", "01011001", "01011010", "01011011", "01011100", "01011101", "01011110", "01011111",
        "01100000", "01100001", "01100010", "01100011", "01100100", "01100101", "01100110", "01100111", "01101000", "01101001", "01101010", "01101011", "01101100", "01101101", "01101110", "01101111",
        "01110000", "01110001", "01110010", "01110011", "01110100", "01110101", "01110110", "01110111", "01111000", "01111001", "01111010", "01111011", "01111100", "01111101", "01111110", "01111111",
        "10000000", "10000001", "10000010", "10000011", "10000100", "10000101", "10000110", "10000111", "10001000", "10001001", "10001010", "10001011", "10001100", "10001101", "10001110", "10001111",
        "10010000", "10010001", "10010010", "10010011", "10010100", "10010101", "10010110", "10010111", "10011000", "10011001", "10011010", "10011011", "10011100", "10011101", "10011110", "10011111",
        "10100000", "10100001", "10100010", "10100011", "10100100", "10100101", "10100110", "10100111", "10101000", "10101001", "10101010", "10101011", "10101100", "10101101", "10101110", "10101111",
        "10110000", "10110001", "10110010", "10110011", "10110100", "10110101", "10110110", "10110111", "10111000", "10111001", "10111010", "10111011", "10111100", "10111101", "10111110", "10111111",
        "11000000", "11000001", "11000010", "11000011", "11000100", "11000101", "11000110", "11000111", "11001000", "11001001", "11001010", "11001011", "11001100", "11001101", "11001110", "11001111",
        "11010000", "11010001", "11010010", "11010011", "11010100", "11010101", "11010110", "11010111", "11011000", "11011001", "11011010", "11011011", "11011100", "11011101", "11011110", "11011111",
        "11100000", "11100001", "11100010", "11100011", "11100100", "11100101", "11100110", "11100111", "11101000", "11101001", "11101010", "11101011", "11101100", "11101101", "11101110", "11101111",
        "11110000", "11110001", "11110010", "11110011", "11110100", "11110101", "11110110", "11110111", "11111000", "11111001", "11111010", "11111011", "11111100", "11111101", "11111110", "11111111"
    };
}
