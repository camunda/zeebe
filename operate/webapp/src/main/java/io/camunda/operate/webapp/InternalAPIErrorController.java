/*
 * Copyright Camunda Services GmbH
 *
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING, OR DISTRIBUTING THE SOFTWARE (“USE”), YOU INDICATE YOUR ACCEPTANCE TO AND ARE ENTERING INTO A CONTRACT WITH, THE LICENSOR ON THE TERMS SET OUT IN THIS AGREEMENT. IF YOU DO NOT AGREE TO THESE TERMS, YOU MUST NOT USE THE SOFTWARE. IF YOU ARE RECEIVING THE SOFTWARE ON BEHALF OF A LEGAL ENTITY, YOU REPRESENT AND WARRANT THAT YOU HAVE THE ACTUAL AUTHORITY TO AGREE TO THE TERMS AND CONDITIONS OF THIS AGREEMENT ON BEHALF OF SUCH ENTITY.
 * “Licensee” means you, an individual, or the entity on whose behalf you receive the Software.
 *
 * Permission is hereby granted, free of charge, to the Licensee obtaining a copy of this Software and associated documentation files to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject in each case to the following conditions:
 * Condition 1: If the Licensee distributes the Software or any derivative works of the Software, the Licensee must attach this Agreement.
 * Condition 2: Without limiting other conditions in this Agreement, the grant of rights is solely for non-production use as defined below.
 * "Non-production use" means any use of the Software that is not directly related to creating products, services, or systems that generate revenue or other direct or indirect economic benefits.  Examples of permitted non-production use include personal use, educational use, research, and development. Examples of prohibited production use include, without limitation, use for commercial, for-profit, or publicly accessible systems or use for commercial or revenue-generating purposes.
 *
 * If the Licensee is in breach of the Conditions, this Agreement, including the rights granted under it, will automatically terminate with immediate effect.
 *
 * SUBJECT AS SET OUT BELOW, THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * NOTHING IN THIS AGREEMENT EXCLUDES OR RESTRICTS A PARTY’S LIABILITY FOR (A) DEATH OR PERSONAL INJURY CAUSED BY THAT PARTY’S NEGLIGENCE, (B) FRAUD, OR (C) ANY OTHER LIABILITY TO THE EXTENT THAT IT CANNOT BE LAWFULLY EXCLUDED OR RESTRICTED.
 */
package io.camunda.operate.webapp;

import io.camunda.operate.OperateProfileService;
import io.camunda.operate.exceptions.OperateRuntimeException;
import io.camunda.operate.webapp.rest.exception.Error;
import io.camunda.operate.webapp.rest.exception.InternalAPIException;
import io.camunda.operate.webapp.rest.exception.NotAuthorizedException;
import io.camunda.operate.webapp.rest.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class InternalAPIErrorController {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalAPIErrorController.class);

  @Autowired private OperateProfileService operateProfileService;

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(OperateRuntimeException.class)
  public ResponseEntity<Error> handleOperateRuntimeException(
      final OperateRuntimeException exception) {
    LOGGER.warn(exception.getMessage(), exception);
    final Error error =
        new Error()
            .setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .setMessage(operateProfileService.getMessageByProfileFor(exception));
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(io.camunda.operate.store.NotFoundException.class)
  public ResponseEntity<Error> handleRuntimeNotFoundException(
      final io.camunda.operate.store.NotFoundException exception) {
    LOGGER.warn(exception.getMessage(), exception);
    final Error error =
        new Error()
            .setStatus(HttpStatus.NOT_FOUND.value())
            .setMessage(operateProfileService.getMessageByProfileFor(exception));
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(InternalAPIException.class)
  public ResponseEntity<Error> handleInternalAPIException(final InternalAPIException exception) {
    LOGGER.warn(String.format("Instance: %s; %s", exception.getInstance(), exception.getMessage()));
    final Error error =
        new Error()
            .setStatus(HttpStatus.BAD_REQUEST.value())
            .setInstance(exception.getInstance())
            .setMessage(operateProfileService.getMessageByProfileFor(exception));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Error> handleInternalNotFoundException(final NotFoundException exception) {
    LOGGER.warn(String.format("Instance: %s; %s", exception.getInstance(), exception.getMessage()));
    final Error error =
        new Error()
            .setStatus(HttpStatus.NOT_FOUND.value())
            .setInstance(exception.getInstance())
            .setMessage(operateProfileService.getMessageByProfileFor(exception));
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }

  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler(NotAuthorizedException.class)
  public ResponseEntity<Error> handleNotAuthorizedException(
      final NotAuthorizedException exception) {
    LOGGER.warn(String.format("Instance: %s; %s", exception.getInstance(), exception.getMessage()));
    final Error error =
        new Error()
            .setStatus(HttpStatus.FORBIDDEN.value())
            .setInstance(exception.getInstance())
            .setMessage(operateProfileService.getMessageByProfileFor(exception));
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(error);
  }
}
