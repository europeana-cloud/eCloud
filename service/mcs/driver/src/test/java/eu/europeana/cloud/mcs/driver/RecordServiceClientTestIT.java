package eu.europeana.cloud.mcs.driver;

import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.common.model.CloudId;
import eu.europeana.cloud.common.model.Record;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import eu.europeana.cloud.service.mcs.exception.RepresentationNotExistsException;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@Ignore
public class RecordServiceClientTestIT {
//http://localhosta:8080/mcs/records/2YRDQTLJMPCN264Y75CVIJ65RPZP5DJFS36CYAGMNIGT3GLKLMDA/representations/edm/versions
//https://test.ecloud.psnc.pl/api/records/2YRDQTLJMPCN264Y75CVIJ65RPZP5DJFS36CYAGMNIGT3GLKLMDA/representations/edm/versions

//https://test.ecloud.psnc.pl/api/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA/representations/metadataRecord/versions/
//http://localhost:8080/mcs/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA/representations/metadataRecord/versions/



    //private static final Logger LOGGER = LoggerFactory.getLogger(RecordServiceClientTestIT.class);

    private  static final String LOCAL_TEST_URL = "http://localhost:8080/mcs";
    private  static final String LOCAL_TEST_UIS_URL = "http://localhost:8080/uis";
    private  static final String REMOTE_TEST_URL = "https://test.ecloud.psnc.pl/api";
    private  static final String REMOTE_TEST_UIS_URL = "https://test.ecloud.psnc.pl/api";

    private static final String USER_NAME = "metis_test";  //user z bazy danych
    private static final String USER_PASSWORD = "1RkZBuVf";
    private static final String ADMIN_NAME = "admin";  //admin z bazy danych
    private static final String ADMIN_PASSWORD = "glEumLWDSVUjQcRVswhN";

    private static final String PROVIDER_ID = "xxx";
    private static final String DATA_SET_ID = "DATA_SET_ID";

    //http://localhost:8080/mcs/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA_
    //https://test.ecloud.psnc.pl/api/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA_
    @Test
    public void getRecord() throws MCSException {
        String cloudId = "SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA";

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, null, null);
        Record record = mcsClient.getRecord(cloudId);
        assertThat(record.getCloudId(), is(cloudId));
    }


    @Test /* !!! */
    public void deleteRecord() throws CloudException, MCSException, IOException {
        String representationName = "StrangeRepresentationNameToDelete";
        String versionTemplate = "versions/";

        UISClient uisClient = new UISClient(REMOTE_TEST_UIS_URL, USER_NAME, USER_PASSWORD);
        CloudId cloudId = uisClient.createCloudId(PROVIDER_ID);

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, ADMIN_NAME, ADMIN_PASSWORD);


        String filename = "log4j.properties";
        String mediatype = MediaType.TEXT_PLAIN_VALUE;
        InputStream is = RecordServiceClientTestIT.class.getResourceAsStream("/"+filename);


        URI representationURI = mcsClient.createRepresentation(cloudId.getId(), representationName, PROVIDER_ID, is, filename, mediatype);
        String representationURIString = representationURI.toString();

        int versionIndex = representationURIString.indexOf(versionTemplate);
        String version = representationURIString.substring(versionIndex+versionTemplate.length());


        //mcsClient.persistRepresentation(cloudId.getId(), representationName, version);

        Representation representation = mcsClient.getRepresentation(cloudId.getId(), representationName);


        Record record1 = mcsClient.getRecord(cloudId.getId());

        mcsClient.deleteRecord(cloudId.getId());

        Record record2 = mcsClient.getRecord(cloudId.getId());
    }

    //http://localhost:8080/mcs/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA/representations
    //https://test.ecloud.psnc.pl/api/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA/representations
    @Test
    public void getRepresentations() throws MCSException {
        String cloudId = "SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA";

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        List<Representation> representations = mcsClient.getRepresentations(cloudId);
        assertThat(representations.size(), is(1));
        assertThat(representations.get(0).getCloudId(), is(cloudId));
    }

    //http://localhost:8080/mcs/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA/representations/metadataRecord
    //https://test.ecloud.psnc.pl/api/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA/representations/metadataRecord
    @Test
    public void getRepresentation() throws MCSException {
        String cloudId = "SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA";

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        Representation representation = mcsClient.getRepresentation(cloudId, "metadataRecord");

        assertThat(representation.getCloudId(), is(cloudId));
    }

    @Test
    public void createRepresentation() throws CloudException, MCSException {
        String representationName = "StrangeRepresentationName";

        UISClient uisClient = new UISClient(REMOTE_TEST_UIS_URL, USER_NAME, USER_PASSWORD);
        CloudId cloudId = uisClient.createCloudId(PROVIDER_ID);

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        URI representationURI = mcsClient.createRepresentation(cloudId.getId(), representationName, PROVIDER_ID);

        int index = representationURI.toString().indexOf("/records/" + cloudId.getId() + "/representations/" + representationName + "/versions/");
        assertThat(index, not(-1));
    }


    @Test
    public void createRepresentationFile() throws CloudException, MCSException, IOException {
        String representationName = "StrangeRepresentationName";

        UISClient uisClient = new UISClient(REMOTE_TEST_UIS_URL, USER_NAME, USER_PASSWORD);
        CloudId cloudId = uisClient.createCloudId(PROVIDER_ID);

        String filename = "log4j.properties";
        String mediatype = MediaType.TEXT_PLAIN_VALUE;
        InputStream is = RecordServiceClientTestIT.class.getResourceAsStream("/"+filename);

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        URI representationURI = mcsClient.createRepresentation(cloudId.getId(), representationName, PROVIDER_ID,
                is, filename, mediatype);

        int index = representationURI.toString().indexOf("/records/" + cloudId.getId() + "/representations/" + representationName + "/versions/");
        assertThat(index, not(-1));
    }


    @Test
    public void createRepresentationFileKeyValue() throws CloudException, MCSException, IOException {
        String representationName = "StrangeRepresentationName";
        String key = "Authorisation";
        String value = "basic-";

        UISClient uisClient = new UISClient(REMOTE_TEST_UIS_URL, USER_NAME, USER_PASSWORD);
        CloudId cloudId = uisClient.createCloudId(PROVIDER_ID);

        String filename = "log4j.properties";
        String mediatype = MediaType.TEXT_PLAIN_VALUE;
        InputStream is = RecordServiceClientTestIT.class.getResourceAsStream("/"+filename);

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        URI representationURI = mcsClient.createRepresentation(cloudId.getId(), representationName, PROVIDER_ID,
                is, filename, mediatype, key, value);

        int index = representationURI.toString().indexOf("/records/" + cloudId.getId() + "/representations/" + representationName + "/versions/");
        assertThat(index, not(-1));
    }

    @Test
    public void createRepresentationFileNoName() throws CloudException, MCSException, IOException {
        String representationName = "StrangeRepresentationName";

        UISClient uisClient = new UISClient(REMOTE_TEST_UIS_URL, USER_NAME, USER_PASSWORD);
        CloudId cloudId = uisClient.createCloudId(PROVIDER_ID);

        String filename = "log4j.properties";
        String mediatype = MediaType.TEXT_PLAIN_VALUE;
        InputStream is = RecordServiceClientTestIT.class.getResourceAsStream("/"+filename);

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        URI representationURI = mcsClient.createRepresentation(cloudId.getId(), representationName, PROVIDER_ID, is, mediatype);

        int index = representationURI.toString().indexOf("/records/" + cloudId.getId() + "/representations/" + representationName + "/versions/");
        assertThat(index, not(-1));
    }

    @Test
    public void deleteRepresentation() throws CloudException, MCSException {
        String representationName = "StrangeRepresentationName";

        UISClient uisClient = new UISClient(REMOTE_TEST_UIS_URL, USER_NAME, USER_PASSWORD);
        CloudId cloudId = uisClient.createCloudId(PROVIDER_ID);

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        URI representationURI = mcsClient.createRepresentation(cloudId.getId(), representationName, PROVIDER_ID);

        mcsClient.deleteRepresentation(cloudId.getId(), representationName);
        Representation representation = mcsClient.getRepresentation(cloudId.getId(), representationName);


        int index = representationURI.toString().indexOf("/records/" + cloudId.getId() + "/representations/" + representationName + "/versions/");
        assertThat(index, not(-1));
    }

//    public void deleteRepresentation(String cloudId, String representationName)


    //http://localhost:8080/mcs/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA/representations
    //https://test.ecloud.psnc.pl/api/records/SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA/representations
    @Test
    public void getRepresentationsName() throws MCSException {
        String cloudId = "SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA";

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        List<Representation> representations = mcsClient.getRepresentations(cloudId, "metadataRecord");
        assertThat(representations.size(), is(2));

        for(Representation representation : representations) {
            assertThat(representation.getCloudId(), is(cloudId));
        }
    }

    @Test
    public void getRepresentationVersion() throws MCSException {
        String cloudId = "SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA";
        String version = "";

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        Representation representation = mcsClient.getRepresentation(cloudId, "metadataRecord", version);
    }

    @Test
    public void getRepresentationVersionKeyValue() throws MCSException {
        String cloudId = "SPBD7WGIBOP6IJSEHSJJL6BTQ7SSSTS2TA3MB6R6O2QTUREKU5DA";
        String version = "";
        String key = "";
        String value = "";

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        Representation representation = mcsClient.getRepresentation(cloudId, "metadataRecord", version, key, value);
    }

    @Test
    public void deleteRepresentationVersion() throws CloudException, MCSException {
        String representationName = "StrangeRepresentationName";
        String version = "";

        UISClient uisClient = new UISClient(REMOTE_TEST_UIS_URL, USER_NAME, USER_PASSWORD);
        CloudId cloudId = uisClient.createCloudId(PROVIDER_ID);

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        URI representationURI = mcsClient.createRepresentation(cloudId.getId(), representationName, PROVIDER_ID);

        mcsClient.deleteRepresentation(cloudId.getId(), representationName, version);
        Representation representation = mcsClient.getRepresentation(cloudId.getId(), representationName);


        int index = representationURI.toString().indexOf("/records/" + cloudId.getId() + "/representations/" + representationName + "/versions/");
        assertThat(index, not(-1));
    }

/*
    public void deleteRepresentation(String cloudId, String representationName, String version, String key, String value)
 */

    @Test
    public void copyRepresentationVersion() throws CloudException, MCSException {
        String representationName = "StrangeRepresentationName";
        String version = "";

        UISClient uisClient = new UISClient(REMOTE_TEST_UIS_URL, USER_NAME, USER_PASSWORD);
        CloudId cloudId = uisClient.createCloudId(PROVIDER_ID);

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        URI representationURI = mcsClient.createRepresentation(cloudId.getId(), representationName, PROVIDER_ID);

        URI representationCopyURI = mcsClient.copyRepresentation(cloudId.getId(), representationName, version);

        int index = representationURI.toString().indexOf("/records/" + cloudId.getId() + "/representations/" + representationName + "/versions/");
        assertThat(index, not(-1));
    }

    @Test
    public void persistRepresentationVrsion() throws CloudException, MCSException {
        String representationName = "StrangeRepresentationName";
        String version = "";

        UISClient uisClient = new UISClient(REMOTE_TEST_UIS_URL, USER_NAME, USER_PASSWORD);
        CloudId cloudId = uisClient.createCloudId(PROVIDER_ID);

        RecordServiceClient mcsClient = new RecordServiceClient(LOCAL_TEST_URL, USER_NAME, USER_PASSWORD);
        URI representationURI = mcsClient.createRepresentation(cloudId.getId(), representationName, PROVIDER_ID);

        mcsClient.persistRepresentation(cloudId.getId(), representationName, version);
        Representation representation = mcsClient.getRepresentation(cloudId.getId(), representationName);


        int index = representationURI.toString().indexOf("/records/" + cloudId.getId() + "/representations/" + representationName + "/versions/");
        assertThat(index, not(-1));
    }


/*
    public void grantPermissionsToVersion(String cloudId, String representationName, String version, String userName, Permission permission) throws MCSException {
 */


/*
    public void revokePermissionsToVersion(String cloudId, String representationName, String version, String userName, Permission permission) throws MCSException {
 */

/*
    public void permitVersion(String cloudId, String representationName, String version) throws MCSException {
 */


 /*
     public List<Representation> getRepresentationsByRevision(String cloudId, String representationName,
                                    String revisionName, String revisionProviderId, String revisionTimestamp)
                                                            throws RepresentationNotExistsException, MCSException {
  */

}