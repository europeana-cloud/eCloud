package eu.europeana.cloud.service.dps.storm.topologies.media.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.gson.Gson;
import eu.europeana.cloud.service.dps.storm.AbstractDpsBolt;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.metis.mediaprocessing.MediaExtractor;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mediaprocessing.model.RdfResourceEntry;
import eu.europeana.metis.mediaprocessing.model.ResourceExtractionResult;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * Created by Tarek on 12/11/2018.
 */
public class ResourceProcessingBolt extends AbstractDpsBolt {
    private static final String RESOURCE_LINKS_COUNT = "RESOURCE_LINKS_COUNT";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceProcessingBolt.class);
    private static final String RESOURCE_LINK_KEY = "RESOURCE_LINK";
    private static final String RESOURCE_METADATA = "RESOURCE_METADATA";
    private static final String EXCEPTION_ERROR_MESSAGE = "EXCEPTION_ERROR_MESSAGE";


    static AmazonS3 amazonClient;
    private String awsAccessKey;
    private String awsSecretKey;
    private String awsEndPoint;
    private String awsBucket;


    private final Gson gson = new Gson();
    private MediaExtractor mediaExtractor;


    public ResourceProcessingBolt(String awsAccessKey, String awsSecretKey, String awsEndPoint, String awsBucket) {
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.awsEndPoint = awsEndPoint;
        this.awsBucket = awsBucket;
    }


    @Override
    public void execute(StormTaskTuple stormTaskTuple) {
        StringBuilder exception = new StringBuilder();
        if (stormTaskTuple.getParameter(RESOURCE_LINKS_COUNT) == null) {
            outputCollector.emit(stormTaskTuple.toStormTuple());
        } else {
            try {
                RdfResourceEntry rdfResourceEntry = gson.fromJson(stormTaskTuple.getParameter(RESOURCE_LINK_KEY), RdfResourceEntry.class);
                ResourceExtractionResult resourceExtractionResult = mediaExtractor.performMediaExtraction(rdfResourceEntry);
                stormTaskTuple.addParameter(RESOURCE_METADATA, gson.toJson(resourceExtractionResult.getMetadata()));
                List<Thumbnail> thumbnails = resourceExtractionResult.getThumbnails();
                for (Thumbnail thumbnail : thumbnails) {
                    try (InputStream stream = thumbnail.getContentStream()) {
                        amazonClient.putObject(awsBucket, thumbnail.getTargetName(), stream, null);
                        LOGGER.info("The thumbnail {} was uploaded successfully to Amazon S3", thumbnail.getTargetName());
                    } catch (Exception e) {
                        String errorMessage = "Error while uploading " + thumbnail.getTargetName() + " to Amazon S3. The full error message is " + e.getMessage();
                        buildErrorMessage(exception, errorMessage);
                    }
                }
            } catch (Exception e) {
                buildErrorMessage(exception, e.getMessage());
            } finally {
                stormTaskTuple.getParameters().remove(RESOURCE_LINK_KEY);
                if (!exception.toString().isEmpty())
                    stormTaskTuple.addParameter(EXCEPTION_ERROR_MESSAGE, exception.toString());
                outputCollector.emit(stormTaskTuple.toStormTuple());
            }
        }

    }

    @Override
    public void prepare() {
        try {
            synchronized (ResourceProcessingBolt.class) {
                initAmazonClient();
            }
            createMediaExtractor();
        } catch (Exception e) {
            LOGGER.error("Error while initialization", e);
        }
    }

    private void createMediaExtractor() throws MediaProcessorException {
        MediaProcessorFactory factory = new MediaProcessorFactory();
        mediaExtractor = factory.createMediaExtractor();
    }

    private void initAmazonClient() {
        if (amazonClient == null) {
            amazonClient = new AmazonS3Client(new BasicAWSCredentials(
                    awsAccessKey,
                    awsSecretKey));
            amazonClient.setEndpoint(awsEndPoint);
        }
    }

    private void buildErrorMessage(StringBuilder message, String newMessage) {
        LOGGER.error("Error while processing {}", newMessage);
        if (message.toString().isEmpty()) {
            message.append(newMessage);
        } else {
            message.append(",").append(newMessage);
        }
    }
}
