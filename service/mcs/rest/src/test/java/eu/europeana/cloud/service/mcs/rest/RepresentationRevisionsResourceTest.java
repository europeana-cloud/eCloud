package eu.europeana.cloud.service.mcs.rest;

import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.common.response.RepresentationRevisionResponse;
import eu.europeana.cloud.common.web.ParamConstants;
import eu.europeana.cloud.service.mcs.RecordService;
import eu.europeana.cloud.service.mcs.exception.RepresentationNotExistsException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.ResultActions;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static eu.europeana.cloud.service.mcs.utils.MockMvcUtils.*;
import static junitparams.JUnitParamsRunner.$;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(JUnitParamsRunner.class)
public class RepresentationRevisionsResourceTest extends AbstractResourceTest {

    private RecordService recordService;

    static final private String globalId = "1";
    static final private String schema = "DC";
    static final private String revisionProviderId = "ABC";
    static final private String revisionName = "rev1";
    static final private String version = "1.0";
    static final private Date revisionTimestamp = new Date();
    static final private RepresentationRevisionResponse representationResponse = new RepresentationRevisionResponse(globalId, schema, version, Arrays.asList(new File("1.xml", "text/xml", "91162629d258a876ee994e9233b2ad87", "2013-01-01", 12345,
            null)), revisionProviderId, revisionName, revisionTimestamp);

    @Before
    public void mockUp() {
        recordService = applicationContext.getBean(RecordService.class);
        Mockito.reset(recordService);
        //
        AclPermissionEvaluator permissionEvaluator = applicationContext.getBean(AclPermissionEvaluator.class);
        Mockito.when(
                permissionEvaluator.hasPermission(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(true);
    }

    @SuppressWarnings("unused")
    private Object[] mimeTypes() {
        return $($(MediaType.APPLICATION_XML), $(MediaType.APPLICATION_JSON));
    }

    @Test
    @Parameters(method = "mimeTypes")
    public void getRepresentationByRevisionResponse(MediaType mediaType)
            throws Exception {
        RepresentationRevisionResponse representationRevisionResponse = new RepresentationRevisionResponse(representationResponse);
        ArrayList<File> files = new ArrayList<>(1);
        files.add(new File("1.xml", "text/xml", "91162629d258a876ee994e9233b2ad87",
                "2013-01-01", 12345L, URI.create("http://localhost:80/records/" + globalId
                + "/representations/" + schema + "/versions/" + version + "/files/1.xml")));
        representationRevisionResponse.setFiles(files);

        Representation representation = new Representation(representationRevisionResponse.getCloudId(), representationRevisionResponse.getRepresentationName(), representationRevisionResponse.getVersion(),
                null, null, representationRevisionResponse.getRevisionProviderId(), representationRevisionResponse.getFiles(), new ArrayList<Revision>(), false, representationRevisionResponse.getRevisionTimestamp());


        List<RepresentationRevisionResponse> expectedResponse = new ArrayList<>();
        expectedResponse.add(representationRevisionResponse);

        doReturn(expectedResponse).when(recordService).getRepresentationRevisions(globalId,
                schema, revisionProviderId, revisionName, null);

        when(recordService.getRepresentation(globalId, representationResponse.getRepresentationName(), representationResponse.getVersion())).thenReturn(representation);

        ResultActions response = mockMvc.perform(get(URITools.getRepresentationRevisionsPath(globalId, schema, revisionName))
                .queryParam(ParamConstants.F_REVISION_PROVIDER_ID, revisionProviderId).accept(mediaType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(mediaType));

        List<Representation> entity = responseContentAsRepresentationList(response, mediaType);
        assertThat(entity.size(), is(1));
        assertThat(entity.get(0), is(representation));
        verify(recordService, times(1)).getRepresentationRevisions(globalId, schema, revisionProviderId, revisionName, null);
        verify(recordService, times(1)).getRepresentation(globalId, schema, representationRevisionResponse.getVersion());
        verifyNoMoreInteractions(recordService);
    }

    @Test
    public void getRepresentationReturns406ForUnsupportedFormat() throws Exception {
        mockMvc.perform(get(URITools.getRepresentationRevisionsPath(globalId, schema, revisionName))
                .queryParam(ParamConstants.F_REVISION_PROVIDER_ID, revisionProviderId)
                .accept(MEDIA_TYPE_APPLICATION_SVG_XML)).andExpect(status().isNotAcceptable());
    }


    @Test
    public void getRepresentationByRevisionsThrowExceptionWhenReturnsEmptyObjectIfRevisionDoesNotExists()
            throws Exception {
        List<RepresentationRevisionResponse> expectedResponse = new ArrayList<>();
        expectedResponse.add(new RepresentationRevisionResponse());

        doReturn(expectedResponse).when(recordService).getRepresentationRevisions(globalId,
                schema, revisionProviderId, revisionName, null);

        doThrow(RepresentationNotExistsException.class).when(recordService).getRepresentation(any(), any(), any());

        mockMvc.perform(get(URITools.getRepresentationRevisionsPath(globalId, schema, revisionName))
                .queryParam(ParamConstants.F_REVISION_PROVIDER_ID, revisionProviderId)
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotFound());

        verify(recordService, times(1)).getRepresentationRevisions(globalId, schema, revisionProviderId, revisionName, null);
        verify(recordService, times(1)).getRepresentation(any(), any(), any());
    }

    @Test
    public void getRepresentationByRevisionsThrowExceptionWhenReturnsRepresentationRevisionResponseIsNull()
            throws Exception {
        when(recordService.getRepresentationRevisions(globalId, schema, revisionProviderId, revisionName, null)).thenReturn(null);
        mockMvc.perform(get(URITools.getRepresentationRevisionsPath(globalId, schema, revisionName))
                .queryParam(ParamConstants.F_REVISION_PROVIDER_ID, revisionProviderId)
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotFound());

        verify(recordService, times(1)).getRepresentationRevisions(globalId, schema, revisionProviderId, revisionName, null);
    }
}

