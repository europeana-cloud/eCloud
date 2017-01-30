package eu.europeana.cloud.service.mcs.rest.storage.selector;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Detect {@link InputStream} media type.
 * @author krystian.
 */
public class ContentStreamDetector {
    private ContentStreamDetector() {
        throw new UnsupportedOperationException("Pure static class!");
    }

    /**
     * Detect {@link InputStream} media type.
     * @param inputStream stream
     * @return detected media type
     * @throws IOException
     */
    public static MediaType detectMediaType(InputStream inputStream) throws IOException {
        if (!inputStream.markSupported())
            throw new UnsupportedOperationException("InputStream marking support is required!");
        return new AutoDetectParser().getDetector().detect(inputStream, new Metadata());

    }
}
