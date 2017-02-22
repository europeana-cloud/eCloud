package eu.europeana.cloud.integration.usecases;

import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.common.model.*;
import eu.europeana.cloud.common.response.CloudTagsResponse;
import eu.europeana.cloud.common.response.CloudVersionRevisionResponse;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.cloud.mcs.driver.RevisionServiceClient;
import eu.europeana.cloud.service.commons.urls.UrlParser;
import eu.europeana.cloud.service.commons.urls.UrlPart;
import eu.europeana.cloud.service.mcs.exception.DataSetAlreadyExistsException;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.europeana.cloud.integration.usecases.IntegrationConstants.FILE_CONTENT;


/**
 * Created by Tarek on 9/21/2016.
 */
public class DatasetHelper {
    private DataSetServiceClient dataSetServiceClient;
    private RecordServiceClient recordServiceClient;
    private RevisionServiceClient revisionServiceClient;
    private UISClient uisClient;
    private CloudId cloudId;
    private static Set<String> cloudIds = new HashSet<>();

    public DatasetHelper(DataSetServiceClient dataSetServiceClient, RecordServiceClient recordServiceClient, RevisionServiceClient revisionServiceClient, UISClient uisClient) {
        this.dataSetServiceClient = dataSetServiceClient;
        this.revisionServiceClient = revisionServiceClient;
        this.recordServiceClient = recordServiceClient;
        this.uisClient = uisClient;
    }

    public final URI prepareDatasetWithRecordsInside(String providerId, String datasetName, String representationName, String revisionName, List<String> tagNames, int numberOfRecords, String specificCloudId) throws MCSException, MalformedURLException, CloudException {
        createProviderIdIfNotExists(uisClient, providerId);
        URI uri = null;
        try {
            uri = dataSetServiceClient.createDataSet(providerId, datasetName, "");
        } catch (DataSetAlreadyExistsException e) {
        }
        addRecordsToDataset(numberOfRecords, datasetName, providerId, representationName, revisionName, tagNames, specificCloudId);
        return uri;


    }

    public final URI prepareEmptyDataset(String providerId, String datasetName) throws CloudException, MCSException {
        createProviderIdIfNotExists(uisClient, providerId);
        return dataSetServiceClient.createDataSet(providerId, datasetName, "");
    }

    public final List<Representation> getRepresentationsInsideDataSetByName(String providerId, String datasetName, String representationName) throws MCSException {
        List<Representation> representationList = new ArrayList<>();
        List<Representation> representations = dataSetServiceClient.getDataSetRepresentations(providerId, datasetName);
        for (Representation representation : representations) {
            if (representationName.equals(representation.getRepresentationName())) {
                representationList.add(representation);
            }
        }
        return representationList;
    }

    public final void assignRepresentationVersionToDataSet(String providerId, String datasetName, String cloudId, String representationName, String version) throws MCSException {
        dataSetServiceClient.assignRepresentationToDataSet(providerId, datasetName, cloudId, representationName, version);

    }

    public final void deleteDataset(String providerId, String datasetName) throws MCSException {
        try {
            dataSetServiceClient.deleteDataSet(providerId, datasetName);
        } catch (MCSException e) {
            System.out.println("The  dataSet " + datasetName + " can't be removed because " + e.getMessage());
        }
    }

    public final Set<String> getCloudIds() {
        return cloudIds;
    }

    public final void cleanCloudIds() {
        cloudIds.clear();
    }

    public final void grantPermissionToVersion(String cloudId, String representationName, String version, String userName, Permission permission) throws MCSException {
        recordServiceClient.grantPermissionsToVersion(cloudId, representationName, version, userName, permission);
    }


    public void addRecordsToDataset(int numberOfRecords, String datasetName, String providerId, String representationName, String revisionName, List<String> tagNames, String specificCloudId) throws CloudException, MCSException, MalformedURLException {
        String newCloudId = specificCloudId;
        if (specificCloudId != null) {
            cloudId = uisClient.getRecordId(specificCloudId).getResults().get(0);
        }
        for (int i = 0; i < numberOfRecords; i++) {
            if (specificCloudId == null) {
                newCloudId = prepareCloudId(providerId);
            }
            String uri = addFileToNewRepresentation(representationName, providerId, FILE_CONTENT);
            String version = getVersionFromFileUri(uri);
            addRevision(providerId, representationName, revisionName, tagNames, newCloudId, version);
            dataSetServiceClient.assignRepresentationToDataSet(providerId, datasetName, newCloudId, representationName, version);

        }
    }

    public void addRevision(String providerId, String representationName, String revisionName, List<String> tagNames, String newCloudId, String version) throws MCSException {
        for (String tagName : tagNames) {
            revisionServiceClient.addRevision(newCloudId, representationName, version, revisionName, providerId, tagName);
        }
    }


    public String prepareCloudId(String providerId) throws CloudException, MCSException {
        cloudId = uisClient.createCloudId(providerId);
        cloudIds.add(cloudId.getId());
        return cloudId.getId();
    }

    private void createProviderIdIfNotExists(UISClient uisClient, String providerId) throws CloudException {
        try {
            uisClient.getDataProvider(providerId);
        } catch (Exception e) {
            DataProviderProperties dataProviderProperties = new DataProviderProperties();
            uisClient.createProvider(providerId, dataProviderProperties);
        }
    }

    public String addFileToNewRepresentation(String representationName, String providerId, String fileContent) throws MCSException {
        InputStream inputStream = IOUtils.toInputStream(fileContent);
        URI uri = recordServiceClient.createRepresentation(cloudId.getId(), representationName, providerId, inputStream, "text/plain");
        return uri.toString();
    }

    public String getVersionFromFileUri(String URL) throws MalformedURLException, MCSException {
        UrlParser parser = new UrlParser(URL);
        return parser.getPart(UrlPart.VERSIONS);
    }

    public final List<CloudVersionRevisionResponse> getDataSetCloudIdsByRepresentation(String datasetName, String providerId, String representationName, String dateFrom, String tagName) throws MCSException {
        return dataSetServiceClient.getDataSetCloudIdsByRepresentation(datasetName, providerId, representationName, dateFrom, tagName);
    }

    public final List<CloudIdAndTimestampResponse> getLatestDataSetCloudIdByRepresentationAndRevision(String dataSetId, String providerId, String revisionProvider, String revisionName, String representationName, Boolean isDeleted) throws MCSException {
        return dataSetServiceClient.getLatestDataSetCloudIdByRepresentationAndRevision(dataSetId, providerId, revisionProvider, revisionName, representationName, isDeleted);
    }

    public final List<CloudTagsResponse> getDataSetRevisions(String providerId, String dataSetId,
                                                             String representationName, String revisionName,
                                                             String revisionProviderId, String revisionTimestamp) throws MCSException {
        return dataSetServiceClient.getDataSetRevisions(providerId, dataSetId, representationName, revisionName, revisionProviderId, revisionTimestamp);
    }

    public String getLatelyTaggedRecords(String dataSetId, String providerId, String cloudId, String representationName, String revisionName, String revisionProviderId)
            throws MCSException {
        return dataSetServiceClient.getLatelyTaggedRecords(dataSetId, providerId, cloudId, representationName, revisionName, revisionProviderId);

    }
}
