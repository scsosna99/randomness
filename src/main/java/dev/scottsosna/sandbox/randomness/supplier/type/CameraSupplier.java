package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CameraSupplier extends AbstractStdinSupplier {

    /**
     * Allows you to specify which camera to use when more than one available.
     */
    @Value("${supplier.camera.index:0}")
    private Integer cameraIndex;

    private static final String supplierType = "camera";

    public String supplierType() {
        return supplierType;
    }

    @Override
    protected String[] getCommandLine() {
        return new String[] {
                "ffmpeg",
                "-f",
                "avfoundation",
                "-framerate",
                "30",
                "-video_size",
                "640x480",
                "-i",
                "%d:none".formatted(cameraIndex),
                "-f",
                "rawvideo",
                "-pix_fmt",
                "yuv420p",
                "-"
        };
    }
}
