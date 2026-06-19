package com.monitor;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentApplication {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String SERVER_ID = "node-win-prod-01"; 

    // Define peak threshold limits for alerting systems
    private static final double CPU_HIGH_PRIORITY_THRESHOLD = 90.0;
    private static final double CPU_LOW_PRIORITY_THRESHOLD = 75.0;
    
    private static final double MEM_HIGH_PRIORITY_THRESHOLD = 85.0;
    private static final double MEM_LOW_PRIORITY_THRESHOLD = 70.0;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("[INFO] Distributed Telemetry Agent Active Loop Configured.");
        System.out.println("[INFO] System polling interval set to: 1 Minute.");
        System.out.println("====================================================");

        FirebaseManager.initializeFirebase();

        // Execution loop sets up to fire exactly every 1 minute
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1. Scrape the native OS statistics
                Map<String, Object> currentMetrics = OSMetricsScanner.getSystemMetrics();
                
                if (currentMetrics.containsKey("error")) {
                    System.err.println("[SKIP] Metrics collection failed: " + currentMetrics.get("error"));
                    return;
                }

                // 2. Overwrite the real-time active state for the dashboard UI
                FirebaseManager.updateActiveState(SERVER_ID, currentMetrics);
                
                // 3. Peak Analytics Optimization Processing
                double cpu = (double) currentMetrics.getOrDefault("cpuUtilizationPercent", 0.0);
                double mem = (double) currentMetrics.getOrDefault("memoryUtilizationPercent", 0.0);

                // Check CPU Peaks
                if (cpu >= CPU_HIGH_PRIORITY_THRESHOLD) {
                    FirebaseManager.writePeakLog(SERVER_ID, "HIGH", "CPU", cpu, currentMetrics);
                } else if (cpu >= CPU_LOW_PRIORITY_THRESHOLD) {
                    FirebaseManager.writePeakLog(SERVER_ID, "LOW", "CPU", cpu, currentMetrics);
                } else if (cpu > 50.0 && cpu < CPU_LOW_PRIORITY_THRESHOLD) {
                    // Example of an ignorable warning log
                    FirebaseManager.writePeakLog(SERVER_ID, "IGNORABLE", "CPU", cpu, currentMetrics);
                }

                // Check Memory Peaks
                if (mem >= MEM_HIGH_PRIORITY_THRESHOLD) {
                    FirebaseManager.writePeakLog(SERVER_ID, "HIGH", "MEMORY", mem, currentMetrics);
                } else if (mem >= MEM_LOW_PRIORITY_THRESHOLD) {
                    FirebaseManager.writePeakLog(SERVER_ID, "LOW", "MEMORY", mem, currentMetrics);
                }

            } catch (Exception e) {
                System.err.println("[ERROR] Exception in agent loop: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }
}