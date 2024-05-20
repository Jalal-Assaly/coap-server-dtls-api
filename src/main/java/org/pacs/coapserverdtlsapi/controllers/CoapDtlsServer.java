package org.pacs.coapserverdtlsapi.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.MdcConnectionListener;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.pacs.coapserverdtlsapi.config.CredentialsUtil;
import org.pacs.coapserverdtlsapi.models.AccessResponseModel;
import org.pacs.coapserverdtlsapi.services.CoapService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.net.InetSocketAddress;

@Component
@RequiredArgsConstructor
public class CoapDtlsServer {

    private final CoapService coapService;

    // Setup properties file
    private static final File CONFIG_FILE = new File("../coap-server-dtls-api/Californium3SecureServer.properties");
    private static final String CONFIG_HEADER = "Californium CoAP Properties file for Secure Server";

    // Add custom properties to default properties
    private static final Configuration.DefinitionsProvider DEFAULTS = config -> {
        config.set(DtlsConfig.DTLS_ROLE, DtlsConfig.DtlsRole.SERVER_ONLY);
        config.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, false);
        config.set(DtlsConfig.DTLS_PRESELECTED_CIPHER_SUITES, CipherSuite.STRONG_ENCRYPTION_PREFERENCE);
    };

    @Bean
    public void coapServer() {
        // Register default CoAP + DTLS configuration profiles
        CoapConfig.register();
        DtlsConfig.register();

        // Set standards
        Configuration configuration = Configuration.createWithFile(CONFIG_FILE, CONFIG_HEADER, DEFAULTS);
        Configuration.setStandard(configuration);

        // Define port number
        int dtlsPort = configuration.get(CoapConfig.COAP_SECURE_PORT);

        // Configure DTLS connector
        DtlsConnectorConfig.Builder builder = DtlsConnectorConfig.builder(configuration)
                .setAddress(new InetSocketAddress(dtlsPort));
        CredentialsUtil.setupCredentials(builder);
        builder.setConnectionListener(new MdcConnectionListener());
        DTLSConnector connector = new DTLSConnector(builder.build());
        CoapEndpoint.Builder coapBuilder = new CoapEndpoint.Builder()
                .setConfiguration(configuration)
                .setConnector(connector);

        // Create CoAP server
        CoapServer server = new CoapServer();
        server.add(new AccessControlResource(coapService));
        server.addEndpoint(coapBuilder.build());
        server.start();

        // add special interceptor for message traces
        for (Endpoint ep : server.getEndpoints()) {
            ep.addInterceptor(new MessageTracer());
        }

        System.out.println("Secure CoAP server powered by Scandium (Sc) is listening on port " + dtlsPort);
    }

    private static class AccessControlResource extends CoapResource {
        private final CoapService coapService;
        public AccessControlResource(CoapService coapService) {
            // Define URI
            super("accessControl");
            // Declare coapService inside class scope
            this.coapService = coapService;
        }

        @Override
        public void handlePOST(CoapExchange exchange) {

            // Endpoint to be called
            String endpoint = "employee";
            AccessResponseModel response;

            // Received request
            Request request = exchange.advanced().getRequest();
            System.out.println(Utils.prettyPrint(request));

            // Visitor or employee role
            byte[] payload = request.getPayload();
            try {
                JsonObject actualJson = JsonParser.parseString(new String(payload)).getAsJsonObject();
                if ("Visitor".equals(actualJson.get("UAT").getAsJsonObject().get("RL").getAsString())) {
                    System.out.println("Found visitor");
                    endpoint = "visitor";
                }
            } catch (Exception e) {
                response = new AccessResponseModel(false);
            }

            // Communicate with ABAC Model
            try {
                response = coapService.sendAccessRequest(endpoint, request.getPayloadString());
            } catch (WebClientResponseException exception) {
                response = new AccessResponseModel(false);
            }

            // Send response
            String jsonResponse;
            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            try {
                jsonResponse = prettyGson.toJson(response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            exchange.respond(CoAP.ResponseCode.CONTENT, jsonResponse, 50);
        }
    }
}