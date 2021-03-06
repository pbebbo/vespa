// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.jdisc.http.server.jetty;

import com.google.inject.Key;
import com.yahoo.jdisc.Container;
import com.yahoo.jdisc.References;
import com.yahoo.jdisc.ResourceReference;
import com.yahoo.jdisc.Response;
import com.yahoo.jdisc.handler.RequestHandler;
import com.yahoo.jdisc.http.HttpRequest;
import com.yahoo.jdisc.service.CurrentContainer;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Steinar Knutsen
 * @author bjorncs
 */
public class HttpRequestFactoryTest {

    private static final int LOCAL_PORT = 8080;

    @Test
    public final void testIllegalQuery() {
        try {
            HttpRequestFactory.newJDiscRequest(
                    new MockContainer(),
                    createMockRequest("http", "example.com", "/search", "query=\"contains_quotes\""));
            fail("Above statement should throw");
        } catch (RequestException e) {
            assertThat(e.getResponseStatus(), is(Response.Status.BAD_REQUEST));
        }
    }

    @Test
    public final void illegal_unicode_in_query_throws_requestexception() {
        try {
            HttpRequestFactory.newJDiscRequest(
                    new MockContainer(),
                    createMockRequest("http", "example.com", "/search", "query=%c0%ae"));
            fail("Above statement should throw");
        } catch (RequestException e) {
            assertThat(e.getResponseStatus(), is(Response.Status.BAD_REQUEST));
            assertThat(e.getMessage(), equalTo("Query violates RFC 2396: Not valid UTF8! byte C0 in state 0"));
        }
    }

    @Test
    public void request_uri_uses_local_port() {
        HttpRequest request = HttpRequestFactory.newJDiscRequest(
                new MockContainer(),
                createMockRequest("http", "example.com", "/search", "query=value"));
        assertEquals(LOCAL_PORT, request.getUri().getPort());
    }

    private static HttpServletRequest createMockRequest(String scheme, String serverName, String path, String queryString) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpConnection connection = mock(HttpConnection.class);
        ServerConnector connector = mock(ServerConnector.class);
        when(connector.getLocalPort()).thenReturn(LOCAL_PORT);
        when(connection.getCreatedTimeStamp()).thenReturn(System.currentTimeMillis());
        when(connection.getConnector()).thenReturn(connector);
        when(request.getAttribute("org.eclipse.jetty.server.HttpConnection")).thenReturn(connection);
        when(request.getProtocol()).thenReturn("HTTP/1.1");
        when(request.getScheme()).thenReturn(scheme);
        when(request.getServerName()).thenReturn(serverName);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemotePort()).thenReturn(1234);
        when(request.getLocalPort()).thenReturn(LOCAL_PORT);
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(queryString);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }

    private static final class MockContainer implements CurrentContainer {

        @Override
        public Container newReference(URI uri) {
            return new Container() {

                @Override
                public RequestHandler resolveHandler(com.yahoo.jdisc.Request request) {
                    return null;
                }

                @Override
                public <T> T getInstance(Key<T> tKey) {
                    return null;
                }

                @Override
                public <T> T getInstance(Class<T> tClass) {
                    return null;
                }

                @Override
                public ResourceReference refer() {
                    return References.NOOP_REFERENCE;
                }

                @Override
                public void release() {

                }

                @Override
                public long currentTimeMillis() {
                    return 0;
                }
            };
        }
    }

}
