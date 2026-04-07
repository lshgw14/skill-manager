package com.skillmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class BatchOperations {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // 解析命令行参数
        String configFile = "batch-config.json";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-ConfigFile") && i + 1 < args.length) {
                configFile = args[i + 1];
            }
        }

        writeLog("Starting batch operations...");
        writeLog(String.format("Config file: %s", configFile));

        // 检查配置文件是否存在
        Path configPath = Paths.get(configFile);
        if (!Files.exists(configPath)) {
            writeLog(String.format("ERROR: Config file does not exist: %s", configFile));
            System.exit(1);
        }

        // 解析配置文件
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> config = null;
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            config = objectMapper.readValue(reader, Map.class);
        } catch (Exception e) {
            writeLog(String.format("ERROR: Failed to parse config file: %s", e.getMessage()));
            System.exit(1);
        }

        // 提取批量操作列表
        List<Map<String, Object>> batchOperations = (List<Map<String, Object>>) config.get("batchOperations");
        if (batchOperations == null || batchOperations.isEmpty()) {
            writeLog("ERROR: No batch operations found in config file");
            System.exit(1);
        }

        // 执行批量操作
        int operationCount = 0;
        int successCount = 0;
        int failCount = 0;

        for (Map<String, Object> operation : batchOperations) {
            operationCount++;
            writeLog(String.format("\n=========================================="));
            writeLog(String.format("Executing operation %d of %d", operationCount, batchOperations.size()));
            writeLog(String.format("=========================================="));

            String operationType = (String) operation.get("type");
            if (operationType == null) {
                writeLog("ERROR: Operation type not specified");
                failCount++;
                continue;
            }

            switch (operationType.toLowerCase()) {
                case "init":
                    if (executeInitOperation(operation)) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                    break;
                case "sync":
                    if (executeSyncOperation(operation)) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                    break;
                default:
                    writeLog(String.format("ERROR: Invalid operation type: %s", operationType));
                    failCount++;
                    break;
            }
        }

        writeLog(String.format("\n=========================================="));
        writeLog(String.format("Batch operations completed"));
        writeLog(String.format("Total operations: %d", operationCount));
        writeLog(String.format("Successful: %d", successCount));
        writeLog(String.format("Failed: %d", failCount));
        writeLog(String.format("=========================================="));
    }

    private static boolean executeInitOperation(Map<String, Object> operation) {
        writeLog("Executing init operation...");

        List<Map<String, Object>> repos = (List<Map<String, Object>>) operation.get("repos");
        if (repos == null || repos.isEmpty()) {
            writeLog("ERROR: No repos specified for init operation");
            return false;
        }

        int repoCount = 0;
        int successCount = 0;
        int failCount = 0;

        for (Map<String, Object> repo : repos) {
            repoCount++;
            writeLog(String.format("\nInitializing repo %d of %d", repoCount, repos.size()));

            String repoUrl = (String) repo.get("repoUrl");
            String localPath = (String) repo.get("localPath");

            if (repoUrl == null) {
                writeLog("ERROR: repoUrl not specified");
                failCount++;
                continue;
            }

            if (localPath == null) {
                writeLog("ERROR: localPath not specified");
                failCount++;
                continue;
            }

            writeLog(String.format("Repo URL: %s", repoUrl));
            writeLog(String.format("Local path: %s", localPath));

            // 调用 InitSkillRepo 执行初始化
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", "target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar",
                    "com.skillmanager.InitSkillRepo",
                    "-RepoUrl", repoUrl,
                    "-LocalPath", localPath
                );
                pb.directory(new File("."));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // 读取输出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writeLog(String.format("Init output: %s", line));
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    writeLog("Repo initialized successfully!");
                    successCount++;
                } else {
                    writeLog("ERROR: Failed to initialize repo");
                    failCount++;
                }
            } catch (Exception e) {
                writeLog(String.format("ERROR: Init error: %s", e.getMessage()));
                failCount++;
            }
        }

        writeLog(String.format("Init operation completed: %d successful, %d failed", successCount, failCount));
        return failCount == 0;
    }

    private static boolean executeSyncOperation(Map<String, Object> operation) {
        writeLog("Executing sync operation...");

        String targetPath = (String) operation.get("targetPath");
        List<Map<String, Object>> repos = (List<Map<String, Object>>) operation.get("repos");

        if (targetPath == null) {
            writeLog("ERROR: targetPath not specified for sync operation");
            return false;
        }

        if (repos == null || repos.isEmpty()) {
            writeLog("ERROR: No repos specified for sync operation");
            return false;
        }

        // 为每个 repo 创建一个临时配置文件
        List<Map<String, Object>> syncConfig = new ArrayList<>();
        Map<String, Object> targetGroup = new HashMap<>();
        targetGroup.put("targetPath", targetPath);
        targetGroup.put("repos", repos);
        syncConfig.add(targetGroup);

        // 写入临时配置文件
        Path tempConfigPath = Paths.get("temp-sync-config.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        try (BufferedWriter writer = Files.newBufferedWriter(tempConfigPath)) {
            objectMapper.writeValue(writer, syncConfig);
        } catch (Exception e) {
            writeLog(String.format("ERROR: Failed to create temp config file: %s", e.getMessage()));
            return false;
        }

        // 调用 SyncSkills 执行同步
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar",
                "com.skillmanager.SyncSkills",
                "-ConfigFile", tempConfigPath.toString()
            );
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writeLog(String.format("Sync output: %s", line));
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                writeLog("Sync operation completed successfully!");
                // 删除临时配置文件
                try {
                    Files.deleteIfExists(tempConfigPath);
                } catch (Exception e) {
                    writeLog(String.format("WARNING: Failed to delete temp config file: %s", e.getMessage()));
                }
                return true;
            } else {
                writeLog("ERROR: Failed to execute sync operation");
                // 删除临时配置文件
                try {
                    Files.deleteIfExists(tempConfigPath);
                } catch (Exception e) {
                    writeLog(String.format("WARNING: Failed to delete temp config file: %s", e.getMessage()));
                }
                return false;
            }
        } catch (Exception e) {
            writeLog(String.format("ERROR: Sync error: %s", e.getMessage()));
            // 删除临时配置文件
            try {
                Files.deleteIfExists(tempConfigPath);
            } catch (Exception ex) {
                writeLog(String.format("WARNING: Failed to delete temp config file: %s", ex.getMessage()));
            }
            return false;
        }
    }

    private static void writeLog(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        System.out.printf("[%s] %s%n", timestamp, message);
    }
}
