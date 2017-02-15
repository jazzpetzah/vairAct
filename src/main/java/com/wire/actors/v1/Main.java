package com.wire.actors.v1;

import com.wire.actors.v1.common.Utils;
import com.wire.actors.v1.service.sync_engine_bridge.SEBridgeService;
import com.wire.actors.v1.webapp.JSONProvider;
import com.wire.actors.v1.webapp.WebApplication;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.http.*;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class.getSimpleName());

    private static final String ROOT_URL = String.format("http://0.0.0.0:%s/%s",
            Utils.getProperty("servicePort"),
            Utils.getProperty("serviceRoot")
    );

    private static HttpServer startServer() {
        final ResourceConfig rc = ResourceConfig.forApplicationClass(WebApplication.class)
                .register(JSONProvider.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(ROOT_URL), rc);
    }

    private static HttpServer configureLogging(HttpServer server) {
        final FilterChain filterChain = server.getListener("grizzly").getFilterChain();

        HttpCodecFilter codecFilter = (HttpCodecFilter) filterChain.get(filterChain.indexOfType(HttpCodecFilter.class));
        codecFilter.getMonitoringConfig().addProbes(new HttpProbe() {
            @Override
            public void onDataReceivedEvent(Connection connection, Buffer buffer) {
                LOG.debug(buffer.toStringContent());            // Log incoming traffic
            }

            @Override
            public void onDataSentEvent(Connection connection, Buffer buffer) {
                LOG.debug(buffer.toStringContent());             // // Log outgoing traffic
            }

            @Override
            public void onHeaderParseEvent(Connection connection, HttpHeader header, int size) {
            }

            @Override
            public void onHeaderSerializeEvent(Connection connection, HttpHeader header, Buffer buffer) {
            }

            @Override
            public void onContentChunkParseEvent(Connection connection, HttpContent content) {
            }

            @Override
            public void onContentChunkSerializeEvent(Connection connection, HttpContent content) {
            }

            @Override
            public void onContentEncodingParseEvent(Connection connection, HttpHeader header, Buffer buffer,
                                                    ContentEncoding contentEncoding) {
            }

            @Override
            public void onContentEncodingParseResultEvent(Connection connection, HttpHeader httpHeader, Buffer buffer,
                                                          ContentEncoding contentEncoding) {
            }

            @Override
            public void onContentEncodingSerializeEvent(Connection connection, HttpHeader header, Buffer buffer,
                                                        ContentEncoding contentEncoding) {
            }

            @Override
            public void onContentEncodingSerializeResultEvent(Connection connection, HttpHeader httpHeader,
                                                              Buffer buffer, ContentEncoding contentEncoding) {
            }

            @Override
            public void onTransferEncodingParseEvent(Connection connection, HttpHeader header, Buffer buffer,
                                                     TransferEncoding transferEncoding) {
            }

            @Override
            public void onTransferEncodingSerializeEvent(Connection connection, HttpHeader header, Buffer buffer,
                                                         TransferEncoding transferEncoding) {
            }

            @Override
            public void onErrorEvent(Connection connection, HttpPacket httpPacket, Throwable error) {
            }
        });

        return server;
    }

    public static void main(String[] args) {
        final HttpServer server = configureLogging(startServer());
        try {
            // This is to init the cache immediately
            SEBridgeService.getInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            while (true) {
                Thread.sleep(1000 * 60 * 60);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.shutdown();
    }
}
