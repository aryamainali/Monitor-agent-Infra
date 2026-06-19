package com.monitor;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import java.io.FileInputStream;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

public class FirebaseManager {

    private static Firestore db;
    private static boolean isInitialized = false;

    public static void initializeFirebase() {
        try {
            String configPath = System.getenv("FIREBASE_CONFIG_PATH");
            if (configPath == null || configPath.isEmpty()) {
                configPath = "src/main/resources/serviceAccountKey.json";
            }

            FileInputStream serviceAccount = new FileInputStream(configPath);
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
            isInitialized = true;
            System.out.println("[INFO] Authenticated via Service Account. Firestore Connection Live.");
        } catch (Exception e) {
            System.err.println("[WARN] Firebase Auth paused: " + e.getMessage());
        }
    }

    /**
     * Overwrites the single active state document for this specific server.
     * This keeps the dashboard accurate without bloating the database.
     */
    public static void updateActiveState(String serverId, Map<String, Object> metrics) {
        if (!isInitialized) {
            System.out.println("[LOCAL-STORE] Firestore offline. Local metrics: " + metrics);
            return;
        }

        try {
            // .document(serverId) ensures we overwrite the SAME document every 1 minute
            ApiFuture<WriteResult> result = db.collection("server_states").document(serverId).set(metrics);
            
            result.addListener(() -> {
                try {
                    System.out.println("[FIRESTORE-STATE] Active state refreshed at: " + result.get().getUpdateTime());
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to update active state: " + e.getMessage());
                }
            }, Executors.newSingleThreadExecutor());

        } catch (Exception e) {
            System.err.println("[ERROR] Error updating active state: " + e.getMessage());
        }
    }

    /**
     * Pushes a permanent incident log document when a server hits critical usage peaks.
     */
    public static void writePeakLog(String serverId, String alertPriority, String resourceType, double peakValue, Map<String, Object> details) {
        if (!isInitialized) return;

        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("serverId", serverId);
            logEntry.put("timestamp", System.currentTimeMillis());
            logEntry.put("priority", alertPriority); // HIGH, LOW, IGNORABLE
            logEntry.put("resource", resourceType);   // CPU or MEMORY
            logEntry.put("peakValuePercent", peakValue);
            logEntry.put("snapshotMetrics", details);
            logEntry.put("status", "UNRESOLVED");     // Can be marked "RESOLVED" or deleted via UI

            String logId = serverId + "_PEAK_" + System.currentTimeMillis();
            db.collection("peak_logs").document(logId).set(logEntry);
            
            System.out.println("[ALERT-TRIGGERED] Written " + alertPriority + " priority peak log to Firestore.");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to write peak log: " + e.getMessage());
        }
    }
}