// Copyright (C) 2015 BarD Software
package io.milton.http.webdav;

import io.milton.http.ExistingEntityHandler;
import io.milton.http.HttpManager;
import io.milton.http.LockToken;
import io.milton.http.Request;
import io.milton.http.ResourceHandlerHelper;
import io.milton.http.Response;
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
public class UnlockHandler extends BaseLockHandler implements ExistingEntityHandler {
  private static final String[] METHODS = new String[]{Request.Method.UNLOCK.code};

  public UnlockHandler(WebDavResponseHandler responseHandler, ResourceHandlerHelper resourceHandlerHelper) {
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
    if (currentLock == null) {
      response.sendError(Response.Status.SC_NO_CONTENT, "Not locked");
      return;
    }
    if (StringUtils.equals(request.getIfHeader(), currentLock.tokenId)) {
      ((LockableResource) resource).unlock(request.getIfHeader());
      response.sendError(Response.Status.SC_NO_CONTENT, "Unlocked OK");
      return;
    }
    response.sendError(Response.Status.SC_PRECONDITION_FAILED, "");
  }
}
