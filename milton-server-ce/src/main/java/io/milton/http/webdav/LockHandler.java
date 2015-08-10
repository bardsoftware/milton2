// Copyright (C) 2015 BarD Software
package io.milton.http.webdav;

import io.milton.http.ExistingEntityHandler;
import io.milton.http.HttpManager;
import io.milton.http.LockInfo;
import io.milton.http.LockInfoSaxHandler;
import io.milton.http.LockResult;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;
import io.milton.http.Request;
import io.milton.http.ResourceHandlerHelper;
import io.milton.http.Response;
import io.milton.http.XmlWriter;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.LockedException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.LockableResource;
import io.milton.resource.Resource;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import java.io.IOException;


/**
 * @author dbarashev@bardsoftware.com
 */
public class LockHandler extends BaseLockHandler implements ExistingEntityHandler {
  private static final String[] METHODS = new String[]{Request.Method.LOCK.code};

  public LockHandler(WebDavResponseHandler responseHandler, ResourceHandlerHelper resourceHandlerHelper) {
    super(responseHandler, resourceHandlerHelper);
  }

  @Override
  public String[] getMethods() {
    return METHODS;
  }

  @Override
  public void doProcessExistingResource(HttpManager manager, Request request, Response response, Resource resource) throws NotAuthorizedException, BadRequestException, ConflictException, IOException, SAXException, LockedException, PreConditionFailedException {
    LockableResource lockableResource = (LockableResource) resource;
    LockToken currentLock = lockableResource.getCurrentLock();
    if (currentLock != null) {
      processLockedResource(manager, request, response, lockableResource, currentLock);
    } else {
      processUnlockedResource(manager, request, response, lockableResource);
    }
  }

  private static void sendLockFailure(LockResult lockResult, Response response) {
    switch (lockResult.getFailureReason()) {
      case ALREADY_LOCKED:
        response.sendError(Response.Status.SC_LOCKED, "This resource is already locked");
        break;
      case PRECONDITION_FAILED:
        response.sendError(Response.Status.SC_PRECONDITION_FAILED, "");
        break;
      default:
        response.sendError(Response.Status.SC_INTERNAL_SERVER_ERROR, "");
    }
  }

  private void processUnlockedResource(HttpManager manager, Request request, Response response, LockableResource resource) throws IOException, SAXException, NotAuthorizedException, LockedException, PreConditionFailedException {
    LockTimeout timeout = LockTimeout.parseTimeout(request);
    LockInfo lockInfo = LockInfoSaxHandler.parseLockInfo(request);
    if (lockInfo.type == null || lockInfo.scope == null) {
      myResponseHandler.respondBadRequest(resource, response, request);
      return;
    }
    LockResult lockResult = resource.lock(timeout, lockInfo);
    if (lockResult.isSuccessful()) {
      respondLockStatus(response, lockResult);
      return;
    }
    sendLockFailure(lockResult, response);
  }

  private void processLockedResource(HttpManager manager, Request request, Response response, LockableResource resource, LockToken currentLock) throws IOException, SAXException, NotAuthorizedException, PreConditionFailedException {
    LockInfo lockInfo = LockInfoSaxHandler.parseLockInfo(request);
    if (lockInfo.type == null || lockInfo.scope == null) {
      myResponseHandler.respondBadRequest(resource, response, request);
      return;
    }
    if (!StringUtils.equals(request.getIfHeader(), currentLock.tokenId)) {
      response.sendError(Response.Status.SC_PRECONDITION_FAILED, "Token mismatch");
      return;
    }
    LockResult lockResult = resource.refreshLock(request.getIfHeader());
    if (lockResult.isSuccessful()) {
      if (lockResult.getLockToken() == null) {
        response.sendError(Response.Status.SC_INTERNAL_SERVER_ERROR, "Lock acquisition failed");
        return;
      }
      respondLockStatus(response, lockResult);
      return;
    }
    sendLockFailure(lockResult, response);
  }

  private void respondLockStatus(Response response, LockResult lockResult) {
    response.setLockTokenHeader(lockResult.getLockToken().tokenId);
    response.setContentTypeHeader(Response.XML);
    XmlWriter xmlWriter = new XmlWriter(response.getOutputStream());
    xmlWriter.writeXMLHeader();
    xmlWriter.writeElement(WebDavProtocol.NS_DAV.getPrefix(), WebDavProtocol.DAV_URI, "prop", XmlWriter.Type.OPENING);
    LockTokenValueWriter valueWriter = new LockTokenValueWriter();
    valueWriter.writeLockDiscovery(xmlWriter, lockResult.getLockToken());
    xmlWriter.writeElement(WebDavProtocol.NS_DAV.getPrefix(), WebDavProtocol.DAV_URI, "prop", XmlWriter.Type.CLOSING);
  }
}
