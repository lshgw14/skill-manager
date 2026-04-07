package com.skillmanager;

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

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-RepoUrl") && i + 1 < args.length) {
                repoUrl = args[i + 1];
            } else if (args[i].equals("-LocalPath") && i + 1 < args.length) {
                localPath = args[i + 1];
            }
        }

        // 验证必需参数
        if (repoUrl == null) {
            writeLog("ERROR: RepoUrl is required");
            writeLog("Usage: java -cp <classpath> com.skillmanager.InitSkillRepo -RepoUrl <repository-url> [-LocalPath <local-path>]");
            System.exit(1);
        }

        writeLog("Starting skill repo initialization...");
        writeLog(String.format("Repo URL: %s", repoUrl));
        writeLog(String.format("Local path: %s", localPath));

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

        writeLog("Initialization completed!");
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
