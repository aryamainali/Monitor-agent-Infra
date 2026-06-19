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

public class FirebaseManager {

    private static Firestore db;
    private static boolean isInitialized = false;

    public static void initializeFirebase() {
        try {
            // 1. Look for an environment variable first, otherwise fallback to local path safely hidden by .gitignore
            String configPath = System.getenv("FIREBASE_CONFIG_PATH");
            if (configPath == null || configPath.isEmpty()) {
                configPath = "src/main/resources/serviceAccountKey.json";
            }

            // Secure Service Account Auth token verification
            FileInputStream serviceAccount = new FileInputStream(configPath);

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
            isInitialized = true;
            System.out.println("[INFO] Authenticated via Service Account. Firestore Connection Live.");
        } catch (java.io.FileNotFoundException e) {
            System.err.println("[WARN] Firebase Auth paused. Configuration key file not found safely: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to initialize Firebase: " + e.getMessage());
        }
    }

    public static void sendMetricsToFirestore(String serverId, Map<String, Object> metrics) {
        if (!isInitialized) {
            System.out.println("[LOCAL-STORE] Firestore offline. Local metrics: " + metrics);
            return;
        }
        
        try {
            // Store under collection "telemetry_logs" with a sub-document per snapshot update
            String documentId = serverId + "_" + System.currentTimeMillis();
            ApiFuture<WriteResult> result = db.collection("telemetry_logs").document(documentId).set(metrics);
            
            // Asynchronously log verification confirmation 
            result.addListener(() -> {
                try {
                    System.out.println("[FIRESTORE-SYNC] Document written successfully at: " + result.get().getUpdateTime());
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to verify Firestore write: " + e.getMessage());
                }
            }, Executors.newSingleThreadExecutor());

        } catch (Exception e) {
            System.err.println("[ERROR] Error uploading document metrics to Firestore: " + e.getMessage());
        }
    }
}