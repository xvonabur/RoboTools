package eu.livotov.labs.android.robotools.net.multipart;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A PartSource that reads from a byte array.  This class should be used when
 * the data to post is already loaded into memory.
 *
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @since 2.0
 */
public class ByteArrayPartSource implements PartSource {

    /**
     * Name of the source file.
     */
    private String fileName;

    /**
     * Byte array of the source file.
     */
    private byte[] bytes;

    /**
     * Constructor for ByteArrayPartSource.
     *
     * @param fileName the name of the file these bytes represent
     * @param bytes    the content of this part
     */
    public ByteArrayPartSource(String fileName, byte[] bytes) {

        this.fileName = fileName;
        this.bytes = bytes;

    }

    /**
     * @see PartSource#getLength()
     */
    public long getLength() {
        return bytes.length;
    }

    /**
     * @see PartSource#getFileName()
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @see PartSource#createInputStream()
     */
    public InputStream createInputStream() {
        return new ByteArrayInputStream(bytes);
    }

}