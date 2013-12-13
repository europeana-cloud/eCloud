package eu.europeana.cloud.service.mcs.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import eu.europeana.cloud.common.model.DataProviderProperties;
import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.service.mcs.ApplicationContextUtils;
import eu.europeana.cloud.service.mcs.DataProviderService;
import eu.europeana.cloud.service.mcs.RecordService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * FileResourceTest
 */
public class FilesResourceTest extends JerseyTest {

    private RecordService recordService;

    private DataProviderService providerService;

    private Representation rep;

    private File file;

    private WebTarget filesWebTarget;


    //    private UISClientHandlerImpl uisHandler;

    @Before
    public void mockUp()
            throws Exception {
        ApplicationContext applicationContext = ApplicationContextUtils.getApplicationContext();
        recordService = applicationContext.getBean(RecordService.class);
        providerService = applicationContext.getBean(DataProviderService.class);

        //        uisHandler = applicationContext.getBean(UISClientHandlerImpl.class);
        //        Mockito.doReturn(true).when(uisHandler).recordExistInUIS(Mockito.anyString());

        providerService.createProvider("1", new DataProviderProperties());
        rep = recordService.createRepresentation("1", "1", "1");
        file = new File();
        file.setFileName("fileName");
        file.setMimeType("mime/fileSpecialMime");

        Map<String, Object> allPathParams = ImmutableMap.<String, Object> of(ParamConstants.P_GID, rep.getRecordId(),
            ParamConstants.P_SCHEMA, rep.getSchema(), ParamConstants.P_VER, rep.getVersion());
        filesWebTarget = target(FilesResource.class.getAnnotation(Path.class).value()).resolveTemplates(allPathParams);
    }


    @After
    public void cleanUp()
            throws Exception {
        providerService.deleteProvider("1");
        recordService.deleteRepresentation(rep.getRecordId(), rep.getSchema());
    }


    @Override
    public Application configure() {
        return new JerseyConfig().property("contextConfigLocation", "classpath:spiedServicesTestContext.xml");
    }


    @Override
    protected void configureClient(ClientConfig config) {
        config.register(MultiPartFeature.class);
    }


    @Test
    public void shouldUploadDataWithPost()
            throws Exception {
        // given particular (random in this case) content in service
        byte[] content = new byte[1000];
        ThreadLocalRandom.current().nextBytes(content);
        String contentMd5 = Hashing.md5().hashBytes(content).toString();

        // when content is added to record representation
        FormDataMultiPart multipart = new FormDataMultiPart().field(ParamConstants.F_FILE_MIME, file.getMimeType())
                .field(ParamConstants.F_FILE_DATA, new ByteArrayInputStream(content),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE);

        Response postFileResponse = filesWebTarget.request().post(Entity.entity(multipart, multipart.getMediaType()));
        assertEquals("Unexpected status code", Response.Status.CREATED.getStatusCode(), postFileResponse.getStatus());
        assertEquals("File content tag mismatch", contentMd5, postFileResponse.getEntityTag().getValue());

        // then data should be in record service
        rep = recordService.getRepresentation(rep.getRecordId(), rep.getSchema(), rep.getVersion());
        assertEquals(1, rep.getFiles().size());

        File insertedFile = rep.getFiles().get(0);
        ByteArrayOutputStream contentBos = new ByteArrayOutputStream();
        recordService.getContent(rep.getRecordId(), rep.getSchema(), rep.getVersion(), insertedFile.getFileName(),
            contentBos);
        assertEquals("MD5 file mismatch", contentMd5, insertedFile.getMd5());
        assertEquals(content.length, insertedFile.getContentLength());
        assertArrayEquals(content, contentBos.toByteArray());
    }
}
