package eu.europeana.cloud.service.dps.storm.topologies.oaipmh.bolt.harvester;

import com.google.common.base.Throwables;
import eu.europeana.cloud.service.dps.storm.AbstractDpsBolt;
import eu.europeana.cloud.service.dps.storm.topologies.oaipmh.exceptions.HarvesterException;
import eu.europeana.cloud.service.dps.storm.utils.CustomConnection;
import org.dspace.xoai.model.oaipmh.Verb;
import org.dspace.xoai.serviceprovider.parameters.GetRecordParameters;
import org.dspace.xoai.serviceprovider.parameters.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpression;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Class harvest record from the external OAI-PMH repository.
 */
public class Harvester implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(Harvester.class);
    private static final int DEFAULT_RETRIES = 3;
    private int socketTimeout = CustomConnection.DEFAULT_SOCKET_TIMEOUT;

    /**
     * Harvest record
     *
     * @param oaiPmhEndpoint OAI-PMH endpoint
     * @param recordId       record id
     * @param metadataPrefix metadata prefix
     * @param expression     XPATH expression
     * @return return metadata
     * @throws HarvesterException
     * @throws IOException
     */
    public InputStream harvestRecord(String oaiPmhEndpoint, String recordId, String metadataPrefix, XPathExpression expression, XPathExpression statusCheckerExpression)
            throws HarvesterException, IOException {


        GetRecordParameters params = new GetRecordParameters().withIdentifier(recordId).withMetadataFormatPrefix(metadataPrefix);
        int retries = DEFAULT_RETRIES;
        while (true) {
            CustomConnection client = new CustomConnection(oaiPmhEndpoint, socketTimeout);
            try {
                String record = client.execute(Parameters.parameters().withVerb(Verb.Type.GetRecord).include(params));
                XmlXPath xmlXPath = new XmlXPath(record);
                if (xmlXPath.isDeletedRecord(statusCheckerExpression)) {
                    retries = 0;
                    throw new HarvesterException("The record is deleted");
                }
                return xmlXPath.xpath(expression);
            } catch (Exception e) {
                if (retries-- > 0) {
                    LOGGER.warn("Error harvesting record {}. Retries left:{} ", recordId, retries);
                    try {
                        Thread.sleep(AbstractDpsBolt.SLEEP_TIME);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        LOGGER.error(Throwables.getStackTraceAsString(ex));
                    }
                } else {
                    throw new HarvesterException(String.format("Problem with harvesting record %1$s for endpoint %2$s because of: %3$s",
                            recordId, oaiPmhEndpoint, e.getMessage()), e);
                }
            }
        }
    }

    /**
     * Set the socket timeout (maximum time for entire request with fetching data)
     * Method dedicated for unit test. It allows decrease this value in test environment.
     * Default value is {@link CustomConnection#DEFAULT_SOCKET_TIMEOUT}
     *
     * @param socketTimeout New value for socket timeout
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}


