package eu.europeana.cloud.service.mcs.rest.storage.selector;

import com.google.common.collect.ImmutableSet;
import eu.europeana.cloud.service.mcs.Storage;
import org.apache.tika.mime.MediaType;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.Set;

/**
 * Select {@link Storage} based on {@link MediaType} and data size.
 *
 * @author krystian.
 */
public class StorageSelector {
    private static final Set<MediaType> STORE_ONLY_IN_OBJECT_STORAGE =
            ImmutableSet.of(MediaType.image("jp2"));

    private final PreBufferedInputStream inputStream;
    private final int objectStoreSizeThreshold;
    private final MediaType userMediaType;

    /**
     * Creates {@link StorageSelector}.
     * @param inputStream input stream
     * @param userMediaType
     */
    public StorageSelector(final PreBufferedInputStream inputStream, final String userMediaType) {
        this.inputStream = inputStream;
        this.objectStoreSizeThreshold = inputStream.getBufferSize();
        this.userMediaType = MediaType.parse(userMediaType);
    }

    /**
     * Select {@link Storage}.
     * @return chosen storage
     */
    public Storage selectStorage() {
        return decide(assertUserMediaType(), getAvailable());
    }

    /**
     * Make decision based {@link MediaType} and data size.
     * @param detected media type
     * @param available data size
     * @return
     */
    protected Storage decide(MediaType detected, int available) {
        if (!STORE_ONLY_IN_OBJECT_STORAGE.contains(detected) && objectStoreSizeThreshold > available) {
            return Storage.DATA_BASE;
        } else {
            return Storage.OBJECT_STORAGE;
        }
    }

    private int getAvailable() {
        int available;
        try {
            available = inputStream.available();
        } catch (IOException e) {
            throw new BadRequestException("Unable to check data content length!", e);
        }
        return available;
    }

    private MediaType assertUserMediaType() {
        MediaType detected;
        try {
            detected = ContentStreamDetector.detectMediaType(inputStream);
        } catch (IOException e) {
            throw new BadRequestException("Unable to detect mime type.", e);
        }
        if (userMediaType == null || !userMediaType.equals(detected)) {
            throw new BadRequestException("Provided media type does not match to content media type!");
        }
        return detected;
    }
}
