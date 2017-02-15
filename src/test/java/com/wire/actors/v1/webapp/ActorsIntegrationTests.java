package com.wire.actors.v1.webapp;

import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import com.wire.actors.v1.common.Utils;
import com.wire.actors.v1.model.DeviceInfo;
import com.wire.actors.v1.model.DevicesInfo;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import java.net.URI;

public class ActorsIntegrationTests {
    private static final String ROOT_URL = String.format("http://0.0.0.0:%s/%s",
            Utils.getProperty("servicePort"),
            Utils.getProperty("serviceRoot")
    );
    private Client client;
    private HttpServer server;

    private Invocation.Builder query(String endpoint) {
        return client
                .target(String.format("%s/%s", ROOT_URL, endpoint))
                .request()
                .accept(MediaType.APPLICATION_JSON);
    }

    private static HttpServer startServer() {
        final ResourceConfig rc = ResourceConfig.forApplicationClass(WebApplication.class)
                .register(JSONProvider.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(ROOT_URL), rc);
    }

    @Before
    public void setup() {
        this.server = startServer();
        this.client = ClientBuilder.newClient();
    }

    @Test
    public void testCreateDevices() throws Exception {
        final DeviceInfo testModel = new DeviceInfo();
        testModel.setName("sameName");
        for (int i = 0; i < 2; i++) {
            final DeviceInfo resultModel = query("devices/create")
                    .post(Entity.json(testModel))
                    .readEntity(DeviceInfo.class);
            assertThat(resultModel.getUuid(), not(isEmptyString()));
        }
        final DevicesInfo resultModel = query("devices")
                .get()
                .readEntity(DevicesInfo.class);
        assertThat(resultModel.getDevices().size(), equalTo(2));
    }

    @After
    public void tearDown() {
        server.shutdown();
    }

}
