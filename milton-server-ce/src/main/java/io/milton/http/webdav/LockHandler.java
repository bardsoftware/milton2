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
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.LockableResource;
import io.milton.resource.Resource;
import org.xml.sax.SAXException;

import java.io.IOException;


/**
 * @author dbarashev@bardsoftware.com
 */
public class LockHandler implements ExistingEntityHandler {
  private static final String[] METHODS = new String[]{Request.Method.LOCK.code};
  private final ResourceHandlerHelper myResourceHandlerHelper;
  private final WebDavResponseHandler myResponseHandler;

  public LockHandler(WebDavResponseHandler responseHandler, ResourceHandlerHelper resourceHandlerHelper) {
    myResponseHandler = responseHandler;
    myResourceHandlerHelper = resourceHandlerHelper;
  }

  @Override
  public String[] getMethods() {
    return METHODS;
  }

  @Override
  public boolean isCompatible(Resource res) {
    return res instanceof LockableResource;
  }

  @Override
  public void process(HttpManager httpManager, Request request, Response response) throws ConflictException, NotAuthorizedException, BadRequestException, NotFoundException {
    myResourceHandlerHelper.process(httpManager, request, response, this);
  }

  @Override
  public void processResource(HttpManager manager, Request request, Response response, Resource r) throws NotAuthorizedException, ConflictException, BadRequestException {
    myResourceHandlerHelper.processResource(manager, request, response, r, this);
  }

  @Override
  public void processExistingResource(HttpManager manager, Request request, Response response, Resource resource) throws NotAuthorizedException, BadRequestException, ConflictException, NotFoundException {
    try {
      doProcessExistingResource(manager, request, response, resource);
    } catch (IOException e) {
      //log(e);
      throw new RuntimeException(e);
    } catch (SAXException e) {
      //log(e);
      throw new RuntimeException(e);
    } catch (LockedException e) {
      myResponseHandler.respondLocked(request, response, resource);
    } catch (PreConditionFailedException e) {
      myResponseHandler.respondPreconditionFailed(request, response, resource);
    }
  }

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
    if (!request.getIfHeader().equals(currentLock.tokenId)) {
      response.sendError(Response.Status.SC_PRECONDITION_FAILED, "Token mismatch");
      return;
    }
    LockResult lockResult = resource.refreshLock(request.getIfHeader());
    if (lockResult.isSuccessful()) {
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
    xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "lockdiscovery");
    xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "activelock");
    {
      xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "locktype");
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "write");
      xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "locktype");

      xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "lockscope");
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), lockResult.getLockToken().info.scope.name().toLowerCase());
      xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "lockscope");

      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "depth", Request.Depth.INFINITY.name().toLowerCase());
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "owner", lockResult.getLockToken().info.lockedByUser);
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "timeout", lockResult.getLockToken().timeout.toString());

      xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "locktoken");
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "href", lockResult.getLockToken().tokenId);
      xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "locktoken");
    }
    xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "activelock");
    xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "lockdiscovery");
    xmlWriter.writeElement(WebDavProtocol.NS_DAV.getPrefix(), WebDavProtocol.DAV_URI, "prop", XmlWriter.Type.CLOSING);
  }


}
