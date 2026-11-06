package com.openbas.intelligence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OpenBAS Intelligence Platform — Main Application Entry Point
 *
 * Industrial Interoperability and Agentic Building Operations Platform.
 * Connects building automation equipment, collects and normalizes industrial
 * telemetry, coordinates specialized AI agents, and executes approved
 * building-control actions through a deterministic safety layer.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class OpenBasIntelligenceApplication {

    private static final Logger log = LoggerFactory.getLogger(OpenBasIntelligenceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OpenBasIntelligenceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=============================================================");
        log.info("  OpenBAS Intelligence Platform — READY");
        log.info("  Industrial Interoperability & Agentic Building Operations");
        log.info("  Swagger UI: http://localhost:8080/swagger-ui.html");
        log.info("  API Docs:   http://localhost:8080/api-docs");
        log.info("  Actuator:   http://localhost:8080/actuator");
        log.info("=============================================================");
    }
}
