package com.skillmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class CsvToJson {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // 解析命令行参数
        String csvFile = "skills.csv";
        String jsonFile = "sync-config.json";
        String targetPath = "C:\\Users\\admin\\.trae-cn\\skills\\";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-CsvFile") && i + 1 < args.length) {
                csvFile = args[i + 1];
            } else if (args[i].equals("-JsonFile") && i + 1 < args.length) {
                jsonFile = args[i + 1];
            } else if (args[i].equals("-TargetPath") && i + 1 < args.length) {
                targetPath = args[i + 1];
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
            if (headerLine == null) {
                writeLog("WARNING: CSV file is empty");
                System.exit(0);
            }

            String[] headers = headerLine.split(",");
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }
                csvData.add(row);
            }
        } catch (IOException e) {
            writeLog(String.format("ERROR: Failed to read CSV file: %s", e.getMessage()));
            System.exit(1);
        }

        if (csvData.isEmpty()) {
            writeLog("WARNING: CSV file is empty");
            System.exit(0);
        }

        writeLog(String.format("CSV file contains %d rows", csvData.size()));

        // 读取现有 JSON 文件
        List<Map<String, Object>> jsonConfig = new ArrayList<>();
        Path jsonPath = Paths.get(jsonFile);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        if (Files.exists(jsonPath)) {
            writeLog(String.format("Reading existing JSON file: %s", jsonFile));
            try (BufferedReader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
                // 使用 Jackson 解析 JSON
                jsonConfig = objectMapper.readValue(reader, List.class);
            } catch (Exception e) {
                writeLog("WARNING: Invalid JSON file, creating new configuration");
                jsonConfig = new ArrayList<>();
            }
        } else {
            writeLog("JSON file does not exist, creating new configuration");
        }

        // 查找或创建目标组
        Map<String, Object> targetGroup = null;
        for (Map<String, Object> group : jsonConfig) {
            if (targetPath.equals(group.get("targetPath"))) {
                targetGroup = group;
                break;
            }
        }

        if (targetGroup == null) {
            writeLog(String.format("Creating new target group: %s", targetPath));
            targetGroup = new HashMap<>();
            targetGroup.put("targetPath", targetPath);
            targetGroup.put("repos", new ArrayList<>());
            jsonConfig = new ArrayList<>();
            jsonConfig.add(targetGroup);
        }

        // 创建 repo map
        Map<String, List<String>> repoMap = new HashMap<>();
        List<Map<String, Object>> repos = (List<Map<String, Object>>) targetGroup.get("repos");
        if (repos != null) {
            for (Map<String, Object> repo : repos) {
                String repoPath = (String) repo.get("repoPath");
                List<String> skillNames = (List<String>) repo.get("skillNames");
                if (repoPath != null && skillNames != null) {
                    repoMap.put(repoPath, new ArrayList<>(skillNames));
                }
            }
        }

        boolean modified = false;

        // 处理 CSV 数据
        for (Map<String, String> row : csvData) {
            String repoPath = row.get("repoPath");
            String skillName = row.get("skillName");

            if (repoPath == null || repoPath.trim().isEmpty() || skillName == null || skillName.trim().isEmpty()) {
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
            } else {
                writeLog(String.format("Creating new repo config: %s, adding skill: %s", repoPath, skillName));
                List<String> skillNames = new ArrayList<>();
                skillNames.add(skillName);
                repoMap.put(repoPath, skillNames);
                modified = true;
            }
        }

        // 更新 repos
        List<Map<String, Object>> newRepos = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : repoMap.entrySet()) {
            Map<String, Object> repo = new HashMap<>();
            repo.put("repoPath", entry.getKey());
            repo.put("skillNames", entry.getValue());
            newRepos.add(repo);
        }

        targetGroup.put("repos", newRepos);

        // 保存 JSON
        if (modified) {
            try (BufferedWriter writer = Files.newBufferedWriter(jsonPath, StandardCharsets.UTF_8)) {
                objectMapper.writeValue(writer, jsonConfig);
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to save JSON file: %s", e.getMessage()));
                System.exit(1);
            }
            writeLog(String.format("JSON config saved to: %s", jsonFile));
            writeLog("Config content:");
            try {
                objectMapper.writeValue(System.out, jsonConfig);
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to write JSON to console: %s", e.getMessage()));
            }
        } else {
            writeLog("No updates needed");
        }

        writeLog("Processing completed");
    }

    private static void writeLog(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
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
            // 读取文件内容
            Charset charset = StandardCharsets.UTF_8;
            if (encoding.equals("Unicode")) {
                charset = Charset.forName("UTF-16LE");
            } else if (encoding.equals("BigEndianUnicode")) {
                charset = Charset.forName("UTF-16BE");
            }

            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            String content = new String(bytes, charset);

            // 写入 UTF-8 编码
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));

            writeLog("Conversion completed successfully");
            return true;
        } catch (Exception e) {
            writeLog(String.format("ERROR: Failed to convert encoding: %s", e.getMessage()));
            return false;
        }
    }
}
