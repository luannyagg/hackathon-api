package org.caixa.filters;


import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Provider
@ApplicationScoped
public class MetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Inject
    MeterRegistry meterRegistry;

    private static final String START_NS = "start-ns";
    private static final String TIMER_PREFIX = "http_server_time_ms";
    private static final String COUNT_PREFIX = "http_server_count";
    private static final String SUCCESS_PREFIX = "http_server_success";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        containerRequestContext.setProperty(START_NS, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        String path = containerRequestContext.getUriInfo().getPath();
        String method = containerRequestContext.getMethod();

        long start = (long) containerRequestContext.getProperty(START_NS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        boolean success = containerResponseContext.getStatus() < 500;

        meterRegistry.timer(TIMER_PREFIX, "endpoint", path, "method", method).record(elapsedMs, TimeUnit.MILLISECONDS);
        meterRegistry.counter(COUNT_PREFIX, "endpoint", path, "method", method).increment();
        meterRegistry.counter(SUCCESS_PREFIX, "endpoint", path, "method", method, "success", String.valueOf(success)).increment();
    }
}
