package io.milton.httpclient;

/**
 * Just a wrapper for an etag. If this is present, but the etag is null it means
 *  to do a if-none-match: * (ie ensure there is no resource)
 * If etag is present it will be if-match: (etag), ie ensure the resource is the same
 * 
 * If you pass a null instead of this, then it means do no match checks
 *
 * @author brad
 */
public class IfMatchCheck {
    private final String etag;
    private final boolean setIfMatch;
    private final boolean setIf;

    public IfMatchCheck(String etag) {
    	this(etag, true, false);
    }

    public IfMatchCheck(String etag, boolean setIfMatch, boolean setIf) {
        this.etag = etag;
        this.setIfMatch = setIfMatch;
        this.setIf = setIf;
	}
    public String getEtag() {
        return etag;
    }        

    public boolean isSetIfMatch() { return this.setIfMatch; }

    public boolean isSetIf() { return this.setIf; }
}
