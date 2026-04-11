package com.skillmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.base.Strings;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.base.Splitter;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class InitSkillRepo {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        if (!Strings.isNullOrEmpty(configFile)) {
            writeLog(String.format("Using configuration file: %s", configFile));
            try {
                ArrayNode repos = readConfigFile(configFile);
                if (repos != null && repos.size() > 0) {
                    writeLog(String.format("Found %d repositories to initialize", repos.size()));
                    for (int i = 0; i < repos.size(); i++) {
                        ObjectNode repo = (ObjectNode) repos.get(i);
                        String repoName = repo.has("repoName") ? repo.get("repoName").asText() : "Unknown";
                        String repoUrlFromConfig = repo.get("repoUrl").asText();
                        String localPathFromConfig = repo.get("localPath").asText();
                        
                        Preconditions.checkNotNull(repoUrlFromConfig, "repoUrl is required in config file");
                        Preconditions.checkNotNull(localPathFromConfig, "localPath is required in config file");
                        
                        writeLog(String.format("\nInitializing repository %d/%d: %s", i + 1, repos.size(), repoName));
                        writeLog(String.format("Repo URL: %s", repoUrlFromConfig));
                        writeLog(String.format("Local path: %s", localPathFromConfig));
                        
                        initRepo(repoUrlFromConfig, localPathFromConfig, repoName);
                    }
                    writeLog("\nAll repositories initialized successfully!");
                    System.exit(0);
                } else {
                    writeLog("ERROR: No repositories found in configuration file");
                    System.exit(1);
                }
            } catch (Exception e) {
                writeLog(String.format("ERROR: Failed to read configuration file: %s", e.getMessage()));
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            // 验证必需参数
            if (Strings.isNullOrEmpty(repoUrl)) {
                writeLog("ERROR: RepoUrl is required");
                writeLog("Usage: java -cp <classpath> com.skillmanager.InitSkillRepo -RepoUrl <repository-url> [-LocalPath <local-path>]");
                writeLog("Or: java -cp <classpath> com.skillmanager.InitSkillRepo -ConfigFile <config-file>");
                System.exit(1);
            }

            // 从repoUrl中提取repoName
            String repoName = extractRepoName(repoUrl);
            writeLog(String.format("Repo URL: %s", repoUrl));
            writeLog(String.format("Repo Name: %s", repoName));
            writeLog(String.format("Local path: %s", localPath));
            initRepo(repoUrl, localPath, repoName);
            writeLog("Initialization completed!");
        }
    }

    private static ArrayNode readConfigFile(String configFile) throws Exception {
        Preconditions.checkNotNull(configFile, "configFile cannot be null");
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode config = (ObjectNode) objectMapper.readTree(new File(configFile));
        return (ArrayNode) config.get("repos");
    }

    private static String extractRepoName(String repoUrl) {
        Preconditions.checkNotNull(repoUrl, "repoUrl cannot be null");
        // 从repoUrl中提取repoName
        // 对于https://github.com/anthropics/skills.git，提取anthropics_skills
        try {
            // 移除.git后缀
            String urlWithoutGit = repoUrl.replaceAll("\\.git$", "");
            // 提取最后两个路径段
            List<String> parts = Lists.newArrayList(Splitter.on("/").split(urlWithoutGit));
            if (parts.size() >= 2) {
                String owner = parts.get(parts.size() - 2);
                String repo = parts.get(parts.size() - 1);
                return owner + "_" + repo;
            }
        } catch (Exception e) {
            // 如果提取失败，返回默认值
            writeLog(String.format("WARNING: Failed to extract repo name from URL: %s", e.getMessage()));
        }
        return "Unknown";
    }

    private static void initRepo(String repoUrl, String localPath, String repoName) {
        Preconditions.checkNotNull(repoUrl, "repoUrl cannot be null");
        Preconditions.checkNotNull(localPath, "localPath cannot be null");
        
        // 检查本地路径是否存在，不存在则创建
        Path localPathObj = Paths.get(localPath);
        if (!Files.exists(localPathObj)) {
            writeLog(String.format("Creating local directory: %s", localPath));
            try {
                Files.createDirectories(localPathObj);
                writeLog(String.format("Local directory created successfully: %s", localPath));
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to create local directory: %s", e.getMessage()));
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            writeLog(String.format("Local directory already exists: %s", localPath));
        }

        // 执行git clone命令
        writeLog(String.format("Cloning repository: %s...", repoName));
        writeLog(String.format("Repository URL: %s", repoUrl));
        writeLog(String.format("Local directory: %s", localPath));

        try (Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localPathObj.toFile())
                .setTimeout(300) // 设置300秒超时
                .call()) {
            
            writeLog("Repository cloned successfully!");
            // 验证克隆结果
            Path clonedDir = localPathObj;
            if (Files.exists(clonedDir) && Files.isDirectory(clonedDir)) {
                writeLog("Cloned repository contents:");
                listDirectory(clonedDir.toString());
            }
        } catch (GitAPIException e) {
            writeLog(String.format("ERROR: Git clone error: %s", e.getMessage()));
            writeLog("Possible causes:");
            writeLog("1. Network connection issues");
            writeLog("2. SSH authentication failure (check your SSH keys)");
            writeLog("3. Invalid repository URL");
            writeLog("4. Repository does not exist");
            writeLog("5. Firewall or proxy restrictions");
            writeLog("Detailed error stack trace:");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            writeLog(String.format("ERROR: Git clone error: %s", e.getMessage()));
            writeLog("Detailed error stack trace:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void writeLog(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        System.out.printf("[%s] %s%n", timestamp, message);
    }

    private static void listDirectory(String path) {
        Preconditions.checkNotNull(path, "path cannot be null");
        
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
            e.printStackTrace();
        }
    }
}
