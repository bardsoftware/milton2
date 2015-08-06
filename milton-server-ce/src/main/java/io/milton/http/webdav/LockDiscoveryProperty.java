// Copyright (C) 2015 BarD Software
package io.milton.http.webdav;

import io.milton.http.LockToken;
import io.milton.resource.LockableResource;
import io.milton.resource.PropFindableResource;

/**
 * @author dbarashev@bardsoftware.com
 */
public class LockDiscoveryProperty implements PropertyMap.StandardProperty<LockToken> {
  @Override
  public String fieldName() {
    return "lockdiscovery";
  }

  @Override
  public LockToken getValue(PropFindableResource res) {
    if (res instanceof LockableResource == false) {
      return null;
    }
    LockableResource resLockable = (LockableResource) res;
    return resLockable.getCurrentLock();
  }

  @Override
  public Class getValueClass() {
    return LockToken.class;
  }
}
