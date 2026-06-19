package com.monitor;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentApplication {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // Assign a unique identification token for this specific host node instance
    private static final String SERVER_ID = "node-win-prod-01"; 

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("[INFO] Distributed Telemetry Agent Initialized.");
        System.out.println("[INFO] Running on Java Architecture Platform.");
        System.out.println("====================================================");

        // Attempt cloud service binding connection
        FirebaseManager.initializeFirebase();

        // 5-second background polling lifecycle loop execution
        scheduler.scheduleAtFixedRate(() -> {
            try {
                /// 1. Scrape the native OS subsystem layer counters
                Map<String, Object> currentMetrics = OSMetricsScanner.getSystemMetrics();
                
                // 2. Offload to Firestore Document Engine
                FirebaseManager.sendMetricsToFirestore(SERVER_ID, currentMetrics);
                
            } catch (Exception e) {
                System.err.println("[ERROR] Exception in telemetry execution loop: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}