package com.skillmanager;

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
        if (Files.exists(jsonPath)) {
            writeLog(String.format("Reading existing JSON file: %s", jsonFile));
            try (BufferedReader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
                // 简单的 JSON 解析
                String jsonContent = reader.lines().collect(java.util.stream.Collectors.joining());
                // 这里使用简单的解析方式，实际项目中可以使用 JSON 库
                jsonConfig = parseJson(jsonContent);
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
                writer.write(toJson(jsonConfig));
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to save JSON file: %s", e.getMessage()));
                System.exit(1);
            }
            writeLog(String.format("JSON config saved to: %s", jsonFile));
            writeLog("Config content:");
            System.out.println(toJson(jsonConfig));
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

    public static List<Map<String, Object>> parseJson(String json) {
        // 简单的 JSON 解析，实际项目中可以使用 JSON 库
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 移除首尾的空白字符
            json = json.trim();
            if (json.startsWith("[")) {
                // 数组形式
                json = json.substring(1, json.length() - 1);
                String[] elements = splitJsonArray(json);
                for (String element : elements) {
                    if (!element.trim().isEmpty()) {
                        result.add(parseJsonObject(element));
                    }
                }
            } else if (json.startsWith("{")) {
                // 对象形式
                result.add(parseJsonObject(json));
            }
        } catch (Exception e) {
            writeLog(String.format("ERROR: Failed to parse JSON: %s", e.getMessage()));
        }
        return result;
    }

    public static Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> result = new HashMap<>();
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }

        json = json.substring(1, json.length() - 1);
        String[] pairs = splitJsonPairs(json);
        for (String pair : pairs) {
            if (pair.trim().isEmpty()) {
                continue;
            }
            int colonIndex = pair.indexOf(':');
            if (colonIndex == -1) {
                continue;
            }
            String key = pair.substring(0, colonIndex).trim().replace('"', ' ').trim();
            String valueStr = pair.substring(colonIndex + 1).trim();

            Object value;
            if (valueStr.startsWith("{")) {
                value = parseJsonObject(valueStr);
            } else if (valueStr.startsWith("[")) {
                value = parseJsonArray(valueStr);
            } else if (valueStr.startsWith("\"")) {
                value = valueStr.substring(1, valueStr.length() - 1);
            } else if (valueStr.equals("true")) {
                value = true;
            } else if (valueStr.equals("false")) {
                value = false;
            } else if (valueStr.equals("null")) {
                value = null;
            } else {
                try {
                    value = Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    value = valueStr;
                }
            }
            result.put(key, value);
        }
        return result;
    }

    public static List<Object> parseJsonArray(String json) {
        List<Object> result = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            return result;
        }

        json = json.substring(1, json.length() - 1);
        String[] elements = splitJsonArray(json);
        for (String element : elements) {
            if (element.trim().isEmpty()) {
                continue;
            }
            if (element.trim().startsWith("{")) {
                result.add(parseJsonObject(element));
            } else if (element.trim().startsWith("[")) {
                result.add(parseJsonArray(element));
            } else if (element.trim().startsWith("\"")) {
                result.add(element.trim().substring(1, element.trim().length() - 1));
            } else if (element.trim().equals("true")) {
                result.add(true);
            } else if (element.trim().equals("false")) {
                result.add(false);
            } else if (element.trim().equals("null")) {
                result.add(null);
            } else {
                try {
                    result.add(Double.parseDouble(element.trim()));
                } catch (NumberFormatException e) {
                    result.add(element.trim());
                }
            }
        }
        return result;
    }

    private static String[] splitJsonArray(String json) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : json.toCharArray()) {
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result.toArray(new String[0]);
    }

    private static String[] splitJsonPairs(String json) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : json.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                }
                if (c == ',' && depth == 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result.toArray(new String[0]);
    }

    private static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + ((String) obj).replace("\"", "\\\"") + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> list = (List<?>) obj;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(toJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) obj;
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append('"').append(entry.getKey()).append('"').append(": ");
                sb.append(toJson(entry.getValue()));
                i++;
            }
            sb.append("}");
            return sb.toString();
        }
        return obj.toString();
    }
}
