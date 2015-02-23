package eu.europeana.cloud.service.mcs.rest;

import static eu.europeana.cloud.common.web.ParamConstants.P_CLOUDID;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Component;

import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.service.mcs.RecordService;
import eu.europeana.cloud.service.mcs.exception.RecordNotExistsException;

/**
 * Resource that represents record representations.
 */
@Path("/records/{" + P_CLOUDID + "}/representations")
@Component
@Scope("request")
public class RepresentationsResource {

    @Autowired
    private RecordService recordService;

    /**
     * Returns list of all latest persistent versions of record representation.
     *
     * @return list of representations
     * @throws RecordNotExistsException provided id is not known to Unique
     * Identifier Service.
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<Representation> getRepresentations(@Context UriInfo uriInfo, @PathParam(P_CLOUDID) String globalId)
            throws RecordNotExistsException {
        List<Representation> representationInfos = recordService.getRecord(globalId).getRepresentations();
        prepare(uriInfo, representationInfos);
        return representationInfos;
    }

    private void prepare(UriInfo uriInfo, List<Representation> representationInfos) {
        for (Representation representationInfo : representationInfos) {
            representationInfo.setFiles(null);
            EnrichUriUtil.enrich(uriInfo, representationInfo);
        }
    }
}
