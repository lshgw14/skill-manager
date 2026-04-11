package com.skillmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.base.Splitter;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CsvToJson {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Splitter CSV_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

    public static void main(String[] args) {
        // 解析命令行参数
        String csvFile = "skills.csv";
        String jsonFile = "sync-config.json";
        String targetPath = "C:\\Users\\admin\\.trae-cn\\skills\\";
        String outputType = "sync"; // 默认值为 sync

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-CsvFile") && i + 1 < args.length) {
                csvFile = args[i + 1];
            } else if (args[i].equals("-JsonFile") && i + 1 < args.length) {
                jsonFile = args[i + 1];
            } else if (args[i].equals("-TargetPath") && i + 1 < args.length) {
                targetPath = args[i + 1];
            } else if (args[i].equals("-OutputType") && i + 1 < args.length) {
                outputType = args[i + 1];
            }
        }

        // 解析完整路径
        Path csvPath = Paths.get(csvFile).toAbsolutePath();
        if (!Files.exists(csvPath)) {
            writeLog(String.format("ERROR: CSV file '%s' does not exist", csvPath));
            System.exit(1);
        }

        // 转换为 UTF-8 编码
        if (!convertToUtf8(csvPath.toString())) {
            writeLog("ERROR: Failed to ensure UTF-8 encoding for CSV file");
            System.exit(1);
        }

        writeLog(String.format("Reading CSV file: %s", csvPath));

        // 读取 CSV 文件
        List<Map<String, String>> csvData = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (Strings.isNullOrEmpty(headerLine)) {
                writeLog("WARNING: CSV file is empty");
                System.exit(0);
            }

            // 处理 UTF-8 BOM
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            List<String> headers = Lists.newArrayList(CSV_SPLITTER.split(headerLine));
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> values = Lists.newArrayList(CSV_SPLITTER.split(line));
                Map<String, String> row = Maps.newHashMap();
                for (int i = 0; i < Math.min(headers.size(), values.size()); i++) {
                    row.put(headers.get(i), values.get(i));
                }
                csvData.add(row);
            }
        } catch (IOException e) {
            writeLog(String.format("ERROR: Failed to read CSV file: %s", e.getMessage()));
            e.printStackTrace();
            System.exit(1);
        }

        if (csvData.isEmpty()) {
            writeLog("WARNING: CSV file is empty");
            System.exit(0);
        }

        writeLog(String.format("CSV file contains %d rows", csvData.size()));

        Path jsonPath = Paths.get(jsonFile);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        if ("init".equals(outputType)) {
            // 处理 init 模式：生成 init-config.json
            writeLog("Processing in init mode: generating init-config.json");
            
            // 从 CSV 数据中提取唯一的仓库信息
            Map<String, Map<String, String>> repoMap = Maps.newHashMap();
            for (Map<String, String> row : csvData) {
                String repoPath = row.get("repoPath");
                String repoName = row.get("repoName");
                String localPath = row.get("localPath");
                String repoUrl = row.get("repoUrl");
                
                if (Strings.isNullOrEmpty(repoPath)) {
                    writeLog("Skipping empty repoPath");
                    continue;
                }
                
                repoPath = repoPath.trim();
                if (!repoMap.containsKey(repoPath)) {
                    Map<String, String> repoInfo = Maps.newHashMap();
                    if (!Strings.isNullOrEmpty(repoName)) {
                        repoInfo.put("repoName", repoName.trim());
                    }
                    if (!Strings.isNullOrEmpty(localPath)) {
                        repoInfo.put("localPath", localPath.trim());
                    }
                    if (!Strings.isNullOrEmpty(repoUrl)) {
                        repoInfo.put("repoUrl", repoUrl.trim());
                    }
                    repoMap.put(repoPath, repoInfo);
                }
            }
            
            // 生成 init-config.json 格式
            Map<String, Object> initConfig = Maps.newHashMap();
            List<Map<String, Object>> repos = Lists.newArrayList();
            
            for (Map.Entry<String, Map<String, String>> entry : repoMap.entrySet()) {
                String repoPath = entry.getKey();
                Map<String, String> repoInfo = entry.getValue();
                
                String repoUrl = repoInfo.get("repoUrl");
                
                // 如果 CSV 中没有提供 repoUrl，则从 repoPath 生成
                if (Strings.isNullOrEmpty(repoUrl)) {
                    // 从 repoPath 提取 GitHub 仓库信息
                    List<String> parts = Lists.newArrayList(Splitter.on("\\\\").split(repoPath));
                    repoUrl = "";
                    if (parts.size() >= 2) {
                        String owner = parts.size() >= 3 ? parts.get(parts.size() - 3) : "";
                        String repo = parts.get(parts.size() - 2);
                        repoUrl = String.format("https://github.com/%s/%s.git", owner, repo);
                    }
                }
                
                Map<String, Object> repoConfig = Maps.newHashMap();
                repoConfig.put("repoName", repoInfo.get("repoName"));
                repoConfig.put("repoUrl", repoUrl);
                repoConfig.put("localPath", repoInfo.get("localPath"));
                repos.add(repoConfig);
            }
            
            initConfig.put("repos", repos);
            
            // 保存到文件
            try (BufferedWriter writer = Files.newBufferedWriter(jsonPath, StandardCharsets.UTF_8)) {
                objectMapper.writeValue(writer, initConfig);
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to save JSON file: %s", e.getMessage()));
                e.printStackTrace();
                System.exit(1);
            }
            writeLog(String.format("Init config saved to: %s", jsonFile));
            writeLog("Config content:");
            try {
                objectMapper.writeValue(System.out, initConfig);
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to write JSON to console: %s", e.getMessage()));
                e.printStackTrace();
            }
        } else {
            // 处理 sync 模式：生成 sync-config.json
            writeLog("Processing in sync mode: generating sync-config.json");
            
            // 读取现有 JSON 文件
            Map<String, Object> jsonConfig = Maps.newHashMap();
            
            if (Files.exists(jsonPath)) {
                writeLog(String.format("Reading existing JSON file: %s", jsonFile));
                try (BufferedReader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
                    // 使用 Jackson 解析 JSON
                    jsonConfig = objectMapper.readValue(reader, Map.class);
                } catch (Exception e) {
                    writeLog("WARNING: Invalid JSON file, creating new configuration");
                    jsonConfig = Maps.newHashMap();
                }
            } else {
                writeLog("JSON file does not exist, creating new configuration");
            }

            // 查找或创建目标组
            Map<String, Object> targetGroup = null;
            if (targetPath.equals(jsonConfig.get("targetPath"))) {
                targetGroup = jsonConfig;
            }

            if (targetGroup == null) {
                writeLog(String.format("Creating new target group: %s", targetPath));
                targetGroup = Maps.newHashMap();
                targetGroup.put("targetPath", targetPath);
                targetGroup.put("repos", Lists.newArrayList());
                jsonConfig = targetGroup;
            }

            // 创建 repo map
            Map<String, List<String>> repoMap = Maps.newHashMap();
            Map<String, String> repoNameMap = Maps.newHashMap();
            List<Map<String, Object>> repos = (List<Map<String, Object>>) targetGroup.get("repos");
            if (repos != null) {
                for (Map<String, Object> repo : repos) {
                    String repoPath = (String) repo.get("repoPath");
                    List<String> skillNames = (List<String>) repo.get("skillNames");
                    String repoName = (String) repo.get("repoName");
                    if (!Strings.isNullOrEmpty(repoPath) && skillNames != null) {
                        repoMap.put(repoPath, new ArrayList<>(skillNames));
                        if (!Strings.isNullOrEmpty(repoName)) {
                            repoNameMap.put(repoPath, repoName);
                        }
                    }
                }
            }

            boolean modified = false;

            // 处理 CSV 数据
            for (Map<String, String> row : csvData) {
                String repoPath = row.get("repoPath");
                String skillName = row.get("skillName");
                String repoName = row.get("repoName");

                if (Strings.isNullOrEmpty(repoPath) || Strings.isNullOrEmpty(skillName)) {
                    writeLog(String.format("Skipping empty row: repoPath=%s, skillName=%s", repoPath, skillName));
                    continue;
                }

                repoPath = repoPath.trim();
                skillName = skillName.trim();

                if (repoMap.containsKey(repoPath)) {
                    List<String> skillNames = repoMap.get(repoPath);
                    if (!skillNames.contains(skillName)) {
                        writeLog(String.format("Adding skill '%s' to existing repo: %s", skillName, repoPath));
                        skillNames.add(skillName);
                        modified = true;
                    } else {
                        writeLog(String.format("Skill '%s' already exists in repo: %s", skillName, repoPath));
                    }
                    
                    // Keep repoName unchanged from existing config
                    // Do not update repoName from CSV
                } else {
                    writeLog(String.format("Creating new repo config: %s, adding skill: %s", repoPath, skillName));
                    List<String> skillNames = Lists.newArrayList();
                    skillNames.add(skillName);
                    repoMap.put(repoPath, skillNames);
                    if (!Strings.isNullOrEmpty(repoName)) {
                        repoNameMap.put(repoPath, repoName.trim());
                    }
                    modified = true;
                }
            }

            // 更新 repos
            List<Map<String, Object>> newRepos = Lists.newArrayList();
            for (Map.Entry<String, List<String>> entry : repoMap.entrySet()) {
                Map<String, Object> repo = Maps.newHashMap();
                repo.put("repoPath", entry.getKey());
                repo.put("skillNames", entry.getValue());
                if (repoNameMap.containsKey(entry.getKey())) {
                    repo.put("repoName", repoNameMap.get(entry.getKey()));
                }
                newRepos.add(repo);
            }

            targetGroup.put("repos", newRepos);

            // 保存 JSON
            if (modified) {
                try (BufferedWriter writer = Files.newBufferedWriter(jsonPath, StandardCharsets.UTF_8)) {
                    objectMapper.writeValue(writer, jsonConfig);
                } catch (IOException e) {
                    writeLog(String.format("ERROR: Failed to save JSON file: %s", e.getMessage()));
                    e.printStackTrace();
                    System.exit(1);
                }
                writeLog(String.format("JSON config saved to: %s", jsonFile));
                writeLog("Config content:");
                try {
                    objectMapper.writeValue(System.out, jsonConfig);
                } catch (IOException e) {
                    writeLog(String.format("ERROR: Failed to write JSON to console: %s", e.getMessage()));
                    e.printStackTrace();
                }
            } else {
                writeLog("No updates needed");
            }
        }

        writeLog("Processing completed");
    }

    private static void writeLog(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        System.out.printf("[%s] %s%n", timestamp, message);
    }

    private static String getFileEncoding(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] bytes = new byte[4];
            int read = fis.read(bytes);

            if (read >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return "UTF8";
            }
            if (read >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                return "Unicode";
            }
            if (read >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                return "BigEndianUnicode";
            }
            return "Default";
        } catch (Exception e) {
            return "Locked";
        }
    }

    private static boolean convertToUtf8(String filePath) {
        String encoding = getFileEncoding(filePath);
        writeLog(String.format("Detected encoding: %s", encoding));

        if (encoding.equals("UTF8")) {
            writeLog("File is already UTF-8, no conversion needed");
            return true;
        }

        if (encoding.equals("Locked")) {
            writeLog("WARNING: File is locked by another process, skipping encoding conversion");
            writeLog("Please close the file in other applications and try again if encoding issues occur");
            return true;
        }

        writeLog(String.format("Converting file from %s to UTF-8...", encoding));

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            String content = null;
            Charset usedCharset = null;

            if (encoding.equals("Default")) {
                // 尝试使用常见编码进行解码
                String[] encodingsToTry = {"UTF-8", "GBK", "GB2312", "ISO-8859-1"};
                for (String encName : encodingsToTry) {
                    try {
                        Charset charset = Charset.forName(encName);
                        content = new String(bytes, charset);
                        // 简单验证内容是否有效
                        if (content != null && (content.matches(".*[a-zA-Z0-9].*") || content.length() > 0)) {
                            usedCharset = charset;
                            writeLog(String.format("Successfully decoded with encoding: %s", encName));
                            break;
                        }
                    } catch (Exception e) {
                        // 忽略解码错误，尝试下一个编码
                    }
                }

                if (content == null) {
                    // 如果所有编码都失败，使用默认编码
                    content = new String(bytes, Charset.defaultCharset());
                    usedCharset = Charset.defaultCharset();
                    writeLog(String.format("Using default encoding as fallback: %s", usedCharset.name()));
                }
            } else {
                // 使用检测到的编码
                Charset charset = StandardCharsets.UTF_8;
                if (encoding.equals("Unicode")) {
                    charset = Charset.forName("UTF-16LE");
                } else if (encoding.equals("BigEndianUnicode")) {
                    charset = Charset.forName("UTF-16BE");
                }
                content = new String(bytes, charset);
                usedCharset = charset;
            }

            // 写入 UTF-8 编码
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));

            writeLog("Conversion completed successfully");
            return true;
        } catch (Exception e) {
            writeLog(String.format("ERROR: Failed to convert encoding: %s", e.getMessage()));
            e.printStackTrace();
            return false;
        }
    }
}
