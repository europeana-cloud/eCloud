package eu.europeana.cloud.service.dps.storm.io;

import eu.europeana.cloud.common.model.DataSet;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
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
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class AddResultToDataSetBolt extends AbstractDpsBolt {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AddResultToDataSetBolt.class);

    private String ecloudMcsAddress;
    private transient DataSetServiceClient dataSetServiceClient;

    public AddResultToDataSetBolt(String ecloudMcsAddress) {
        this.ecloudMcsAddress = ecloudMcsAddress;
    }

    @Override
    public void prepare() {
        if(ecloudMcsAddress == null) {
            throw new NullPointerException("MCS Server must be set!");
        }
        dataSetServiceClient = new DataSetServiceClient(ecloudMcsAddress);
    }

    @Override
    public void execute(Tuple anchorTuple, StormTaskTuple stormTaskTuple) {
        LOGGER.info("Adding result to dataset");
        final String authorizationHeader = stormTaskTuple.getParameter(PluginParameterKeys.AUTHORIZATION_HEADER);
        String resultUrl = stormTaskTuple.getParameter(PluginParameterKeys.OUTPUT_URL);
        try {
            List<String> datasets = readDataSetsList(stormTaskTuple.getParameter(PluginParameterKeys.OUTPUT_DATA_SETS));
            if (datasets != null) {
                LOGGER.info("Data-sets that will be affected: {}", datasets);
                for (String datasetLocation : datasets) {
                    Representation resultRepresentation = parseResultUrl(resultUrl);
                    DataSet dataSet = parseDataSetURl(datasetLocation);
                    assignRepresentationToDataSet(dataSet, resultRepresentation, authorizationHeader);
                }
            }
            if (stormTaskTuple.getParameter(PluginParameterKeys.UNIFIED_ERROR_MESSAGE) == null)
                emitSuccessNotification(anchorTuple, stormTaskTuple.getTaskId(), stormTaskTuple.getFileUrl(), "", "", resultUrl);
            else
                emitSuccessNotification(anchorTuple, stormTaskTuple.getTaskId(), stormTaskTuple.getFileUrl(), "", "", resultUrl, stormTaskTuple.getParameter(PluginParameterKeys.UNIFIED_ERROR_MESSAGE), stormTaskTuple.getParameter(PluginParameterKeys.EXCEPTION_ERROR_MESSAGE));
        } catch (MCSException | DriverException e) {
            LOGGER.warn("Error while communicating with MCS {}", e.getMessage());
            emitErrorNotification(anchorTuple, stormTaskTuple.getTaskId(), resultUrl, e.getMessage(), "The cause of the error is: "+e.getCause());
        } catch (MalformedURLException e) {
            emitErrorNotification(anchorTuple, stormTaskTuple.getTaskId(), resultUrl, e.getMessage(), "The cause of the error is: "+e.getCause());
        } finally {
            outputCollector.ack(anchorTuple);
        }
    }

    private void assignRepresentationToDataSet(DataSet dataSet, Representation resultRepresentation, String authorizationHeader) throws MCSException {
        int retries = DEFAULT_RETRIES;
        while (true) {
            try {
                dataSetServiceClient.assignRepresentationToDataSet(
                        dataSet.getProviderId(),
                        dataSet.getId(),
                        resultRepresentation.getCloudId(),
                        resultRepresentation.getRepresentationName(),
                        resultRepresentation.getVersion(),
                        AUTHORIZATION,
                        authorizationHeader);
                break;
            } catch (Exception e) {
                if (retries-- > 0) {
                    LOGGER.warn("Error while assigning record to dataset. Retries left: {}", retries);
                    waitForSpecificTime();
                } else {
                    LOGGER.error("Error while assigning record to dataset.");
                    throw e;
                }
            }
        }
    }

    private List<String> readDataSetsList(String listParameter) {
        if (listParameter == null) {
            return null;
        }
        return Arrays.asList(listParameter.split(","));
    }

    private Representation parseResultUrl(String url) throws MalformedURLException {
        UrlParser parser = new UrlParser(url);
        if (parser.isUrlToRepresentationVersionFile()) {
            Representation rep = new Representation();
            rep.setCloudId(parser.getPart(UrlPart.RECORDS));
            rep.setRepresentationName(parser.getPart(UrlPart.REPRESENTATIONS));
            rep.setVersion(parser.getPart(UrlPart.VERSIONS));
            return rep;
        }
        throw new MalformedURLException("The resulted output URL is not formulated correctly");
    }

    private DataSet parseDataSetURl(String url) throws MalformedURLException {
        UrlParser parser = new UrlParser(url);
        if (parser.isUrlToDataset()) {
            DataSet dataSet = new DataSet();
            dataSet.setId(parser.getPart(UrlPart.DATA_SETS));
            dataSet.setProviderId(parser.getPart(UrlPart.DATA_PROVIDERS));
            return dataSet;
        }
        throw new MalformedURLException("The dataSet URL is not formulated correctly");

    }

    @Override
    protected void cleanInvalidData(StormTaskTuple tuple) {
        int attemptNumber = tuple.getRecordAttemptNumber();
        LOGGER.error("Attempt number {} to process this message. No cleaning needed here.", attemptNumber);
        // nothing to clean here when the message is reprocessed
    }

}
