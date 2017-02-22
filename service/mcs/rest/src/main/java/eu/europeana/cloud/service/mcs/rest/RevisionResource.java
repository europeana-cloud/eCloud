package eu.europeana.cloud.service.mcs.rest;

import eu.europeana.cloud.common.model.DataSet;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.common.utils.RevisionUtils;
import eu.europeana.cloud.common.utils.Tags;
import eu.europeana.cloud.service.mcs.DataSetService;
import eu.europeana.cloud.service.mcs.RecordService;
import eu.europeana.cloud.service.mcs.exception.ProviderNotExistsException;
import eu.europeana.cloud.service.mcs.exception.RepresentationNotExistsException;
import eu.europeana.cloud.service.mcs.exception.RevisionIsNotValidException;
import jersey.repackaged.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static eu.europeana.cloud.common.web.ParamConstants.*;

/**
 * Created by Tarek on 8/2/2016.
 */

@Path("/records/{" + P_CLOUDID + "}/representations/{" + P_REPRESENTATIONNAME
        + "}/versions/{" + P_VER + "}/revisions")
@Component
@Scope("request")
public class RevisionResource {
    private static final Logger LOGGER = LoggerFactory.getLogger("RequestsLogger");

    @Autowired
    private RecordService recordService;

    @Autowired
    private DataSetService dataSetService;

    /**
     * Adds a new revision to representation version.
     * <strong>Read permissions required.</strong>
     *
     * @param globalId           cloud id of the record (required).
     * @param schema             schema of representation (required).
     * @param version            a specific version of the representation(required).
     * @param revisionName       the name of revision (required).
     * @param revisionProviderId revision provider id (required).
     * @param tag                tag flag (acceptance,published,deleted)
     * @return URI to specific revision with specific tag inside a version.TODO
     * @throws RepresentationNotExistsException representation does not exist in specified version
     * @throws RevisionIsNotValidException      if the added revision was not valid
     * @statuscode 201 object has been created.
     */
    @POST
    @Path("/{" + P_REVISION_NAME + "}/revisionProvider/{" + P_REVISION_PROVIDER_ID + "}/tag/{" + P_TAG + "}")
    @PreAuthorize("hasPermission(#globalId.concat('/').concat(#schema).concat('/').concat(#version),"
            + " 'eu.europeana.cloud.common.model.Representation', read)")
    public Response addRevision(@Context UriInfo uriInfo,
                                @PathParam(P_CLOUDID) final String globalId,
                                @PathParam(P_REPRESENTATIONNAME) final String schema,
                                @PathParam(P_VER) final String version,
                                @PathParam(P_REVISION_NAME) String revisionName,
                                @PathParam(P_TAG) String tag,
                                @PathParam(P_REVISION_PROVIDER_ID) String revisionProviderId
    )
            throws RepresentationNotExistsException, RevisionIsNotValidException, ProviderNotExistsException {
        ParamUtil.validate(P_TAG, tag, Arrays.asList(Tags.ACCEPTANCE.getTag(), Tags.PUBLISHED.getTag(), Tags.DELETED.getTag()));
        Revision revision = new Revision(revisionName, revisionProviderId);
        setRevisionTags(revision, new HashSet<>(Arrays.asList(tag)));
        addRevision(globalId, schema, version, revision);

        // insert information in extra table
        recordService.insertRepresentationRevision(globalId, schema, revisionProviderId, revisionName, version, revision.getCreationTimeStamp());

        return Response.created(uriInfo.getAbsolutePath()).build();
    }

    private void addRevision(String globalId, String schema, String version, Revision revision) throws RevisionIsNotValidException, ProviderNotExistsException, RepresentationNotExistsException {
        createAssignmentToRevisionOnDataSets(globalId, schema, version, revision);
        recordService.addRevision(globalId, schema, version, revision);
        dataSetService.updateProviderDatasetRepresentation(globalId, schema, version, revision);
        // insert information in extra table
        recordService.insertRepresentationRevision(globalId, schema, revision.getRevisionProviderId(), revision.getRevisionName(), version, revision.getCreationTimeStamp());
        dataSetService.updateAllRevisionDatasetsEntries(globalId, schema, version, revision);
    }

    private void createAssignmentToRevisionOnDataSets(String globalId, String schema,
                                                      String version, Revision revision)
            throws ProviderNotExistsException, RepresentationNotExistsException {
        Map<String, Set<String>> dataSets = dataSetService.getDataSets(globalId, schema, version);
        for (Map.Entry<String, Set<String>> entry : dataSets.entrySet()) {
            for (String dataset : entry.getValue()) {
                dataSetService.addDataSetsRevisions(entry.getKey(), dataset, revision, schema, globalId);
            }
        }
    }

    /**
     * Adds a new revision to representation version.
     * <strong>Read permissions required.</strong>
     *
     * @param revision Revision (required).
     * @return URI to revisions inside a version. TODO
     * @throws RepresentationNotExistsException representation does not exist in specified version
     * @statuscode 201 object has been created.
     */
    @POST
    @PreAuthorize("hasPermission(#globalId.concat('/').concat(#schema).concat('/').concat(#version),"
            + " 'eu.europeana.cloud.common.model.Representation', read)")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRevision(@Context UriInfo uriInfo,
                                @PathParam(P_CLOUDID) final String globalId,
                                @PathParam(P_REPRESENTATIONNAME) final String schema,
                                @PathParam(P_VER) final String version,
                                Revision revision
    )
            throws RevisionIsNotValidException, ProviderNotExistsException, RepresentationNotExistsException {

        addRevision(globalId, schema, version, revision);

        // insert information in extra table
        recordService.insertRepresentationRevision(globalId, schema, revision.getRevisionProviderId(), revision.getRevisionName(), version, revision.getCreationTimeStamp());

        return Response.created(uriInfo.getAbsolutePath()).build();
    }


    /**
     * Adds a new revision to representation version.
     * <strong>Read permissions required.</strong>
     *
     * @param globalId           cloud id of the record (required).
     * @param schema             schema of representation (required).
     * @param version            a specific version of the representation(required).
     * @param revisionName       the name of revision (required).
     * @param revisionProviderId revision provider id (required).
     * @param tags               set of tags (acceptance,published,deleted)
     * @return URI to a revision tags inside a version.TODO
     * @throws RepresentationNotExistsException representation does not exist in specified version
     * @throws RevisionIsNotValidException      if the added revision was not valid
     * @statuscode 201 object has been created.
     */

    @POST
    @Path("/{" + P_REVISION_NAME + "}/revisionProvider/{" + P_REVISION_PROVIDER_ID + "}/tags")
    @PreAuthorize("hasPermission(#globalId.concat('/').concat(#schema).concat('/').concat(#version),"
            + " 'eu.europeana.cloud.common.model.Representation', read)")
    public Response addRevision(@Context UriInfo uriInfo,
                                @PathParam(P_CLOUDID) final String globalId,
                                @PathParam(P_REPRESENTATIONNAME) final String schema,
                                @PathParam(P_VER) final String version,
                                @PathParam(P_REVISION_NAME) String revisionName,
                                @PathParam(P_REVISION_PROVIDER_ID) String revisionProviderId,
                                @FormParam(F_TAGS) Set<String> tags
    )
            throws RepresentationNotExistsException, RevisionIsNotValidException, ProviderNotExistsException {

        ParamUtil.validateTags(tags, new HashSet<>(Sets.newHashSet(Tags.ACCEPTANCE.getTag(), Tags.PUBLISHED.getTag(), Tags.DELETED.getTag())));
        Revision revision = new Revision(revisionName, revisionProviderId);
        setRevisionTags(revision, tags);
        addRevision(globalId, schema, version, revision);

        // insert information in extra table
        recordService.insertRepresentationRevision(globalId, schema, revisionProviderId, revisionName, version, revision.getCreationTimeStamp());
        return Response.created(uriInfo.getAbsolutePath()).entity(revision).build();
    }

    private Revision setRevisionTags(Revision revision, Set<String> tags) {
        if (tags == null || tags.isEmpty())
            return revision;
        if (tags.contains(Tags.ACCEPTANCE.getTag()))
            revision.setAcceptance(true);
        if (tags.contains(Tags.PUBLISHED.getTag()))
            revision.setPublished(true);
        if (tags.contains(Tags.DELETED.getTag()))
            revision.setDeleted(true);
        return revision;
    }

}
