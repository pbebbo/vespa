// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.config.server.http;

import com.yahoo.config.application.api.ApplicationFile;
import com.yahoo.config.application.api.ApplicationPackage;
import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.Capacity;
import com.yahoo.config.provision.ClusterSpec;
import com.yahoo.config.provision.HostFilter;
import com.yahoo.config.provision.HostSpec;
import com.yahoo.config.provision.ProvisionLogger;
import com.yahoo.config.provision.Provisioner;
import com.yahoo.config.provision.TenantName;
import com.yahoo.container.jdisc.HttpRequest;
import com.yahoo.container.jdisc.HttpResponse;
import com.yahoo.path.Path;
import com.yahoo.transaction.NestedTransaction;
import com.yahoo.transaction.Transaction;
import com.yahoo.vespa.config.server.session.DummyTransaction;
import com.yahoo.vespa.config.server.session.LocalSession;
import com.yahoo.vespa.config.server.session.MockSessionZKClient;
import com.yahoo.vespa.config.server.session.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Base class for session handler tests
 *
 * @author hmusum
 */
public class SessionHandlerTest {

    protected String pathPrefix = "/application/v2/session/";
    public static final String hostname = "foo";
    public static final int port = 1337;


    public static HttpRequest createTestRequest(String path, com.yahoo.jdisc.http.HttpRequest.Method method,
                                                Cmd cmd, Long id, String subPath, InputStream data, Map<String, String> properties) {
        return HttpRequest.createTestRequest("http://" + hostname + ":" + port + path + "/" + id + "/" +
                                             cmd.toString() + subPath, method, data, properties);
    }

    public static HttpRequest createTestRequest(String path, com.yahoo.jdisc.http.HttpRequest.Method method,
                                                Cmd cmd, Long id, String subPath, InputStream data) {
        return HttpRequest.createTestRequest("http://" + hostname + ":" + port + path + "/" + id + "/" +
                                             cmd.toString() + subPath, method, data);
    }

    public static HttpRequest createTestRequest(String path, com.yahoo.jdisc.http.HttpRequest.Method method,
                                                Cmd cmd, Long id, String subPath) {
        return HttpRequest.createTestRequest("http://" + hostname + ":" + port + path + "/" + id + "/" +
                                             cmd.toString() + subPath, method);
    }

    public static HttpRequest createTestRequest(String path, com.yahoo.jdisc.http.HttpRequest.Method method,
                                                Cmd cmd, Long id) {
        return createTestRequest(path, method, cmd, id, "");
    }

    public static HttpRequest createTestRequest(String path, com.yahoo.jdisc.http.HttpRequest.Method method) {
        return HttpRequest.createTestRequest("http://" + hostname + ":" + port + path, method);
    }

    public static HttpRequest createTestRequest(String path) {
        return HttpRequest.createTestRequest("http://" + hostname + ":" + port + path, com.yahoo.jdisc.http.HttpRequest.Method.PUT);
    }

    public static String getRenderedString(HttpResponse response) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.render(baos);
        return baos.toString(StandardCharsets.UTF_8);
    }

    public static class MockLocalSession extends LocalSession {

        public Session.Status status;
        private Instant createTime = Instant.now();
        private ApplicationId applicationId;

        public MockLocalSession(long sessionId, ApplicationPackage app) {
            super(TenantName.defaultName(), sessionId, app, new MockSessionZKClient(app), null);
        }

        public MockLocalSession(long sessionId, ApplicationPackage app, ApplicationId applicationId) {
            this(sessionId, app);
            this.applicationId = applicationId;
        }

        public void setStatus(Session.Status status) {
            this.status = status;
        }

        @Override
        public Session.Status getStatus() {
            return this.status;
        }

        @Override
        public Transaction createActivateTransaction() {
            return new DummyTransaction().add((DummyTransaction.RunnableOperation) () -> status = Status.ACTIVATE);
        }

        @Override
        public ApplicationFile getApplicationFile(Path relativePath, Mode mode) {
            return this.applicationPackage.getFile(relativePath);
        }

        @Override
        public ApplicationId getApplicationId() {
            return applicationId;
        }

        @Override
        public Instant getCreateTime() {
            return createTime;
        }

    }

    public enum Cmd {
        PREPARED("prepared"),
        ACTIVE("active"),
        CONTENT("content");
        private final String name;

        Cmd(String s) {
            this.name = s;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class MockProvisioner implements Provisioner {

        public boolean activated = false;
        public boolean removed = false;
        public boolean restarted = false;
        public ApplicationId lastApplicationId;
        public Collection<HostSpec> lastHosts;

        @Override
        public List<HostSpec> prepare(ApplicationId applicationId, ClusterSpec cluster, Capacity capacity, ProvisionLogger logger) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void activate(NestedTransaction transaction, ApplicationId application, Collection<HostSpec> hosts) {
            activated = true;
            lastApplicationId = application;
            lastHosts = hosts;
        }

        @Override
        public void remove(NestedTransaction transaction, ApplicationId application) {
            removed = true;
            lastApplicationId = application;
        }

        @Override
        public void restart(ApplicationId application, HostFilter filter) {
            restarted = true;
            lastApplicationId = application;
        }

    }

}
