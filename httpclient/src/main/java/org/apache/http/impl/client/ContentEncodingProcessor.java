package org.apache.http.impl.client;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * Class responsible for handling Content Encoding in HTTP. This takes the form of 
 * an {@link HttpRequestInterceptor} that will modify the {@link HttpRequest} if the client hasn't 
 * already specified an <code>Accept-Encoding</code> header. There is an accompanying 
 * {@link HttpResponseInterceptor} implementation that will only examine the {@link HttpResponse} 
 * if the {@link HttpRequestInterceptor} implementation did any modifications.
 * <p>
 * Instances of this class are stateless, therefore they're thread-safe and immutable.
 * 
 * @see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.5
 */
class ContentEncodingProcessor implements HttpResponseInterceptor, HttpRequestInterceptor {

    /**
     * {@inheritDoc}
     */
    public void process(
            HttpRequest request, HttpContext context) throws HttpException, IOException {

        /*
         * If a client of this library has already set this header, presume that they did so for 
         * a reason and so this instance shouldn't handle the response at all.
         */
        if (!request.containsHeader("Accept-Encoding")) {

            /* Signal support for Accept-Encoding transfer encodings. */
            // TODO add compress support.
            request.addHeader("Accept-Encoding", "gzip,deflate");

            /* Store the fact that the request was modified, so that we can potentially handle
             *  the response. */
            context.setAttribute(ContentEncodingProcessor.class.getName(), Boolean.TRUE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(
            HttpResponse response, HttpContext context) throws HttpException, IOException {
        
        if (context.getAttribute(ContentEncodingProcessor.class.getName()) != null) {
            HttpEntity entity = response.getEntity();

            if (entity != null) { // It wasn't a 304 Not Modified response, 204 No Content or similar
                Header ceheader = entity.getContentEncoding();
                if (ceheader != null) {
                    HeaderElement[] codecs = ceheader.getElements();
                    for (int i = 0, n = codecs.length; i < n; ++i) {
                        if ("gzip".equalsIgnoreCase(codecs[i].getName())) {
                            response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                            return;
                        } else if ("deflate".equalsIgnoreCase(codecs[i].getName())) {
                            response.setEntity(new DeflateDecompressingEntity(response.getEntity()));
                            return;
                        }
                        // TODO add compress. identity is a no-op.
                    }
                }
            }
        }
    }

}
