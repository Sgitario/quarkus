package io.quarkus.kubernetes.deployment.config;

import io.smallrye.config.FallbackConfigSourceInterceptor;

public class KubernetesToOpenShiftFallbackConfigSourceInterceptor extends FallbackConfigSourceInterceptor {
    public KubernetesToOpenShiftFallbackConfigSourceInterceptor() {
        super(name -> {
            return name.startsWith("quarkus.kubernetes") ? name.replaceAll("quarkus\\.kubernetes", "quarkus.openshift")
                    : name;
        });
    }
}
