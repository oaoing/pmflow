package com.oaoing.pmflow.common.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PrincipalAuditorAware implements AuditorAware<String> {
    private static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(SYSTEM);
        // return ActorContext.get().orElse(SYSTEM);
    }
}
