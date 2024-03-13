/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.configuration;

import static fr.xelians.esafe.common.constant.Header.X_APPLICATION_ID;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class SwaggerConfig {

  @Bean
  public GroupedOpenApi defaultDocket() {
    return GroupedOpenApi.builder()
        .group("all")
        .pathsToMatch("/**")
        .addOperationCustomizer(
            (operation, handlerMethod) -> {
              operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));

              operation.addParametersItem(
                  new HeaderParameter()
                      .name(X_APPLICATION_ID)
                      .description("application identifier")
                      .required(false)
                      .schema(new StringSchema()));
              return operation;
            })
        .build();
  }
}
