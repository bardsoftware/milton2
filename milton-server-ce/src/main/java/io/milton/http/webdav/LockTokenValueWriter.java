// Copyright (C) 2015 BarD Software
package io.milton.http.webdav;

import io.milton.http.LockToken;
import io.milton.http.Request;
import io.milton.http.XmlWriter;
import io.milton.http.values.ValueWriter;

import java.util.Map;

/**
 * @author dbarashev@bardsoftware.com
 */
public class LockTokenValueWriter implements ValueWriter {
  @Override
  public boolean supports(String nsUri, String localName, Class valueClass) {
    return LockToken.class.isAssignableFrom(valueClass) && WebDavProtocol.DAV_URI.equals(nsUri) && "lockdiscovery".equals(localName);
  }

  @Override
  public void writeValue(XmlWriter xmlWriter, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes) {
    if (val == null) {
      return;
    }
    LockToken lockToken = (LockToken) val;
    writeLockDiscovery(xmlWriter, lockToken);
  }

  public void writeLockDiscovery(XmlWriter xmlWriter, LockToken lockToken) {
    xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "lockdiscovery");
    xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "activelock");
    {
      xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "locktype");
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "write");
      xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "locktype");

      xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "lockscope");
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), lockToken.info.scope.name().toLowerCase());
      xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "lockscope");

      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "depth", Request.Depth.INFINITY.name().toLowerCase());
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "owner", lockToken.info.lockedByUser);
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "timeout", lockToken.timeout.toString());

      xmlWriter.open(WebDavProtocol.NS_DAV.getPrefix(), "locktoken");
      xmlWriter.writeProperty(WebDavProtocol.NS_DAV.getPrefix(), "href", lockToken.tokenId);
      xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "locktoken");
    }
    xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "activelock");
    xmlWriter.close(WebDavProtocol.NS_DAV.getPrefix(), "lockdiscovery");
  }

  @Override
  public Object parse(String namespaceURI, String localPart, String value) {
    return null;
  }
}
