package com.skillmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class InitSkillRepo {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // 解析命令行参数
        String repoUrl = null;
        String localPath = System.getProperty("user.dir");
        String configFile = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-RepoUrl") && i + 1 < args.length) {
                repoUrl = args[i + 1];
            } else if (args[i].equals("-LocalPath") && i + 1 < args.length) {
                localPath = args[i + 1];
            } else if (args[i].equals("-ConfigFile") && i + 1 < args.length) {
                configFile = args[i + 1];
            }
        }

        writeLog("Starting skill repo initialization...");

        // 处理配置文件
        if (configFile != null) {
            writeLog(String.format("Using configuration file: %s", configFile));
            try {
                ArrayNode repos = readConfigFile(configFile);
                if (repos != null && repos.size() > 0) {
                    writeLog(String.format("Found %d repositories to initialize", repos.size()));
                    for (int i = 0; i < repos.size(); i++) {
                        ObjectNode repo = (ObjectNode) repos.get(i);
                        String repoUrlFromConfig = repo.get("repoUrl").asText();
                        String localPathFromConfig = repo.get("localPath").asText();
                        
                        writeLog(String.format("\nInitializing repository %d/%d:", i + 1, repos.size()));
                        writeLog(String.format("Repo URL: %s", repoUrlFromConfig));
                        writeLog(String.format("Local path: %s", localPathFromConfig));
                        
                        initRepo(repoUrlFromConfig, localPathFromConfig);
                    }
                    writeLog("\nAll repositories initialized successfully!");
                    System.exit(0);
                } else {
                    writeLog("ERROR: No repositories found in configuration file");
                    System.exit(1);
                }
            } catch (Exception e) {
                writeLog(String.format("ERROR: Failed to read configuration file: %s", e.getMessage()));
                System.exit(1);
            }
        } else {
            // 验证必需参数
            if (repoUrl == null) {
                writeLog("ERROR: RepoUrl is required");
                writeLog("Usage: java -cp <classpath> com.skillmanager.InitSkillRepo -RepoUrl <repository-url> [-LocalPath <local-path>]");
                writeLog("Or: java -cp <classpath> com.skillmanager.InitSkillRepo -ConfigFile <config-file>");
                System.exit(1);
            }

            writeLog(String.format("Repo URL: %s", repoUrl));
            writeLog(String.format("Local path: %s", localPath));
            initRepo(repoUrl, localPath);
            writeLog("Initialization completed!");
        }
    }

    private static ArrayNode readConfigFile(String configFile) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode config = (ObjectNode) objectMapper.readTree(new File(configFile));
        return (ArrayNode) config.get("repos");
    }

    private static void initRepo(String repoUrl, String localPath) {
        // 检查本地路径是否存在，不存在则创建
        Path localPathObj = Paths.get(localPath);
        if (!Files.exists(localPathObj)) {
            writeLog(String.format("Creating local directory: %s", localPath));
            try {
                Files.createDirectories(localPathObj);
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to create local directory: %s", e.getMessage()));
                System.exit(1);
            }
        }

        // 执行git clone命令
        writeLog("Cloning repository...");
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "clone", repoUrl, localPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writeLog(String.format("Git output: %s", line));
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                writeLog("Repository cloned successfully!");
                // 验证克隆结果
                Path clonedDir = localPathObj;
                if (Files.exists(clonedDir) && Files.isDirectory(clonedDir)) {
                    writeLog("Cloned repository contents:");
                    listDirectory(clonedDir.toString());
                }
            } else {
                writeLog("ERROR: Failed to clone repository");
                System.exit(1);
            }
        } catch (Exception e) {
            writeLog(String.format("ERROR: Git clone error: %s", e.getMessage()));
            System.exit(1);
        }
    }

    private static void writeLog(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        System.out.printf("[%s] %s%n", timestamp, message);
    }

    private static void listDirectory(String path) {
        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            writeLog(String.format("Directory does not exist: %s", path));
            return;
        }

        try {
            Files.walk(dirPath, 2)  // 只列出两级目录
                 .filter(Files::isRegularFile)
                 .forEach(file -> {
                     Path relativePath = dirPath.relativize(file);
                     System.out.printf("  [FILE] %s%n", relativePath);
                 });
            Files.walk(dirPath, 2)  // 只列出两级目录
                 .filter(Files::isDirectory)
                 .filter(dir -> !dir.equals(dirPath))
                 .forEach(dir -> {
                     Path relativePath = dirPath.relativize(dir);
                     System.out.printf("  [DIR] %s%n", relativePath);
                 });
        } catch (IOException e) {
            writeLog(String.format("ERROR: Failed to list directory: %s", e.getMessage()));
        }
    }
}
