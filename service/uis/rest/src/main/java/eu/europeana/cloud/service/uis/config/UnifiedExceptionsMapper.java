package eu.europeana.cloud.service.uis.config;

import eu.europeana.cloud.common.exceptions.GenericException;
import eu.europeana.cloud.common.exceptions.ProviderDoesNotExistException;
import eu.europeana.cloud.common.response.ErrorInfo;
import eu.europeana.cloud.service.uis.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackages = {"eu.europeana.cloud.service.uis.rest"})
public class UnifiedExceptionsMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedExceptionsMapper.class);

    private static final String OTHER_ERROR_CODE_MESSAGE="OTHER";

    @ExceptionHandler({
            DatabaseConnectionException.class,
            IdHasBeenMappedException.class,
            ProviderDoesNotExistException.class,
            RecordDatasetEmptyException.class,
            RecordDoesNotExistException.class,
            RecordExistsException.class,
            RecordIdDoesNotExistException.class,
            ProviderAlreadyExistsException.class,
            CloudIdAlreadyExistException.class,
            CloudIdDoesNotExistException.class
    })
    public ResponseEntity<ErrorInfo> handleException(GenericException e) {
        LOGGER.error("Exception handling fired for", e);
        return buildResponse(e);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorInfo> handleAccessDeniedException(AccessDeniedException e) {
        LOGGER.error("Exception handling fired for ", e);
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .body(new ErrorInfo(OTHER_ERROR_CODE_MESSAGE, e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorInfo> handleRuntimeException(RuntimeException e) {
        LOGGER.error("Exception handling fired for ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(new ErrorInfo(OTHER_ERROR_CODE_MESSAGE, e.getMessage()));
    }

    private ResponseEntity<ErrorInfo> buildResponse(GenericException e) {
        return ResponseEntity.status(e.getErrorInfo().getHttpCode().getStatusCode())
                .body(e.getErrorInfo().getErrorInfo());
    }
}
