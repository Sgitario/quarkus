package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SlashPathRestClientTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloClient.class, HelloResource.class))
            // disable the removal of trailing slash at client side
            .overrideConfigKey("quarkus.rest-client.removes-trailing-slash", "false")
            // disable the removal of trailing slash at server side
            .overrideConfigKey("quarkus.resteasy-reactive.removes-trailing-slash", "false")
            .overrideRuntimeConfigKey("quarkus.rest-client.test.url",
                    "http://localhost:${quarkus.http.test-port:8081}");

    @RestClient
    HelloClient client;

    @Test
    void shouldHello() {
        assertThat(client.echo()).isEqualTo("/hello/");
    }

    @RegisterRestClient(configKey = "test")
    @Path("/hello/")
    public interface HelloClient {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String echo();
    }

    @Path("/hello/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class HelloResource {

        @GET
        public String echo(@Context UriInfo uriInfo) {
            return uriInfo.getPath();
        }
    }
}
