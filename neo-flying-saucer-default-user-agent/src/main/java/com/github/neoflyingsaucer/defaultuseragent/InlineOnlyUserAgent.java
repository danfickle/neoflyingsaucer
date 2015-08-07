/*
 * InlineOnlyUserAgent.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package com.github.neoflyingsaucer.defaultuseragent;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

import org.w3c.dom.Document;

import com.github.neoflyingsaucer.event.DocumentListener;
import com.github.neoflyingsaucer.extend.controller.error.FSErrorController;
import com.github.neoflyingsaucer.extend.controller.error.LangId;
import com.github.neoflyingsaucer.extend.controller.error.FSError.FSErrorLevel;
import com.github.neoflyingsaucer.extend.output.FSImage;
import com.github.neoflyingsaucer.extend.useragent.CSSResourceI;
import com.github.neoflyingsaucer.extend.useragent.HTMLResourceI;
import com.github.neoflyingsaucer.extend.useragent.ImageResourceI;
import com.github.neoflyingsaucer.extend.useragent.Optional;
import com.github.neoflyingsaucer.extend.useragent.ResourceCache;
import com.github.neoflyingsaucer.extend.useragent.StylesheetI;
import com.github.neoflyingsaucer.extend.useragent.UserAgentCallback;
import com.github.neoflyingsaucer.resource.HTMLResource;
import com.github.neoflyingsaucer.resource.ImageResource;
import com.github.neoflyingsaucer.util.GeneralUtil;
import com.github.neoflyingsaucer.util.ImageUtil;

/**
 * Use this class when you don't want any remote or local resources fetched, only passed in html.
 */
public class InlineOnlyUserAgent implements UserAgentCallback, DocumentListener 
{
    private ResourceCache _resourceCache = new ResourceCache() {

		@Override
		public Optional<Document> getHtmlDocument(String resolvedUri) {
			return Optional.empty();
		}

		@Override
		public void putHtmlDocument(String resolvedUri, Document doc) {
		}

		@Override
		public Optional<StylesheetI> getCssStylesheet(String resolvedUri) {
			return Optional.empty();
		}

		@Override
		public void putCssStylesheet(String resolvedUri, StylesheetI sheet) {
		}

		@Override
		public void putImage(String resolvedUri, Class<?> imgType, FSImage img) {
		}

		@Override
		public Optional<FSImage> getImage(String resolvedUri, Class<?> imgType) {
			return Optional.empty();
		}
    };

    public InlineOnlyUserAgent() {}

    @Override
    public Optional<CSSResourceI> getCSSResource(String uri)
    {
    	return Optional.empty();
    }

    @Override
    public Optional<ImageResourceI> getImageResource(String uri) 
    {
        if (ImageUtil.isEmbeddedBase64Image(uri)) 
        {
            InputStream image = ImageUtil.loadEmbeddedBase64Image(uri);
            ImageResource ir = createImageResource(null, image);
            return Optional.<ImageResourceI>ofNullable(ir);
        }
        else
        {
        	return Optional.empty();
        }
    }

    /**
     * Factory method to generate ImageResources from a given Image. May be overridden in subclass. 
     *
     * @param uri The URI for the image, resolved to an absolute URI.
     * @param img The image to package; may be null (for example, if image could not be loaded).
     *
     * @return An ImageResource containing the image.
     */
    protected ImageResource createImageResource(String uri, InputStream img) 
    {
        return new ImageResource(uri, img);
    }

    /**
     * Retrieves the XML located at the given URI. It's assumed the URI does point to a XML--the URI will
     * be accessed (using java.io or java.net), opened, read and then passed into the XML parser (XMLReader)
     * configured for Flying Saucer. The result is packed up into an XMLResource for later consumption.
     *
     * @param uri Location of the XML source.
     * @return An XMLResource containing the image.
     */
    @Override
    public Optional<HTMLResourceI> getHTMLResource(String uri) 
    {
       	return Optional.empty();
    }

    @Override
    public Optional<byte[]> getBinaryResource(String uri)
    {
    	return Optional.empty();
    }

    /**
     * Returns true if the given URI was visited, meaning it was requested at some point since initialization.
     *
     * @param uri A URI which might have been visited.
     * @return Always false; visits are not tracked in the NaiveUserAgent.
     */
    @Override
    public boolean isVisited(String uri) 
    {
        return false;
    }

    /**
     * Resolves the base URI/absolute URI pair.
     * If absolute, leaves as is, if relative, returns an absolute URI
     * based on the baseUri and uri.
     * You may need to only override this method if your URIs resolve to 
     * to one of the following URL protocols: HTTP, HTTPS, JAR, FILE.
	 * This method is always called before requesting a resource.
	 * 
     * @param baseUri A base URI. May be null, in which case the uri must be absolute.
     * @param uri A URI, possibly relative.
	 *
     * @return A URI as String, resolved, or null if there was an exception (for example if the URI is malformed).
     */
    @Override
    public Optional<String> resolveURI(String baseUri, String uri) 
    {
        if (uri == null && baseUri == null)
        	return Optional.empty();

        if (baseUri == null) 
        {
        	try 
        	{
        		URI result = new URI(uri);
        		return Optional.of(result.normalize().toString());
        	}
        	catch (URISyntaxException e)
        	{
        		FSErrorController.log(InlineOnlyUserAgent.class, FSErrorLevel.ERROR, LangId.INVALID_URI, uri);
        		return Optional.empty();
        	}
        }
        else
        {
        	try
        	{
        		URI base = new URI(baseUri);
        		URI rel = new URI(uri);

        		URI absolute = base.resolve(rel);
        		return Optional.of(absolute.normalize().toString());
        	}
        	catch (URISyntaxException e)
        	{
        		FSErrorController.log(InlineOnlyUserAgent.class, FSErrorLevel.ERROR, LangId.INVALID_BASE_URI_PAIR, baseUri, uri);
        		return Optional.empty();
        	}
        }
    }

    @Override
    public void documentStarted() {}

    @Override
    public void documentLoaded() { /* ignore*/ }

    @Override
    public void onLayoutException(final Throwable t) { /* ignore*/ }

    @Override
    public void onRenderException(final Throwable t) { /* ignore*/ }

	/**
	 * Used internally when a document can't be loaded--returns XHTML as an XMLResource indicating that fact.
	 *
	 * @param uri The URI which could not be loaded.
	 *
	 * @return An XMLResource containing XML which about the failure.
	 */
	@Override
	public HTMLResourceI getErrorDocument(final String uri, int errorCode) 
	{
        HTMLResourceHelper xr;

        // URI may contain & symbols which can "break" the XHTML we're creating
        final String cleanUri = GeneralUtil.escapeHTML(uri);
        final String notFound = "<html><h1>Document not found</h1><h2>" + errorCode + "</h2>" + "<p>Could not access URI <pre>" + cleanUri + "</pre></p></html>";

        xr = HTMLResourceHelper.load(notFound);
        return new HTMLResource("about:error", xr.getDocument());
    }

	@Override
	public ResourceCache getResourceCache() 
	{
		return _resourceCache;
	}

	public void setResourceCache(ResourceCache cache)
	{
		_resourceCache = cache;
	}
	
	@Override
	public Optional<HTMLResourceI> parseHTMLResource(String uri, String html) 
	{
		HTMLResourceHelper helper = HTMLResourceHelper.load(html); 

		if (helper.getDocument() != null)
			return Optional.<HTMLResourceI>of(new HTMLResource(uri, helper.getDocument()));
		else
			return Optional.empty();
	}

	@Override
	public Optional<HTMLResourceI> parseHTMLResource(String uri, File html) 
	{
		return Optional.empty();
	}

	@Override
	public Optional<HTMLResourceI> parseHTMLResource(String uri, Reader html) 
	{
		return Optional.empty();
	}
}
