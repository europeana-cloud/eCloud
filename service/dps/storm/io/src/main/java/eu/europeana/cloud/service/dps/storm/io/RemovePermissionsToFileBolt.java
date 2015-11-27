package eu.europeana.cloud.service.dps.storm.io;

import eu.europeana.cloud.common.model.Permission;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.cloud.service.commons.urls.UrlParser;
import eu.europeana.cloud.service.commons.urls.UrlPart;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.storm.AbstractDpsBolt;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

/**
 * Will remove permissions to selected file from topology owner.
 */
public class RemovePermissionsToFileBolt extends AbstractDpsBolt {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RemovePermissionsToFileBolt.class);

    private RecordServiceClient recordServiceClient;
    private final String ecloudMcsAddress;
    private final String username;
    private final String password;

    public RemovePermissionsToFileBolt(String ecloudMcsAddress, String username, String password) {
        this.ecloudMcsAddress = ecloudMcsAddress;
        this.username = username;
        this.password = password;
    }

    @Override
    public void prepare() {
        recordServiceClient = new RecordServiceClient(ecloudMcsAddress, username, password);
    }

    @Override
    public void execute(StormTaskTuple tuple) {
        String resultFileUrl = readResultFileUrl(tuple);
        if (resultFileUrl == null) {
            LOGGER.info("Empty fileUrl. Permissions will not be removed");
            outputCollector.ack(inputTuple);
            return;
        }

        LOGGER.info("Removing permissions for {} on {}", username, resultFileUrl);
        removePermissions(resultFileUrl, username, tuple);
    }

    void removePermissions(String fileUrl, String userName, StormTaskTuple tuple) {
        try {
            UrlParser urlParser = new UrlParser(fileUrl);
            if (urlParser.isUrlToRepresentationVersionFile()) {
                recordServiceClient.revokePermissionsToVersion(
                        urlParser.getPart(UrlPart.RECORDS),
                        urlParser.getPart(UrlPart.REPRESENTATIONS),
                        urlParser.getPart(UrlPart.VERSIONS),
                        userName, Permission.ALL);
                emitSuccess(tuple);
            } else {
                logAndEmitError(tuple, "Provided url does not point to ecloud file. Permissions will not be removed on: {}" + fileUrl);
            }

        } catch (MalformedURLException e) {
            logAndEmitError(tuple, "Url to file is malformed. Permissions will not be removed on: {}" + fileUrl, e);
        } catch (MCSException e) {
            logAndEmitError(tuple, "There was exception while trying to remove permissions on: {}" + fileUrl, e);
        }
    }

    private String readResultFileUrl(StormTaskTuple tuple) {
        return tuple.getParameter(PluginParameterKeys.OUTPUT_URL);
    }
}
