package org.caixa.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.caixa.dto.TelemetriaDTO;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Path("/telemetria")
public class TelemetriaController {

    @Inject
    MeterRegistry registry;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TelemetriaDTO stats() {
        // Volume por endpoint (somando todas as chamadas)
        Map<String, Long> volume = registry.get("http_server_count").counters().stream()
                .collect(Collectors.groupingBy(
                        c -> c.getId().getTag("endpoint"),
                        Collectors.summingLong(c -> (long) c.count())
                ));

        // Sucesso por endpoint (% de requisições com status < 500)
        Map<String, Double> sucessoPct = new HashMap<>();
        List<Counter> successCounters = (List<Counter>) registry.find("http_server_success").counters();

        Map<String, Double> ok = successCounters.stream()
                .filter(c -> "true".equals(c.getId().getTag("success")))
                .collect(Collectors.groupingBy(
                        c -> c.getId().getTag("endpoint"),
                        Collectors.summingDouble(Counter::count)
                ));

        Map<String, Double> total = successCounters.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getId().getTag("endpoint"),
                        Collectors.summingDouble(Counter::count)
                ));

        for (String ep : total.keySet()) {
            double pct = total.get(ep) == 0 ? 0.0 : (ok.getOrDefault(ep, 0.0) / total.get(ep)) * 100.0;
            sucessoPct.put(ep, Math.round(pct * 100.0) / 100.0); // arredondamento para 2 casas
        }

        // Tempo médio por endpoint (em milissegundos)
        Map<String, Double> tempoMedio = registry.get("http_server_time_ms").timers().stream()
                .collect(Collectors.groupingBy(
                        t -> t.getId().getTag("endpoint"),
                        Collectors.averagingDouble(t -> {
                            double totalTimeMs = t.totalTime(TimeUnit.MILLISECONDS);
                            long count = t.count();
                            return count == 0 ? 0.0 : totalTimeMs / count;
                        })
                ));

        return new TelemetriaDTO(volume, tempoMedio, sucessoPct);
    }
}