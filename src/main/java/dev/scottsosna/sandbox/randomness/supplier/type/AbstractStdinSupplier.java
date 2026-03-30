package dev.scottsosna.sandbox.randomness.supplier.type;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractStdinSupplier extends AbstractRandomSupplier {

    /**
     * Determines whether thread reading from stdin should continue
     */
    protected final AtomicBoolean supplierIsRunning = new AtomicBoolean(false);

    /**
     * The circular buffer into which the data is stored.
     */
    private byte[] circular;

    /**
     * the size of the buffer, used for reference when reading.
     */
    @Value("${supplier.stdin.bufferSize:262144}")
    private Integer bufferSize;

    /**
     * Maximum bytes read from stdin on each read
     */
    @Value("${supplier.stdin.chunkSize:25600}")
    private Integer chunkSize;

    /**
     * External process generating output read by supplier.
     */
    private Process externalProcess = null;

    /**
     * For thread-safety, track each thread's current read position, randomizing each thread's starting
     * position within buffer to ensure multiple threads don't get same bytes.
     */
    private final ThreadLocal<Integer> readPos =
            ThreadLocal.withInitial(() -> new Random().nextInt(Integer.valueOf(bufferSize) - 1));

    /**
     * Returns a long value based on the bytes in the circular buffer.
     */
    public long getAsLong() {

        //  If supplier is not running or hasn't read enough data to fill the buffer at least once, then throw
        //  exception to ensure caller doesn't get bad value (such as buffer initialized to all zeros).
        if (!isReady()) {
            throw new IllegalStateException("Supplier not ready");
        }

        //  Determine if next read would extend past the end of the buffer, in which case loop
        //  around to the beginning of buffer
        if ((readPos.get() + Long.BYTES) >= bufferSize) {
            readPos.set(0);
        }

        // Construct a long by shifting the existing answer 8 bits to the left and or'ing the current byte.
        ByteBuffer buff = ByteBuffer.allocate(Long.BYTES);
        buff.put(circular, readPos.get(), Long.BYTES);
        buff.flip();
        return buff.getLong();
    }

    /**
     * Spring Lifecycle: start the process that generates data read in from stdin.
     */
    @Override
    public void start() {
        super.start();
        try {
            //  Start external process
            startExternalProcess();
            supplierIsRunning.set(true);

            //  Begin separate thread for consuming stdin.
            new Thread(this::readData).start();
        } catch (Exception e) {
            //  Something bad has happened which means none of this is going to work.
            System.out.println("Exception occurred: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Spring Lifecycle: terminate external process generating the data read in from stdin.
     */
    @Override
    public void stop() {
        if (externalProcess != null || supplierIsRunning.get()) {
            super.stop();
            supplierIsRunning.set(false);
            setReady(false);
            if (externalProcess != null) {
                externalProcess.destroyForcibly();
                externalProcess = null;
            }
        }
    }

    /**
     * Spring Lifecycle: is external process currently running?
     */
    public boolean isRunning() {
        return supplierIsRunning.get();
    }

    /**
     * Read the SDR data from the pipe named and stuff it into the buffer.
     */
    private void readData() {

        InputStream is = externalProcess.getInputStream();
        byte[] temp = new byte[chunkSize];
        try {

            //  Minimize any potential waits by reading 1K at a time.
            int writePos = 0;

            //  Keep reading data until the flag is turned off/disabled/whatever.
            while (supplierIsRunning.get()) {

                //  Read chunk of bytes from stdin.
                int read = is.read(temp, 0, chunkSize);
                if (read == -1) break;

                //  XOR the data into the circular buffer.
                for (int index = 0; index < read; index++) {
                    //  May need to wrap around to beginning of buffer.
                    if (writePos >= circular.length) {
                        writePos = 0;
                        setReady(true);
                    }

                    //  XOR the read data with whatever is already in circular buffer
                    circular[writePos++] ^= temp[index];
                }
            }
        } catch (Exception e) {
            System.out.println("**** Exception reading data: " + e);
            e.printStackTrace();
        }

        //  A supplier still flagged as running indicates that the InputStream ran has no more data to provide - likely
        //  because the underlying process died - and therefore dropped out of the loop.  A supplier implementing
        //  RestartableSupplier means that the supplier may be restartable and therefore still providing random
        //  numbers
        if (supplierIsRunning.get() && this instanceof RestartableSupplier rs) {
            rs.restart();
        } else {
            stop();
        }
    }

    protected void resetForStart() {
        setReady(false);
    }

    /**
     * Buffer must be initialized post-construction of bean because Spring Config property is not
     * injected in abstract class until bean is constructed.
     */
    @PostConstruct
    public void initialize() {
        circular = new byte[bufferSize];
    }

    /**
     * Starts an external process via an OS command as required by supplier.
     */
    public Process startExternalProcess() throws Exception {
        resetForStart();
        externalProcess = new ProcessBuilder(getCommandLine()).start();
        return externalProcess;
    }

    /**
     * String array specifies command to execute and any required command line arguments for supplier.
     */
    abstract protected String[] getCommandLine();
}