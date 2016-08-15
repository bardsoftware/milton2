// Copyright (C) 2016 BarD Software
package io.milton.http.webdav;

import io.milton.resource.PropFindableResource;

/**
 * @author dbarashev@bardsoftware.com
 */
public class SupportedLocksProperty implements PropertyMap.StandardProperty<SupportedLocks> {
  @Override
  public String fieldName() {
    return "supportedlocks";
  }

  @Override
  public SupportedLocks getValue(PropFindableResource res) {
    return new SupportedLocks(res);
  }

  @Override
  public Class getValueClass() {
    return SupportedLocks.class;
  }
}
