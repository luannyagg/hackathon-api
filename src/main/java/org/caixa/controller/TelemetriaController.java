package org.caixa.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.caixa.dto.EndpointDTO;
import org.caixa.dto.TelemetriaDTO;

import java.time.LocalDate;
import java.util.List;

@Path("/telemetria")
public class TelemetriaController {

    @Inject
    MeterRegistry registry;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TelemetriaDTO getTelemetria() {

        List<EndpointDTO> endpoints = registry.getMeters().stream()
                .filter(m -> m.getId().getName().equals("http.server.requests")) // pega só métricas HTTP
                .map(m -> {
                    Timer timer = registry.get(m.getId().getName()).tags(m.getId().getTags()).timer();

                    String nomeApi = m.getId().getTag("uri");
                    long qtdRequisicoes = timer.count();
                    double tempoMedio = timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
                    double tempoMaximo = timer.max(java.util.concurrent.TimeUnit.MILLISECONDS);

                    // por enquanto o mínimo vai ser 0 (precisa de histograma ativado para calcular)
                    double tempoMinimo = 0;

                    long total = timer.count();
                    double sucesso = registry
                            .counter("http.server.requests", "uri", nomeApi, "status", "200")
                            .count();
                    double percentualSucesso = total > 0 ? sucesso / total : 0.0;

                    return new EndpointDTO(
                            nomeApi,
                            qtdRequisicoes,
                            tempoMedio,
                            tempoMinimo,
                            tempoMaximo,
                            percentualSucesso
                    );
                })
                .toList();

        return new TelemetriaDTO(
                LocalDate.now().toString(),
                endpoints
        );
    }
}
