package dev.scottsosna.sandbox.randomness.supplier.type;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component

public class Mp4DecoderSupplier extends AbstractStdinSupplier implements RestartableSupplier {

    /**
     * Preloaded list of video files found.
     */
    private List<String> videoFiles = null;

    /**
     * Directory containing video files.
     */
    @Value("${supplier.mp4.video.path:./videos}")
    private String videoPath;

    /**
     * subset of ffmpeg-supported pixel formats for decoding
     */
    private static final String[] formats = new String[] {
            "yuva420p9le",
            "yuva422p9le",
            "yuva444p9le",
            "yuva420p10le",
            "yuva422p10le",
            "yuva444p10le",
            "yuva420p16le",
            "yuva422p16le",
            "yuva444p16le"};

    /**
     *  "secure" random number generator to select file or format, not ideal
     *  but need something prior to actual decoding.
     */
    private static final SecureRandom random = new SecureRandom();

    private static final String supplierType = "mp4";

    public String supplierType() {
        return supplierType;
    }

    @Override
    protected String[] getCommandLine() {
        String fileName = getFileName();
        String format = getPixelFormat();
        System.out.println("Processing " + fileName + " to " + format);
        return new String[] {"ffmpeg", "-i", fileName, "-f", "rawvideo", "-c:v", "rawvideo", "-pix_fmt", format, "-"};
    }

    /**
     * Randomly select a video file to decode.
     */
    private String getFileName() {
        //  Potential files to decode may need discovery.
        if (videoFiles == null || videoFiles.isEmpty()) {
            loadVideoFiles();
        }

        //  Remove a pseudo-random file from the array and return it to caller.
        int currentSize = videoFiles.size();
        String fileName = (currentSize > 1) ? videoFiles.remove(random.nextInt(videoFiles.size() - 1)) : videoFiles.removeFirst();
        return fileName;
    }

    /**
     * Load file names from directory of supposed video files.
     */
    private void loadVideoFiles() {
        try (Stream<Path> fileStream = Files.list(Path.of(videoPath))) {
            videoFiles = fileStream
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            //  No files means no data from which to create random errors, don't proceed
            if (videoFiles.isEmpty()) {
                throw new IllegalArgumentException("No files found in " + videoPath);
            }
        } catch (IOException ioe) {
            //  Likely unable to read from directory, again no files from which to create
            //  random errors, also don't proceed.'
            throw new IllegalArgumentException("Unable to load video files from " + videoPath, ioe);
        }
    }

    /**
     * Randomly select a pixel format in which to decode, based on preloaded list.
     */
    private String getPixelFormat() {
        return formats[random.nextInt(formats.length - 1)];
    }
}
