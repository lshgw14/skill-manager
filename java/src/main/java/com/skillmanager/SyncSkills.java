package com.skillmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SyncSkills {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // 解析命令行参数
        String skillName = null;
        String repoPath = "E:\\develop\\code\\open-source\\github\\skills\\mattpocock\\skills";
        String targetPath = "C:\\Users\\admin\\.trae-cn\\skills\\";
        String configFile = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-SkillName") && i + 1 < args.length) {
                skillName = args[i + 1];
            } else if (args[i].equals("-RepoPath") && i + 1 < args.length) {
                repoPath = args[i + 1];
            } else if (args[i].equals("-TargetPath") && i + 1 < args.length) {
                targetPath = args[i + 1];
            } else if (args[i].equals("-ConfigFile") && i + 1 < args.length) {
                configFile = args[i + 1];
            }
        }

        writeLog("Starting skill sync...");

        // 如果指定了配置文件，从配置文件读取同步任务
        if (!Strings.isNullOrEmpty(configFile)) {
            // 检查配置文件是否存在
            Path configPath = Paths.get(configFile);
            if (!Files.exists(configPath)) {
                writeLog(String.format("ERROR: Config file does not exist: %s", configFile));
                System.exit(1);
            }

            writeLog(String.format("Reading config file: %s", configFile));

            // 读取和解析 JSON 配置文件
            List<Map<String, Object>> configs = Lists.newArrayList();
            ObjectMapper objectMapper = new ObjectMapper();
            
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                // 使用 Jackson 解析 JSON
                configs = objectMapper.readValue(reader, List.class);
            } catch (Exception e) {
                writeLog(String.format("ERROR: Invalid JSON config file: %s", e.getMessage()));
                e.printStackTrace();
                System.exit(1);
            }

            // 初始化计数器
            int successCount = 0;
            int failCount = 0;

            // 遍历配置项并执行同步
            List<Map<String, Object>> configList;
            if (configs.isEmpty()) {
                // 处理单个对象的情况
                configList = Lists.newArrayList();
                try {
                    Map<String, Object> singleConfig = objectMapper.readValue(Files.readString(configPath), Map.class);
                    configList.add(singleConfig);
                } catch (IOException e) {
                    writeLog(String.format("ERROR: Failed to read config file: %s", e.getMessage()));
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                configList = configs;
            }

            for (Map<String, Object> targetGroup : configList) {
                String groupTargetPath = (String) targetGroup.get("targetPath");
                if (Strings.isNullOrEmpty(groupTargetPath)) {
                    continue;
                }

                List<Map<String, Object>> repos = (List<Map<String, Object>>) targetGroup.get("repos");
                if (repos == null) {
                    continue;
                }

                for (Map<String, Object> repo : repos) {
                    String repoName = (String) repo.get("repoName");
                        if (Strings.isNullOrEmpty(repoName)) {
                            repoName = "Unknown";
                        }
                        String repoPathFromConfig = (String) repo.get("repoPath");
                        Object skillNamesObj = repo.get("skillNames");

                        if (Strings.isNullOrEmpty(repoPathFromConfig) || skillNamesObj == null) {
                            continue;
                        }

                    List<String> skillNames;
                    if (skillNamesObj instanceof String) {
                        skillNames = Collections.singletonList((String) skillNamesObj);
                    } else if (skillNamesObj instanceof List) {
                        skillNames = Lists.newArrayList();
                        for (Object obj : (List<?>) skillNamesObj) {
                            if (obj instanceof String) {
                                skillNames.add((String) obj);
                            }
                        }
                    } else {
                        continue;
                    }

                    for (String skill : skillNames) {
                            writeLog(String.format("Processing config: Source=%s (Repo: %s), Skill=%s, Target=%s", repoPathFromConfig, repoName, skill, groupTargetPath));

                            boolean result = syncSkill(skill, repoPathFromConfig, groupTargetPath);

                        if (result) {
                            successCount++;
                        } else {
                            failCount++;
                        }
                    }
                }
            }

            writeLog(String.format("Config file processing completed. Success: %d, Failed: %d", successCount, failCount));
        } else {
            // 没有指定配置文件，使用命令行参数执行同步
            syncSkill(skillName, repoPath, targetPath);
        }

        writeLog("Sync completed!");
    }

    private static void writeLog(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        System.out.printf("[%s] %s%n", timestamp, message);
    }

    private static void writeDirectoryTree(String path, String title) {
        Preconditions.checkNotNull(path, "path cannot be null");
        Preconditions.checkNotNull(title, "title cannot be null");
        
        writeLog(String.format("========== %s ==========", title));

        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            writeLog(String.format("Directory does not exist: %s", path));
            writeLog("==========================================");
            return;
        }

        writeLog(String.format("Directory path: %s", path));
        writeLog("Directory contents:");

        List<Path> items = Lists.newArrayList();
        try {
            Files.walk(dirPath)
                 .filter(Files::isRegularFile)
                 .forEach(items::add);
            Files.walk(dirPath)
                 .filter(Files::isDirectory)
                 .forEach(items::add);
        } catch (IOException e) {
            writeLog(String.format("ERROR: Failed to walk directory: %s", e.getMessage()));
            e.printStackTrace();
            writeLog("==========================================");
            return;
        }

        int fileCount = 0;
        int dirCount = 0;

        for (Path itemPath : items) {
            if (itemPath.equals(dirPath)) {
                continue;
            }
            Path relativePath = dirPath.relativize(itemPath);
            if (Files.isDirectory(itemPath)) {
                System.out.printf("  [DIR] %s%n", relativePath);
                dirCount++;
            } else {
                try {
                    long size = Files.size(itemPath);
                    double sizeKb = size / 1024.0;
                    System.out.printf("  [FILE] %s (%.2f KB)%n", relativePath, sizeKb);
                    fileCount++;
                } catch (IOException e) {
                    System.out.printf("  [FILE] %s (unknown size)%n", relativePath);
                    fileCount++;
                }
            }
        }

        writeLog(String.format("Stats: %d directories, %d files", dirCount, fileCount));
        writeLog("==========================================");
    }

    private static boolean syncSkill(String skillName, String repoPath, String targetPath) {
        Preconditions.checkNotNull(repoPath, "repoPath cannot be null");
        Preconditions.checkNotNull(targetPath, "targetPath cannot be null");
        
        // 检查源仓库路径是否存在
        Path repoPathObj = Paths.get(repoPath);
        if (!Files.exists(repoPathObj)) {
            writeLog(String.format("ERROR: Source repo path does not exist: %s", repoPath));
            return false;
        }

        // 更新源仓库（git pull）
        writeLog(String.format("Updating source repo: %s", repoPath));
        try (Git git = Git.open(repoPathObj.toFile())) {
            git.pull().call();
        } catch (GitAPIException e) {
            writeLog("WARNING: git pull failed, continuing with existing code...");
            writeLog(String.format("Git error: %s", e.getMessage()));
        } catch (Exception e) {
            writeLog(String.format("WARNING: git pull error: %s", e.getMessage()));
            e.printStackTrace();
        }

        // 检查并创建目标目录
        Path targetPathObj = Paths.get(targetPath);
        if (!Files.exists(targetPathObj)) {
            writeLog(String.format("Creating target directory: %s", targetPath));
            try {
                Files.createDirectories(targetPathObj);
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to create target directory: %s", e.getMessage()));
                e.printStackTrace();
                return false;
            }
        }

        // 同步单个技能
        if (!Strings.isNullOrEmpty(skillName)) {
            Path sourceSkillPath = repoPathObj.resolve(skillName);

            // 检查指定的技能是否存在
            if (!Files.exists(sourceSkillPath)) {
                writeLog(String.format("ERROR: Specified skill does not exist: %s", sourceSkillPath));
                return false;
            }

            Path destSkillPath = targetPathObj.resolve(skillName);

            // 复制前打印源目录（验证）
            writeDirectoryTree(sourceSkillPath.toString(), "BEFORE COPY - Source directory");

            // 执行复制操作
            writeLog(String.format("Copying skill: %s", skillName));
            writeLog(String.format("Source path: %s", sourceSkillPath));
            writeLog(String.format("Target path: %s", destSkillPath));

            // 使用类似 rsync 的方法进行跨平台支持
            writeLog("---------- sync output start ----------");
            try {
                // 创建目标目录（如果不存在）
                if (!Files.exists(destSkillPath)) {
                    Files.createDirectories(destSkillPath);
                }

                // 遍历源目录
                Files.walk(sourceSkillPath)
                     .filter(Files::isRegularFile)
                     .forEach(sourceFile -> {
                         try {
                             Path relativePath = sourceSkillPath.relativize(sourceFile);
                             Path targetFile = destSkillPath.resolve(relativePath);

                             // 创建目标目录（如果不存在）
                             Files.createDirectories(targetFile.getParent());

                             // 如果文件不存在或源文件更新时间较新，则复制
                             if (!Files.exists(targetFile) || Files.getLastModifiedTime(sourceFile).compareTo(Files.getLastModifiedTime(targetFile)) > 0) {
                                 Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                                 System.out.printf("Copied: %s%n", relativePath);
                             }
                         } catch (IOException e) {
                             writeLog(String.format("ERROR: Failed to copy file: %s", e.getMessage()));
                             e.printStackTrace();
                         }
                     });
            } catch (Exception e) {
                writeLog(String.format("ERROR: Copy failed: %s", e.getMessage()));
                e.printStackTrace();
                return false;
            }

            writeLog("---------- sync output end ----------");

            // 复制后打印目标目录（验证）
            writeDirectoryTree(destSkillPath.toString(), "AFTER COPY - Target directory");

            writeLog(String.format("Successfully synced skill: %s", skillName));
        } else {
            // 同步所有技能
            writeLog("Syncing all skills...");

            // 从源仓库获取所有技能目录
            List<String> skills = Lists.newArrayList();
            try {
                Files.list(repoPathObj)
                     .filter(Files::isDirectory)
                     .forEach(dir -> skills.add(dir.getFileName().toString()));
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to list skills: %s", e.getMessage()));
                e.printStackTrace();
                return false;
            }

            // 复制前打印源目录概览
            writeDirectoryTree(repoPath, "BEFORE COPY - Source repo overview");

            for (String skill : skills) {
                Path sourceSkillPath = repoPathObj.resolve(skill);
                Path destSkillPath = targetPathObj.resolve(skill);

                // 打印单个技能源目录
                writeDirectoryTree(sourceSkillPath.toString(), String.format("BEFORE COPY - Skill [%s] source", skill));

                // 执行复制操作
                writeLog(String.format("Copying skill: %s", skill));
                writeLog(String.format("Source path: %s", sourceSkillPath));
                writeLog(String.format("Target path: %s", destSkillPath));

                writeLog("---------- sync output start ----------");
                try {
                    // 创建目标目录（如果不存在）
                    if (!Files.exists(destSkillPath)) {
                        Files.createDirectories(destSkillPath);
                    }

                    // 遍历源目录
                    Files.walk(sourceSkillPath)
                         .filter(Files::isRegularFile)
                         .forEach(sourceFile -> {
                             try {
                                 Path relativePath = sourceSkillPath.relativize(sourceFile);
                                 Path targetFile = destSkillPath.resolve(relativePath);

                                 // 创建目标目录（如果不存在）
                                 Files.createDirectories(targetFile.getParent());

                                 // 如果文件不存在或源文件更新时间较新，则复制
                                 if (!Files.exists(targetFile) || Files.getLastModifiedTime(sourceFile).compareTo(Files.getLastModifiedTime(targetFile)) > 0) {
                                     Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                                     System.out.printf("Copied: %s%n", relativePath);
                                 }
                             } catch (IOException e) {
                                 writeLog(String.format("ERROR: Failed to copy file: %s", e.getMessage()));
                                 e.printStackTrace();
                             }
                         });
                } catch (Exception e) {
                    writeLog(String.format("ERROR: Copy failed: %s", e.getMessage()));
                    e.printStackTrace();
                    continue;
                }

                writeLog("---------- sync output end ----------");

                // 复制后打印目标目录
                writeDirectoryTree(destSkillPath.toString(), String.format("AFTER COPY - Skill [%s] target", skill));
            }

            // 复制后打印目标目录概览
            writeDirectoryTree(targetPath, "AFTER COPY - Target directory overview");

            writeLog(String.format("Successfully synced all skills, total: %d", skills.size()));
        }

        return true;
    }
}
