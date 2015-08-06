// Copyright (C) 2015 BarD Software
package io.milton.http.webdav;

import io.milton.http.ExistingEntityHandler;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.ResourceHandlerHelper;
import io.milton.http.Response;
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
public abstract class BaseLockHandler implements ExistingEntityHandler {
  protected final ResourceHandlerHelper myResourceHandlerHelper;
  protected final WebDavResponseHandler myResponseHandler;

  public BaseLockHandler(WebDavResponseHandler responseHandler, ResourceHandlerHelper resourceHandlerHelper) {
    myResponseHandler = responseHandler;
    myResourceHandlerHelper = resourceHandlerHelper;
  }

  public boolean isCompatible(Resource res) {
    return res instanceof LockableResource;
  }

  public void process(HttpManager httpManager, Request request, Response response) throws ConflictException, NotAuthorizedException, BadRequestException, NotFoundException {
    myResourceHandlerHelper.process(httpManager, request, response, this);
  }

  public void processResource(HttpManager manager, Request request, Response response, Resource r) throws NotAuthorizedException, ConflictException, BadRequestException {
    myResourceHandlerHelper.processResource(manager, request, response, r, this);
  }

  public void processExistingResource(HttpManager manager, Request request, Response response, Resource resource) throws NotAuthorizedException, BadRequestException, ConflictException, NotFoundException {
    try {
      doProcessExistingResource(manager, request, response, resource);
    } catch (IOException | SAXException e) {
      //log(e);
      throw new RuntimeException(e);
    } catch (LockedException e) {
      myResponseHandler.respondLocked(request, response, resource);
    } catch (PreConditionFailedException e) {
      myResponseHandler.respondPreconditionFailed(request, response, resource);
    }
  }

  public abstract void doProcessExistingResource(HttpManager manager, Request request, Response response, Resource resource) throws NotAuthorizedException, BadRequestException, ConflictException, IOException, SAXException, LockedException, PreConditionFailedException;
}
