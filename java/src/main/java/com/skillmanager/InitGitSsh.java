package com.skillmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.base.Strings;
import com.google.common.base.Preconditions;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Iterator;

public class InitGitSsh {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SSH_CONFIG_PATH = System.getProperty("user.home") + "/.ssh/config";
    private static final String SSH_DIR = System.getProperty("user.home") + "/.ssh";

    public static void main(String[] args) {
        // 解析命令行参数
        String action = "all";
        String email = null;
        String server = "all";
        String configFile = null;
        boolean gitConfig = true; // 默认配置git

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-Action") && i + 1 < args.length) {
                action = args[i + 1];
            } else if (args[i].equals("-Email") && i + 1 < args.length) {
                email = args[i + 1];
            } else if (args[i].equals("-Server") && i + 1 < args.length) {
                server = args[i + 1];
            } else if (args[i].equals("-ConfigFile") && i + 1 < args.length) {
                configFile = args[i + 1];
            } else if (args[i].equals("-GitConfig") && i + 1 < args.length) {
                gitConfig = Boolean.parseBoolean(args[i + 1]);
            }
        }

        writeLog("Starting Git SSH initialization...");

        // 处理配置文件
        if (!Strings.isNullOrEmpty(configFile)) {
            writeLog(String.format("Using configuration file: %s", configFile));
            try {
                Map<String, Object> config = null;
                
                // 根据文件扩展名判断配置文件类型
                if (configFile.toLowerCase().endsWith(".yml") || configFile.toLowerCase().endsWith(".yaml")) {
                    writeLog("Reading YAML configuration file...");
                    config = readYmlConfigFile(configFile);
                } else {
                    writeLog("Reading JSON configuration file...");
                    ObjectNode jsonConfig = readConfigFile(configFile);
                    if (jsonConfig != null) {
                        // 转换JSON到Map
                        config = new HashMap<>();
                        if (jsonConfig.has("email")) {
                            config.put("email", jsonConfig.get("email").asText());
                        }
                        if (jsonConfig.has("git")) {
                            ObjectNode gitNode = (ObjectNode) jsonConfig.get("git");
                            Map<String, Object> gitMap = new HashMap<>();
                            if (gitNode.has("global")) {
                                ObjectNode globalNode = (ObjectNode) gitNode.get("global");
                                Map<String, String> globalMap = new HashMap<>();
                                Iterator<String> globalFields = globalNode.fieldNames();
                                while (globalFields.hasNext()) {
                                    String field = globalFields.next();
                                    globalMap.put(field, globalNode.get(field).asText());
                                }
                                gitMap.put("global", globalMap);
                            }
                            if (gitNode.has("repos")) {
                                ArrayNode reposNode = (ArrayNode) gitNode.get("repos");
                                List<Map<String, Object>> reposList = new ArrayList<>();
                                for (int i = 0; i < reposNode.size(); i++) {
                                    ObjectNode repoNode = (ObjectNode) reposNode.get(i);
                                    Map<String, Object> repoMap = new HashMap<>();
                                    repoMap.put("path", repoNode.get("path").asText());
                                    if (repoNode.has("config")) {
                                        ObjectNode configNode = (ObjectNode) repoNode.get("config");
                                        Map<String, String> configMap = new HashMap<>();
                                        Iterator<String> configFields = configNode.fieldNames();
                                        while (configFields.hasNext()) {
                                            String field = configFields.next();
                                            configMap.put(field, configNode.get(field).asText());
                                        }
                                        repoMap.put("config", configMap);
                                    }
                                    reposList.add(repoMap);
                                }
                                gitMap.put("repos", reposList);
                            }
                            config.put("git", gitMap);
                        }
                        if (jsonConfig.has("servers")) {
                            ArrayNode serversNode = (ArrayNode) jsonConfig.get("servers");
                            List<Map<String, Object>> serversList = new ArrayList<>();
                            for (int i = 0; i < serversNode.size(); i++) {
                                ObjectNode serverNode = (ObjectNode) serversNode.get(i);
                                Map<String, Object> serverMap = new HashMap<>();
                                serverMap.put("name", serverNode.get("name").asText());
                                serverMap.put("host", serverNode.get("host").asText());
                                serverMap.put("port", serverNode.get("port").asInt());
                                serverMap.put("user", serverNode.get("user").asText());
                                serverMap.put("identityFile", serverNode.get("identityFile").asText());
                                serversList.add(serverMap);
                            }
                            config.put("servers", serversList);
                        }
                    }
                }
                
                if (config != null) {
                    // 提取email
                    String configEmail = (String) config.get("email");
                    
                    // 从git配置中获取email（如果配置文件中没有直接设置）
                    if (configEmail == null && config.containsKey("git")) {
                        Map<String, Object> gitConfigMap = (Map<String, Object>) config.get("git");
                        if (gitConfigMap.containsKey("global")) {
                            Map<String, String> globalConfig = (Map<String, String>) gitConfigMap.get("global");
                            if (globalConfig.containsKey("user.email")) {
                                configEmail = globalConfig.get("user.email");
                                writeLog(String.format("Using email from git.global.user.email: %s", configEmail));
                            }
                        }
                    }
                    
                    // 处理git配置
                    if (gitConfig && config.containsKey("git")) {
                        writeLog("Configuring Git settings...");
                        Map<String, Object> gitConfigMap = (Map<String, Object>) config.get("git");
                        configureGitSettings(gitConfigMap);
                    }
                    
                    // 处理servers配置
                    if (config.containsKey("servers")) {
                        List<Map<String, Object>> serversList = (List<Map<String, Object>>) config.get("servers");
                        writeLog(String.format("Found %d servers in configuration file", serversList.size()));
                        
                        List<ServerConfig> serverConfigs = new ArrayList<>();
                        for (Map<String, Object> serverMap : serversList) {
                            ServerConfig serverConfig = new ServerConfig(
                                (String) serverMap.get("name"),
                                (String) serverMap.get("host"),
                                (Integer) serverMap.get("port"),
                                (String) serverMap.get("user"),
                                (String) serverMap.get("identityFile")
                            );
                            serverConfigs.add(serverConfig);
                        }

                        // 执行操作
                        if (action.equals("generate") || action.equals("all")) {
                            writeLog("Generating SSH keys...");
                            for (ServerConfig serverConfig : serverConfigs) {
                                generateSshKey(configEmail != null ? configEmail : email, serverConfig.getName());
                            }
                        }

                        if (action.equals("config") || action.equals("all")) {
                            writeLog("Configuring SSH config file...");
                            configureSshConfig(serverConfigs);
                        }
                    }

                    writeLog("Configuration completed successfully!");
                    System.exit(0);
                } else {
                    writeLog("ERROR: Failed to read configuration file");
                    System.exit(1);
                }
            } catch (Exception e) {
                writeLog(String.format("ERROR: Failed to read configuration file: %s", e.getMessage()));
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            // 验证必需参数
            if (Strings.isNullOrEmpty(email)) {
                writeLog("ERROR: Email is required");
                writeLog("Usage: java -cp <classpath> com.skillmanager.InitGitSsh -Email <email> [-Action <generate|config|all>] [-Server <gitee|github|gitcode|all>]");
                writeLog("Or: java -cp <classpath> com.skillmanager.InitGitSsh -ConfigFile <config-file>");
                System.exit(1);
            }

            // 执行操作
            if (action.equals("generate") || action.equals("all")) {
                writeLog("Generating SSH keys...");
                if (server.equals("all")) {
                    generateSshKey(email, "gitee");
                    generateSshKey(email, "github");
                    generateSshKey(email, "gitcode");
                } else {
                    generateSshKey(email, server);
                }
            }

            if (action.equals("config") || action.equals("all")) {
                writeLog("Configuring SSH config file...");
                List<ServerConfig> serverConfigs = new ArrayList<>();
                
                if (server.equals("all")) {
                    serverConfigs.add(new ServerConfig("gitee", "gitee.com", 22, email, "~/.ssh/id_rsa.gitee"));
                    serverConfigs.add(new ServerConfig("github", "github.com", 22, email, "~/.ssh/id_rsa.github"));
                    serverConfigs.add(new ServerConfig("gitcode", "gitcode.com", 22, email, "~/.ssh/id_rsa.gitcode"));
                } else if (server.equals("gitee")) {
                    serverConfigs.add(new ServerConfig("gitee", "gitee.com", 22, email, "~/.ssh/id_rsa.gitee"));
                } else if (server.equals("github")) {
                    serverConfigs.add(new ServerConfig("github", "github.com", 22, email, "~/.ssh/id_rsa.github"));
                } else if (server.equals("gitcode")) {
                    serverConfigs.add(new ServerConfig("gitcode", "gitcode.com", 22, email, "~/.ssh/id_rsa.gitcode"));
                }

                configureSshConfig(serverConfigs);
            }

            writeLog("SSH configuration completed successfully!");
        }
    }

    private static ObjectNode readConfigFile(String configFile) throws Exception {
        Preconditions.checkNotNull(configFile, "configFile cannot be null");
        ObjectMapper objectMapper = new ObjectMapper();
        return (ObjectNode) objectMapper.readTree(new File(configFile));
    }

    private static Map<String, Object> readYmlConfigFile(String configFile) throws Exception {
        Preconditions.checkNotNull(configFile, "configFile cannot be null");
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            return yaml.load(inputStream);
        }
    }

    private static void generateSshKey(String email, String server) {
        Preconditions.checkNotNull(email, "email cannot be null");
        Preconditions.checkNotNull(server, "server cannot be null");

        writeLog(String.format("Generating SSH key for %s...", server));

        // 确保.ssh目录存在
        Path sshDirPath = Paths.get(SSH_DIR);
        if (!Files.exists(sshDirPath)) {
            writeLog(String.format("Creating .ssh directory: %s", SSH_DIR));
            try {
                Files.createDirectories(sshDirPath);
                writeLog(String.format(".ssh directory created successfully: %s", SSH_DIR));
            } catch (IOException e) {
                writeLog(String.format("ERROR: Failed to create .ssh directory: %s", e.getMessage()));
                e.printStackTrace();
                System.exit(1);
            }
        }

        // 生成SSH密钥对
        String privateKeyPath = SSH_DIR + "/id_rsa." + server;
        String publicKeyPath = privateKeyPath + ".pub";

        // 检查密钥文件是否已存在
        if (Files.exists(Paths.get(privateKeyPath))) {
            writeLog(String.format("SSH key already exists for %s: %s", server, privateKeyPath));
            return;
        }

        writeLog(String.format("Generating SSH key pair for %s...", server));
        writeLog(String.format("Private key: %s", privateKeyPath));
        writeLog(String.format("Public key: %s", publicKeyPath));

        // 执行ssh-keygen命令
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd.exe", "/c", "ssh-keygen", "-t", "rsa", "-b", "4096", "-C", email, "-f", privateKeyPath, "-N", "");
            } else {
                processBuilder.command("ssh-keygen", "-t", "rsa", "-b", "4096", "-C", email, "-f", privateKeyPath, "-N", "");
            }

            processBuilder.directory(new File(SSH_DIR));
            Process process = processBuilder.start();

            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                writeLog(String.format("ssh-keygen output: %s", line));
            }

            // 读取错误
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                writeLog(String.format("ssh-keygen error: %s", line));
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                writeLog(String.format("SSH key generated successfully for %s", server));
                // 显示公钥内容
                try {
                    String publicKeyContent = new String(Files.readAllBytes(Paths.get(publicKeyPath)));
                    writeLog(String.format("Public key for %s:", server));
                    writeLog(publicKeyContent.trim());
                } catch (IOException e) {
                    writeLog(String.format("WARNING: Failed to read public key file: %s", e.getMessage()));
                }
            } else {
                writeLog(String.format("ERROR: Failed to generate SSH key for %s, exit code: %d", server, exitCode));
            }
        } catch (Exception e) {
            writeLog(String.format("ERROR: Failed to generate SSH key for %s: %s", server, e.getMessage()));
            e.printStackTrace();
        }
    }

    private static void configureSshConfig(List<ServerConfig> serverConfigs) {
        Preconditions.checkNotNull(serverConfigs, "serverConfigs cannot be null");
        Preconditions.checkArgument(!serverConfigs.isEmpty(), "serverConfigs cannot be empty");

        writeLog("Configuring SSH config file...");
        writeLog(String.format("SSH config file: %s", SSH_CONFIG_PATH));

        // 读取现有配置
        StringBuilder configContent = new StringBuilder();
        Path configPath = Paths.get(SSH_CONFIG_PATH);

        if (Files.exists(configPath)) {
            writeLog("Reading existing SSH config file...");
            try {
                configContent.append(new String(Files.readAllBytes(configPath)));
                // 确保文件以换行结束
                if (configContent.length() > 0 && !configContent.toString().endsWith("\n")) {
                    configContent.append("\n");
                }
            } catch (IOException e) {
                writeLog(String.format("WARNING: Failed to read existing SSH config file: %s", e.getMessage()));
            }
        }

        // 添加新配置
        for (ServerConfig serverConfig : serverConfigs) {
            writeLog(String.format("Adding SSH config for %s...", serverConfig.getName()));
            configContent.append(String.format("#Add %s user\n", serverConfig.getName()));
            configContent.append(String.format("Host %s\n", serverConfig.getName()));
            configContent.append(String.format("    HostName %s\n", serverConfig.getHost()));
            configContent.append(String.format("    User %s\n", serverConfig.getUser()));
            configContent.append("    PreferredAuthentications publickey\n");
            configContent.append(String.format("    IdentityFile %s\n", serverConfig.getIdentityFile()));
            configContent.append("    AddKeysToAgent yes\n");
            configContent.append(String.format("    Port %d\n\n", serverConfig.getPort()));

            // 添加HostName配置
            configContent.append(String.format("Host %s\n", serverConfig.getHost()));
            configContent.append(String.format("    HostName %s\n", serverConfig.getHost()));
            configContent.append(String.format("    User %s\n", serverConfig.getUser()));
            configContent.append("    PreferredAuthentications publickey\n");
            configContent.append(String.format("    IdentityFile %s\n", serverConfig.getIdentityFile()));
            configContent.append("    AddKeysToAgent yes\n");
            configContent.append(String.format("    Port %d\n\n", serverConfig.getPort()));
        }

        // 写入配置文件
        try {
            Files.write(configPath, configContent.toString().getBytes());
            writeLog(String.format("SSH config file updated successfully: %s", SSH_CONFIG_PATH));
        } catch (IOException e) {
            writeLog(String.format("ERROR: Failed to write SSH config file: %s", e.getMessage()));
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void configureGitSettings(Map<String, Object> gitConfig) {
        Preconditions.checkNotNull(gitConfig, "gitConfig cannot be null");

        // 配置全局git设置
        if (gitConfig.containsKey("global")) {
            Map<String, String> globalConfig = (Map<String, String>) gitConfig.get("global");
            if (!globalConfig.isEmpty()) {
                writeLog("Configuring global Git settings...");
                setGitGlobalConfig(globalConfig);
            }
        }

        // 配置仓库特定的git设置
        if (gitConfig.containsKey("repos")) {
            List<Map<String, Object>> repos = (List<Map<String, Object>>) gitConfig.get("repos");
            for (Map<String, Object> repo : repos) {
                String path = (String) repo.get("path");
                if (!Strings.isNullOrEmpty(path) && repo.containsKey("config")) {
                    Map<String, String> repoConfig = (Map<String, String>) repo.get("config");
                    if (!repoConfig.isEmpty()) {
                        writeLog(String.format("Configuring Git settings for repo: %s", path));
                        setGitRepoConfig(path, repoConfig);
                    }
                }
            }
        }
    }

    private static void setGitGlobalConfig(Map<String, String> globalConfig) {
        Preconditions.checkNotNull(globalConfig, "globalConfig cannot be null");

        for (Map.Entry<String, String> entry : globalConfig.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            writeLog(String.format("Setting global Git config: %s = %s", key, value));
            executeGitCommand("config", "--global", key, value);
        }
    }

    private static void setGitRepoConfig(String repoPath, Map<String, String> repoConfig) {
        Preconditions.checkNotNull(repoPath, "repoPath cannot be null");
        Preconditions.checkNotNull(repoConfig, "repoConfig cannot be null");

        File repoDir = new File(repoPath);
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            writeLog(String.format("WARNING: Repository directory does not exist: %s", repoPath));
            return;
        }

        for (Map.Entry<String, String> entry : repoConfig.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            writeLog(String.format("Setting Git config for %s: %s = %s", repoPath, key, value));
            executeGitCommand("-C", repoPath, "config", key, value);
        }
    }

    private static void executeGitCommand(String... args) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            List<String> command = new ArrayList<>();
            command.add("git");
            command.addAll(Arrays.asList(args));

            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd.exe", "/c");
                processBuilder.command().addAll(command);
            } else {
                processBuilder.command(command);
            }

            Process process = processBuilder.start();

            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                writeLog(String.format("git output: %s", line));
            }

            // 读取错误
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                writeLog(String.format("git error: %s", line));
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                writeLog(String.format("ERROR: Git command failed with exit code: %d", exitCode));
            }
        } catch (Exception e) {
            writeLog(String.format("ERROR: Failed to execute git command: %s", e.getMessage()));
            e.printStackTrace();
        }
    }

    private static void writeLog(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        System.out.printf("[%s] %s%n", timestamp, message);
    }

    private static class ServerConfig {
        private final String name;
        private final String host;
        private final int port;
        private final String user;
        private final String identityFile;

        public ServerConfig(String name, String host, int port, String user, String identityFile) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.user = user;
            this.identityFile = identityFile;
        }

        public String getName() {
            return name;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUser() {
            return user;
        }

        public String getIdentityFile() {
            return identityFile;
        }
    }
}
