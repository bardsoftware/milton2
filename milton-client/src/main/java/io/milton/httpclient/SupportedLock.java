// Copyright (C) 2016 BarD Software
package io.milton.httpclient;

/**
 * @author dbarashev@bardsoftware.com
 */
public class SupportedLock {
    public final boolean exclusive;
    public final boolean shared;

    SupportedLock(boolean exclusive, boolean shared) {
        this.exclusive = exclusive;
        this.shared = shared;
    }
}
