package com.oaoing.pmflow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(
        auditorAwareRef = "principalAuditorAware"
)
@Configuration
public class JpaAuditConfig {
}
