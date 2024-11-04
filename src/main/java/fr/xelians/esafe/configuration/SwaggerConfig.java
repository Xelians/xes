/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration;

import static fr.xelians.esafe.common.constant.Header.X_APPLICATION_ID;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.*;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * @author Julien Cornille
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Esafe Swagger API", version = "${app.version}"))
@SecurityScheme(
    name = "oauth2Scheme",
    type = OAUTH2,
    flows =
        @OAuthFlows(
            authorizationCode =
                @OAuthFlow(
                    authorizationUrl =
                        "${spring.security.oauth2.resource-server.jwt.issuer-uri}/oauth2/authorize",
                    tokenUrl =
                        "${spring.security.oauth2.resource-server.jwt.issuer-uri}/oauth2/token")))
public class SwaggerConfig {

  @Bean
  public GroupedOpenApi defaultDocket() {
    return GroupedOpenApi.builder()
        .group("all")
        .pathsToMatch("/**")
        .addOperationCustomizer(
            (operation, handlerMethod) -> {

              // Exclude signup from customization
              if ("signup".equals(operation.getOperationId())) {
                return operation;
              }

              // Add security and header customization to other paths
              operation.addSecurityItem(new SecurityRequirement().addList("oauth2Scheme"));
              operation.addParametersItem(
                  new HeaderParameter()
                      .name(X_APPLICATION_ID)
                      .description("Application identifier")
                      .required(false)
                      .schema(new StringSchema()));
              return operation;
            })
        .build();
  }
}
