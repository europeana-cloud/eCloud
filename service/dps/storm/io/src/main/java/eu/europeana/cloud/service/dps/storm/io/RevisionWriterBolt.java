package eu.europeana.cloud.service.dps.storm.io;

import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.mcs.driver.RevisionServiceClient;
import eu.europeana.cloud.mcs.driver.exception.DriverException;
import eu.europeana.cloud.service.commons.urls.UrlParser;
import eu.europeana.cloud.service.commons.urls.UrlPart;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.storm.AbstractDpsBolt;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Date;

/**
 * Adds defined revisions to given representationVersion
 */
public class RevisionWriterBolt extends AbstractDpsBolt {
    private static final long serialVersionUID = 1L;
    public static final Logger LOGGER = LoggerFactory.getLogger(RevisionWriterBolt.class);

    protected transient RevisionServiceClient revisionsClient;

    private String ecloudMcsAddress;

    public RevisionWriterBolt(String ecloudMcsAddress) {
        this.ecloudMcsAddress = ecloudMcsAddress;
    }

    @Override
    public void execute(Tuple anchorTuple, StormTaskTuple stormTaskTuple) {
        try {
            addRevisionAndEmit(anchorTuple, stormTaskTuple);
        } finally {
            outputCollector.ack(anchorTuple);
        }
    }

    protected void addRevisionAndEmit(Tuple anchorTuple, StormTaskTuple stormTaskTuple) {
        LOGGER.info("{} executed", getClass().getSimpleName());
        try {
            addRevisionToSpecificResource(stormTaskTuple, stormTaskTuple.getParameter(PluginParameterKeys.OUTPUT_URL));
            outputCollector.emit(anchorTuple, stormTaskTuple.toStormTuple());
        } catch (MalformedURLException e) {
            LOGGER.error("URL is malformed: {} ", stormTaskTuple.getParameter(PluginParameterKeys.OUTPUT_URL));
            emitErrorNotification(anchorTuple, stormTaskTuple.getTaskId(), null, e.getMessage(), "The cause of the error is:"+e.getCause());
        } catch (MCSException | DriverException e) {
            LOGGER.warn("Error while communicating with MCS {}", e.getMessage());
            emitErrorNotification(anchorTuple, stormTaskTuple.getTaskId(), null, e.getMessage(), "The cause of the error is:"+e.getCause());
        } finally {
            outputCollector.ack(anchorTuple);
        }
    }

    protected void addRevisionToSpecificResource(StormTaskTuple stormTaskTuple, String affectedResourceURL) throws MalformedURLException, MCSException {
        if (stormTaskTuple.hasRevisionToBeApplied()) {
            LOGGER.info("Adding revisions to representation version: {}", stormTaskTuple.getFileUrl());
            final UrlParser urlParser = new UrlParser(affectedResourceURL);
            Revision revisionToBeApplied = stormTaskTuple.getRevisionToBeApplied();
            if (revisionToBeApplied.getCreationTimeStamp() == null)
                revisionToBeApplied.setCreationTimeStamp(new Date());
            addRevision(urlParser, revisionToBeApplied,stormTaskTuple.getParameter(PluginParameterKeys.AUTHORIZATION_HEADER));
        } else {
            LOGGER.info("Revisions list is empty");
        }
    }

    private void addRevision(UrlParser urlParser, Revision revisionToBeApplied,String authenticationHeader) throws MCSException {
        int retries = DEFAULT_RETRIES;
        while (true) {
            try {
                revisionsClient.addRevision(
                        urlParser.getPart(UrlPart.RECORDS),
                        urlParser.getPart(UrlPart.REPRESENTATIONS),
                        urlParser.getPart(UrlPart.VERSIONS),
                        revisionToBeApplied,
                        AUTHORIZATION,authenticationHeader);
                break;
            } catch (Exception e) {
                if (retries-- > 0) {
                    LOGGER.warn("Error while adding Revisions. Retries left {}", retries);
                    waitForSpecificTime();
                } else {
                    LOGGER.error("Error while getting Revisions from data set.");
                    throw e;
                }
            }
        }
    }

    @Override
    public void prepare() {
        if(ecloudMcsAddress == null) {
            throw new NullPointerException("MCS Server must be set!");
        }
        revisionsClient = new RevisionServiceClient(ecloudMcsAddress);
    }

    @Override
    protected void cleanInvalidData(StormTaskTuple tuple) {
        int attemptNumber = tuple.getRecordAttemptNumber();
        LOGGER.error("Attempt number {} to process this message. No cleaning needed here.", attemptNumber);
        // nothing to clean here when the message is reprocessed
    }
}
