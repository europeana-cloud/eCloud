package eu.europeana.cloud.service.mcs.rest;

import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.service.aas.authentication.SpringUserUtils;
import eu.europeana.cloud.service.mcs.RecordService;
import eu.europeana.cloud.service.mcs.exception.CannotModifyPersistentRepresentationException;
import eu.europeana.cloud.service.mcs.exception.CannotPersistEmptyRepresentationException;
import eu.europeana.cloud.service.mcs.exception.RepresentationNotExistsException;
import eu.europeana.cloud.service.mcs.utils.EnrichUriUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static eu.europeana.cloud.service.mcs.RestInterfaceConstants.*;

/**
 * Resource to manage representation versions.
 */
@RestController
public class RepresentationVersionResource {

    private static final String REPRESENTATION_CLASS_NAME = Representation.class.getName();
    private final RecordService recordService;
    private final MutableAclService mutableAclService;

    public RepresentationVersionResource(RecordService recordService, MutableAclService mutableAclService) {
        this.recordService = recordService;
        this.mutableAclService = mutableAclService;
    }

    /**
     * Returns representation in a specified version.
     * <strong>Read permissions required.</strong>
     *
     * @param cloudId cloud id of the record which contains the representation(required).
     * @param representationName   name of the representation(required).
     * @param version  a specific version of the representation(required).
     * @return representation in requested version
     * @throws RepresentationNotExistsException representation does not exist in the
     *                                          specified version.
     * @summary get representation by version
     */
    @GetMapping(value = REPRESENTATION_VERSION, produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasPermission(#cloudId.concat('/').concat(#representationName).concat('/').concat(#version),"
            + " 'eu.europeana.cloud.common.model.Representation', read)")
    public @ResponseBody Representation getRepresentationVersion(
            HttpServletRequest httpServletRequest,
            @PathVariable String cloudId,
            @PathVariable String representationName,
            @PathVariable String version) throws RepresentationNotExistsException {

        Representation representation = recordService.getRepresentation(cloudId, representationName, version);
        EnrichUriUtil.enrich(httpServletRequest, representation);
        return representation;
    }

    /**
     * Deletes representation version.
     * <strong>Delete permissions required.</strong>
     *
     * @param cloudId cloud id of the record which contains the representation version (required).
     * @param representationName   name of the representation(required).
     * @param version  a specific version of the representation(required).
     * @throws RepresentationNotExistsException              representation does not exist in
     *                                                       specified version.
     * @throws CannotModifyPersistentRepresentationException representation in
     *                                                       specified version is persistent and as such cannot be removed.
     */
    @DeleteMapping(value = REPRESENTATION_VERSION)
    @PreAuthorize("hasPermission(#cloudId.concat('/').concat(#representationName).concat('/').concat(#version)," +
            " 'eu.europeana.cloud.common.model.Representation', delete)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRepresentation(
            @PathVariable String cloudId,
            @PathVariable String representationName,
            @PathVariable String version) throws RepresentationNotExistsException, CannotModifyPersistentRepresentationException {

        recordService.deleteRepresentation(cloudId, representationName, version);

        // let's delete the permissions as well
        ObjectIdentity dataSetIdentity = new ObjectIdentityImpl(REPRESENTATION_CLASS_NAME,
                cloudId + "/" + representationName + "/" + version);
        mutableAclService.deleteAcl(dataSetIdentity, false);
    }

    /**
     * Persists temporary representation.
     * <p/>
     * <strong>Write permissions required.</strong>
     *
     * @param cloudId cloud id of the record which contains the representation version(required).
     * @param representationName   name of the representation(required).
     * @param version  a specific version of the representation(required).
     * @return URI to the persisted representation in content-location.
     * @throws RepresentationNotExistsException              representation does not exist in
     *                                                       specified version.
     * @throws CannotModifyPersistentRepresentationException representation
     *                                                       version is already persistent.
     * @throws CannotPersistEmptyRepresentationException     representation version
     *                                                       has no file attached and as such cannot be made persistent.
     * @statuscode 201 representation is made persistent.
     */
    @PostMapping(value = REPRESENTATION_VERSION_PERSIST)
    @PreAuthorize("hasPermission(#cloudId.concat('/').concat(#representationName).concat('/').concat(#version),"
            + " 'eu.europeana.cloud.common.model.Representation', write)")
    public ResponseEntity<Void> persistRepresentation(
            HttpServletRequest httpServletRequest,
            @PathVariable String cloudId,
            @PathVariable String representationName,
            @PathVariable String version) throws RepresentationNotExistsException,
                        CannotModifyPersistentRepresentationException, CannotPersistEmptyRepresentationException {

        Representation persistentRepresentation = recordService.persistRepresentation(cloudId, representationName, version);
        EnrichUriUtil.enrich(httpServletRequest, persistentRepresentation);
        return ResponseEntity.created(persistentRepresentation.getUri()).build();
    }

    /**
     * Copies all information with all files and their contents from one
     * representation version to a new temporary one.
     * <strong>Read permissions required.</strong>
     *
     * @param cloudId cloud id of the record which contains the representation version
     * @param representationName   name of the representation
     * @param version  a specific version of the representation
     * @return URI to the created representation in content-location.
     * @throws RepresentationNotExistsException representation does not exist in
     *                                          specified version.
     * @summary copy information including file contents from one representation version to another
     * @statuscode 201 representation has been copied to a new one.
     */
    @PostMapping(value = REPRESENTATION_VERSION_COPY)
    @PreAuthorize("hasPermission(#cloudId.concat('/').concat(#representationName).concat('/').concat(#version),"
            + " 'eu.europeana.cloud.common.model.Representation', read)")
    public ResponseEntity<Void> copyRepresentation(
            HttpServletRequest httpServletRequest,
            @PathVariable String cloudId,
            @PathVariable String representationName,
            @PathVariable String version) throws RepresentationNotExistsException {

        Representation representationCopy = recordService.copyRepresentation(cloudId, representationName, version);
        EnrichUriUtil.enrich(httpServletRequest, representationCopy);

        String copiedReprOwner = SpringUserUtils.getUsername();
        if (copiedReprOwner != null) {
            ObjectIdentity versionIdentity = new ObjectIdentityImpl(REPRESENTATION_CLASS_NAME,
                    cloudId + "/" + representationName + "/" + representationCopy.getVersion());

            MutableAcl versionAcl = mutableAclService.createAcl(versionIdentity);

            versionAcl.insertAce(0, BasePermission.READ, new PrincipalSid(copiedReprOwner), true);
            versionAcl.insertAce(1, BasePermission.WRITE, new PrincipalSid(copiedReprOwner), true);
            versionAcl.insertAce(2, BasePermission.DELETE, new PrincipalSid(copiedReprOwner), true);
            versionAcl.insertAce(3, BasePermission.ADMINISTRATION, new PrincipalSid(copiedReprOwner),true);

            mutableAclService.updateAcl(versionAcl);
        }

        return ResponseEntity.created(representationCopy.getUri()).build();
    }
}
