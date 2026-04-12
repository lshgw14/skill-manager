package com.skillmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.base.Preconditions;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BatchOperations {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
            e.printStackTrace();
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
            if (Strings.isNullOrEmpty(operationType)) {
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
                case "ssh":
                    if (executeSshOperation(operation)) {
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
            String repoName = (String) repo.get("repoName");
            if (Strings.isNullOrEmpty(repoName)) {
                repoName = "Unknown";
            }
            writeLog(String.format("\nInitializing repo %d of %d: %s", repoCount, repos.size(), repoName));

            String repoUrl = (String) repo.get("repoUrl");
            String localPath = (String) repo.get("localPath");

            if (Strings.isNullOrEmpty(repoUrl)) {
                writeLog("ERROR: repoUrl not specified");
                failCount++;
                continue;
            }

            if (Strings.isNullOrEmpty(localPath)) {
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
                e.printStackTrace();
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

        if (Strings.isNullOrEmpty(targetPath)) {
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

        // 打印同步操作的仓库信息
        writeLog("\nSyncing repositories:");
        int repoIndex = 0;
        for (Map<String, Object> repo : repos) {
            repoIndex++;
            String repoName = (String) repo.get("repoName");
            if (Strings.isNullOrEmpty(repoName)) {
                repoName = "Unknown";
            }
            String repoPath = (String) repo.get("repoPath");
            writeLog(String.format("%d. %s - %s", repoIndex, repoName, repoPath));
        }

        // 写入临时配置文件
        Path tempConfigPath = Paths.get("temp-sync-config.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        try (BufferedWriter writer = Files.newBufferedWriter(tempConfigPath)) {
            objectMapper.writeValue(writer, syncConfig);
        } catch (Exception e) {
            writeLog(String.format("ERROR: Failed to create temp config file: %s", e.getMessage()));
            e.printStackTrace();
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
                    e.printStackTrace();
                }
                return true;
            } else {
                writeLog("ERROR: Failed to execute sync operation");
                // 删除临时配置文件
                try {
                    Files.deleteIfExists(tempConfigPath);
                } catch (Exception e) {
                    writeLog(String.format("WARNING: Failed to delete temp config file: %s", e.getMessage()));
                    e.printStackTrace();
                }
                return false;
            }
        } catch (Exception e) {
            writeLog(String.format("ERROR: Sync error: %s", e.getMessage()));
            e.printStackTrace();
            // 删除临时配置文件
            try {
                Files.deleteIfExists(tempConfigPath);
            } catch (Exception ex) {
                writeLog(String.format("WARNING: Failed to delete temp config file: %s", ex.getMessage()));
                ex.printStackTrace();
            }
            return false;
        }
    }

    private static boolean executeSshOperation(Map<String, Object> operation) {
        writeLog("Executing SSH operation...");

        String email = (String) operation.get("email");
        String action = (String) operation.get("action");
        String server = (String) operation.get("server");
        String configFile = (String) operation.get("configFile");
        Boolean gitConfig = (Boolean) operation.get("gitConfig");

        // 构建命令参数
        List<String> commandArgs = new ArrayList<>();
        commandArgs.add("java");
        commandArgs.add("-cp");
        commandArgs.add("target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar");
        commandArgs.add("com.skillmanager.InitGitSsh");

        // 添加GitConfig参数
        if (gitConfig != null) {
            commandArgs.add("-GitConfig");
            commandArgs.add(gitConfig.toString());
        }

        if (!Strings.isNullOrEmpty(configFile)) {
            commandArgs.add("-ConfigFile");
            commandArgs.add(configFile);
        } else {
            if (Strings.isNullOrEmpty(email)) {
                writeLog("ERROR: email not specified for SSH operation");
                return false;
            }
            commandArgs.add("-Email");
            commandArgs.add(email);

            if (!Strings.isNullOrEmpty(action)) {
                commandArgs.add("-Action");
                commandArgs.add(action);
            }

            if (!Strings.isNullOrEmpty(server)) {
                commandArgs.add("-Server");
                commandArgs.add(server);
            }
        }

        // 执行命令
        try {
            ProcessBuilder pb = new ProcessBuilder(commandArgs);
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writeLog(String.format("SSH output: %s", line));
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                writeLog("SSH operation completed successfully!");
                return true;
            } else {
                writeLog("ERROR: Failed to execute SSH operation");
                return false;
            }
        } catch (Exception e) {
            writeLog(String.format("ERROR: SSH operation error: %s", e.getMessage()));
            e.printStackTrace();
            return false;
        }
    }

    private static void writeLog(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        System.out.printf("[%s] %s%n", timestamp, message);
    }
}
