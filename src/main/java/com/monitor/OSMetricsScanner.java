package com.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class OSMetricsScanner {

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("osName", System.getProperty("os.name"));
        metrics.put("timestamp", System.currentTimeMillis());

        try {
            if (OS.contains("aix")) {
                metrics.putAll(parseAIXMetrics());
            } else if (OS.contains("nux") || OS.contains("nix")) {
                metrics.putAll(parseLinuxMetrics());
            } else if (OS.contains("win")) {
                metrics.putAll(parseWindowsMetrics());
            } else {
                metrics.put("status", "Unsupported OS");
            }
        } catch (Exception e) {
            metrics.put("error", "Failed to harvest metrics: " + e.getMessage());
        }
        return metrics;
    }

    private static Map<String, Object> parseWindowsMetrics() throws Exception {
        Map<String, Object> winMetrics = new HashMap<>();
        // Querying Windows Management Instrumentation for Free Physical Memory
        Process process = new ProcessBuilder("cmd.exe", "/c", "wmic OS get FreePhysicalMemory,TotalVisibleMemorySize /value").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            long freeMem = 0;
            long totalMem = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("FreePhysicalMemory")) {
                    freeMem = Long.parseLong(line.split("=")[1].trim());
                } else if (line.startsWith("TotalVisibleMemorySize")) {
                    totalMem = Long.parseLong(line.split("=")[1].trim());
                }
            }
            if (totalMem > 0) {
                double memoryUtilization = ((double)(totalMem - freeMem) / totalMem) * 100;
                winMetrics.put("memoryUtilizationPercent", Math.round(memoryUtilization * 100.0) / 100.0);
            }
        }
        winMetrics.put("cpuUtilizationPercent", getWindowsCpuViaWmic());
        return winMetrics;
    }

    private static double getWindowsCpuViaWmic() throws Exception {
        Process process = new ProcessBuilder("cmd.exe", "/c", "wmic cpu get LoadPercentage /value").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("LoadPercentage")) {
                    return Double.parseDouble(line.split("=")[1].trim());
                }
            }
        }
        return 0.0;
    }

    private static Map<String, Object> parseLinuxMetrics() throws Exception {
        Map<String, Object> linuxMetrics = new HashMap<>();
        // In a standard Linux environment, we pull directly from /proc/stat and /proc/meminfo
        linuxMetrics.put("cpuUtilizationPercent", 15.4); // Mocked placeholder until /proc reader or top block is attached
        linuxMetrics.put("memoryUtilizationPercent", 42.1);
        return linuxMetrics;
    }

    private static Map<String, Object> parseAIXMetrics() throws Exception {
        Map<String, Object> aixMetrics = new HashMap<>();
        // For IBM Power Systems/AIX: parse output from lparstat & vmstat
        // Example execution: Process p = new ProcessBuilder("lparstat", "1", "1").start();
        aixMetrics.put("cpuUtilizationPercent", 22.8); // Hooked to native parsers next
        aixMetrics.put("memoryUtilizationPercent", 68.5);
        return aixMetrics;
    }
}