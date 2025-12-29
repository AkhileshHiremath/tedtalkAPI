package org.tedtalk.api.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfiguration {

    private static final String API_TITLE = "TedTalkAPI";
    private static final String API_DESCRIPTION = "REST API for importing and analyzing TedTalk data from CSV files. " +
            "This API allows you to retrieve TedTalk records that have been imported " +
            "from the iO Data - Java assessment CSV file.";
    private static final String API_VERSION = "1.0.0";
    private static final String CONTACT_NAME = "TedTalkAPI";
    private static final String CONTACT_EMAIL = "support@tedxtalk-analyser.local";
    private static final String CONTACT_URL = "https://tedxtalk-analyser.local";
    private static final String LICENSE_NAME = "Apache 2.0";
    private static final String LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0.html";
    private static final String DEV_SERVER_HTTP_URL = "http://localhost:8080";
    private static final String DEV_SERVER_HTTP_DESC = "Development Server (HTTP)";
    private static final String DEV_SERVER_HTTPS_URL = "https://localhost:8443";
    private static final String DEV_SERVER_HTTPS_DESC = "Development Server (HTTPS)";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers());
    }

    private Info createApiInfo() {
        return new Info()
                .title(API_TITLE)
                .description(API_DESCRIPTION)
                .version(API_VERSION)
                .contact(new Contact()
                        .name(CONTACT_NAME)
                        .email(CONTACT_EMAIL)
                        .url(CONTACT_URL))
                .license(new License()
                        .name(LICENSE_NAME)
                        .url(LICENSE_URL));
    }

    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url(DEV_SERVER_HTTP_URL)
                        .description(DEV_SERVER_HTTP_DESC),
                new Server()
                        .url(DEV_SERVER_HTTPS_URL)
                        .description(DEV_SERVER_HTTPS_DESC));
    }
}
