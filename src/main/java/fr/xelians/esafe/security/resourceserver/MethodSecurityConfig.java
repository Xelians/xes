/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.resourceserver;

import fr.xelians.esafe.security.resourceserver.expression.MethodSecurityExpressionRoot;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;

/**
 * Configuration class used in Spring Security to enable and configure method-level security
 * annotations such as @PreAuthorize, @PostAuthorize, @Secured, and @RolesAllowed.
 *
 * @author Youcef Bouhaddouza
 */
@Configuration
@EnableMethodSecurity
class MethodSecurityConfig {
  @Bean
  static MethodSecurityExpressionHandler createExpressionHandler(RoleHierarchy roleHierarchy) {
    return new DefaultMethodSecurityExpressionHandler() {
      @Override
      public EvaluationContext createEvaluationContext(
          Supplier<Authentication> authentication, MethodInvocation mi) {
        StandardEvaluationContext context =
            (StandardEvaluationContext) super.createEvaluationContext(authentication, mi);
        MethodSecurityExpressionRoot root = new MethodSecurityExpressionRoot(authentication);
        root.setThis(mi.getThis());
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(getTrustResolver());
        root.setRoleHierarchy(roleHierarchy);
        root.setDefaultRolePrefix("");
        context.setRootObject(root);
        return context;
      }
    };
  }
}
