package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

// 全局变量
var (
	sshDir       = filepath.Join(os.Getenv("USERPROFILE"), ".ssh")
	sshConfigPath = filepath.Join(sshDir, "config")
)

// 配置结构
type Config struct {
	Email   string    `json:"email" yaml:"email"`
	Git     *GitConfig `json:"git,omitempty" yaml:"git,omitempty"`
	Servers []ServerConfig `json:"servers" yaml:"servers"`
}

// Git配置结构
type GitConfig struct {
	Global map[string]string `json:"global,omitempty" yaml:"global,omitempty"`
	Repos  []RepoConfig `json:"repos,omitempty" yaml:"repos,omitempty"`
}

// 仓库配置结构
type RepoConfig struct {
	Path   string            `json:"path" yaml:"path"`
	Config map[string]string `json:"config,omitempty" yaml:"config,omitempty"`
}

// 服务器配置结构
type ServerConfig struct {
	Name         string `json:"name" yaml:"name"`
	Host         string `json:"host" yaml:"host"`
	Port         int    `json:"port" yaml:"port"`
	User         string `json:"user" yaml:"user"`
	IdentityFile string `json:"identityFile" yaml:"identityFile"`
}

func main() {
	// 解析命令行参数
	action := "all"
	email := ""
	server := "all"
	configFile := ""
	gitConfig := true

	for i := 0; i < len(os.Args); i++ {
		if os.Args[i] == "-Action" && i+1 < len(os.Args) {
			action = os.Args[i+1]
		} else if os.Args[i] == "-Email" && i+1 < len(os.Args) {
			email = os.Args[i+1]
		} else if os.Args[i] == "-Server" && i+1 < len(os.Args) {
			server = os.Args[i+1]
		} else if os.Args[i] == "-ConfigFile" && i+1 < len(os.Args) {
			configFile = os.Args[i+1]
		} else if os.Args[i] == "-GitConfig" && i+1 < len(os.Args) {
			gitConfig = os.Args[i+1] == "true"
		}
	}

	writeLog("Starting Git SSH initialization...")

	// 处理配置文件
	if configFile != "" {
		writeLog(fmt.Sprintf("Using configuration file: %s", configFile))
		
		// 读取配置文件
		config, err := readConfigFile(configFile)
		if err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to read configuration file: %v", err))
			os.Exit(1)
		}

		// 提取email
		configEmail := config.Email
		
		// 从git配置中获取email（如果配置文件中没有直接设置）
		if configEmail == "" && config.Git != nil && config.Git.Global != nil {
			if emailFromGit, ok := config.Git.Global["user.email"]; ok {
				configEmail = emailFromGit
				writeLog(fmt.Sprintf("Using email from git.global.user.email: %s", configEmail))
			}
		}

		// 处理git配置
		if gitConfig && config.Git != nil {
			writeLog("Configuring Git settings...")
			configureGitSettings(config.Git)
		}

		// 处理servers配置
		if len(config.Servers) > 0 {
			writeLog(fmt.Sprintf("Found %d servers in configuration file", len(config.Servers)))

			// 执行操作
			if action == "generate" || action == "all" {
				writeLog("Generating SSH keys...")
				for _, serverConfig := range config.Servers {
					if configEmail != "" {
						generateSshKey(configEmail, serverConfig.Name)
					} else if email != "" {
						generateSshKey(email, serverConfig.Name)
					} else {
						writeLog("ERROR: Email is required")
						os.Exit(1)
					}
				}
			}

			if action == "config" || action == "all" {
				writeLog("Configuring SSH config file...")
				configureSshConfig(config.Servers)
			}
		}

		writeLog("Configuration completed successfully!")
		os.Exit(0)
	} else {
		// 验证必需参数
		if email == "" {
			writeLog("ERROR: Email is required")
			writeLog("Usage: init-git-ssh -Email <email> [-Action <generate|config|all>] [-Server <gitee|github|gitcode|all>]")
			writeLog("Or: init-git-ssh -ConfigFile <config-file>")
			os.Exit(1)
		}

		// 执行操作
		if action == "generate" || action == "all" {
			writeLog("Generating SSH keys...")
			if server == "all" {
				generateSshKey(email, "gitee")
				generateSshKey(email, "github")
				generateSshKey(email, "gitcode")
			} else {
				generateSshKey(email, server)
			}
		}

		if action == "config" || action == "all" {
			writeLog("Configuring SSH config file...")
			serverConfigs := []ServerConfig{}

			if server == "all" {
				serverConfigs = append(serverConfigs, ServerConfig{
					Name:         "gitee",
					Host:         "gitee.com",
					Port:         22,
					User:         email,
					IdentityFile: filepath.Join("~", ".ssh", "id_rsa.gitee"),
				})
				serverConfigs = append(serverConfigs, ServerConfig{
					Name:         "github",
					Host:         "github.com",
					Port:         22,
					User:         email,
					IdentityFile: filepath.Join("~", ".ssh", "id_rsa.github"),
				})
				serverConfigs = append(serverConfigs, ServerConfig{
					Name:         "gitcode",
					Host:         "gitcode.com",
					Port:         22,
					User:         email,
					IdentityFile: filepath.Join("~", ".ssh", "id_rsa.gitcode"),
				})
			} else if server == "gitee" {
				serverConfigs = append(serverConfigs, ServerConfig{
					Name:         "gitee",
					Host:         "gitee.com",
					Port:         22,
					User:         email,
					IdentityFile: filepath.Join("~", ".ssh", "id_rsa.gitee"),
				})
			} else if server == "github" {
				serverConfigs = append(serverConfigs, ServerConfig{
					Name:         "github",
					Host:         "github.com",
					Port:         22,
					User:         email,
					IdentityFile: filepath.Join("~", ".ssh", "id_rsa.github"),
				})
			} else if server == "gitcode" {
				serverConfigs = append(serverConfigs, ServerConfig{
					Name:         "gitcode",
					Host:         "gitcode.com",
					Port:         22,
					User:         email,
					IdentityFile: filepath.Join("~", ".ssh", "id_rsa.gitcode"),
				})
			}

			configureSshConfig(serverConfigs)
		}

		writeLog("SSH configuration completed successfully!")
	}
}

// 读取配置文件
func readConfigFile(configFile string) (*Config, error) {
	// 读取文件内容
	data, err := ioutil.ReadFile(configFile)
	if err != nil {
		return nil, err
	}

	// 解析JSON配置文件
	config := &Config{}
	err = json.Unmarshal(data, config)
	if err != nil {
		return nil, err
	}
	return config, nil
}

// 生成SSH密钥对
func generateSshKey(email, server string) {
	writeLog(fmt.Sprintf("Generating SSH key for %s...", server))

	// 确保.ssh目录存在
	if err := os.MkdirAll(sshDir, 0700); err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to create .ssh directory: %v", err))
		os.Exit(1)
	}

	// 生成SSH密钥对
	privateKeyPath := filepath.Join(sshDir, fmt.Sprintf("id_rsa.%s", server))
	publicKeyPath := privateKeyPath + ".pub"

	// 检查密钥文件是否已存在
	if _, err := os.Stat(privateKeyPath); err == nil {
		writeLog(fmt.Sprintf("SSH key already exists for %s: %s", server, privateKeyPath))
		return
	}

	writeLog(fmt.Sprintf("Generating SSH key pair for %s...", server))
	writeLog(fmt.Sprintf("Private key: %s", privateKeyPath))
	writeLog(fmt.Sprintf("Public key: %s", publicKeyPath))

	// 执行ssh-keygen命令
	var cmd *exec.Cmd
	if strings.Contains(strings.ToLower(os.Getenv("OS")), "windows") {
		cmd = exec.Command("cmd.exe", "/c", "ssh-keygen", "-t", "rsa", "-b", "4096", "-C", email, "-f", privateKeyPath, "-N", "")
	} else {
		cmd = exec.Command("ssh-keygen", "-t", "rsa", "-b", "4096", "-C", email, "-f", privateKeyPath, "-N", "")
	}

	cmd.Dir = sshDir
	op, err := cmd.CombinedOutput()
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to generate SSH key for %s: %v", server, err))
		writeLog(fmt.Sprintf("Output: %s", string(op)))
		return
	}

	writeLog(fmt.Sprintf("SSH key generated successfully for %s", server))

	// 显示公钥内容
	pubKeyData, err := ioutil.ReadFile(publicKeyPath)
	if err != nil {
		writeLog(fmt.Sprintf("WARNING: Failed to read public key file: %v", err))
		return
	}

	writeLog(fmt.Sprintf("Public key for %s:", server))
	writeLog(strings.TrimSpace(string(pubKeyData)))
}

// 配置SSH config文件
func configureSshConfig(serverConfigs []ServerConfig) {
	writeLog("Configuring SSH config file...")
	writeLog(fmt.Sprintf("SSH config file: %s", sshConfigPath))

	// 读取现有配置
	configContent := ""
	if _, err := os.Stat(sshConfigPath); err == nil {
		writeLog("Reading existing SSH config file...")
		data, err := ioutil.ReadFile(sshConfigPath)
		if err != nil {
			writeLog(fmt.Sprintf("WARNING: Failed to read existing SSH config file: %v", err))
		} else {
			configContent = string(data)
			// 确保文件以换行结束
			if configContent != "" && !strings.HasSuffix(configContent, "\n") {
				configContent += "\n"
			}
		}
	}

	// 添加新配置
	for _, serverConfig := range serverConfigs {
		writeLog(fmt.Sprintf("Adding SSH config for %s...", serverConfig.Name))
		configContent += fmt.Sprintf("#Add %s user\n", serverConfig.Name)
		configContent += fmt.Sprintf("Host %s\n", serverConfig.Name)
		configContent += fmt.Sprintf("    HostName %s\n", serverConfig.Host)
		configContent += fmt.Sprintf("    User %s\n", serverConfig.User)
		configContent += "    PreferredAuthentications publickey\n"
		configContent += fmt.Sprintf("    IdentityFile %s\n", serverConfig.IdentityFile)
		configContent += "    AddKeysToAgent yes\n"
		configContent += fmt.Sprintf("    Port %d\n\n", serverConfig.Port)

		// 添加HostName配置
		configContent += fmt.Sprintf("Host %s\n", serverConfig.Host)
		configContent += fmt.Sprintf("    HostName %s\n", serverConfig.Host)
		configContent += fmt.Sprintf("    User %s\n", serverConfig.User)
		configContent += "    PreferredAuthentications publickey\n"
		configContent += fmt.Sprintf("    IdentityFile %s\n", serverConfig.IdentityFile)
		configContent += "    AddKeysToAgent yes\n"
		configContent += fmt.Sprintf("    Port %d\n\n", serverConfig.Port)
	}

	// 写入配置文件
	if err := ioutil.WriteFile(sshConfigPath, []byte(configContent), 0600); err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to write SSH config file: %v", err))
		os.Exit(1)
	}

	writeLog(fmt.Sprintf("SSH config file updated successfully: %s", sshConfigPath))
}

// 配置Git设置
func configureGitSettings(gitConfig *GitConfig) {
	// 配置全局git设置
	if gitConfig.Global != nil && len(gitConfig.Global) > 0 {
		writeLog("Configuring global Git settings...")
		setGitGlobalConfig(gitConfig.Global)
	}

	// 配置仓库特定的git设置
	if gitConfig.Repos != nil && len(gitConfig.Repos) > 0 {
		for _, repo := range gitConfig.Repos {
			if repo.Path != "" && repo.Config != nil && len(repo.Config) > 0 {
				writeLog(fmt.Sprintf("Configuring Git settings for repo: %s", repo.Path))
				setGitRepoConfig(repo.Path, repo.Config)
			}
		}
	}
}

// 设置全局Git配置
func setGitGlobalConfig(globalConfig map[string]string) {
	for key, value := range globalConfig {
		writeLog(fmt.Sprintf("Setting global Git config: %s = %s", key, value))
		executeGitCommand("config", "--global", key, value)
	}
}

// 设置仓库特定的Git配置
func setGitRepoConfig(repoPath string, repoConfig map[string]string) {
	// 检查仓库目录是否存在
	if _, err := os.Stat(repoPath); err != nil {
		writeLog(fmt.Sprintf("WARNING: Repository directory does not exist: %s", repoPath))
		return
	}

	for key, value := range repoConfig {
		writeLog(fmt.Sprintf("Setting Git config for %s: %s = %s", repoPath, key, value))
		executeGitCommand("-C", repoPath, "config", key, value)
	}
}

// 执行Git命令
func executeGitCommand(args ...string) {
	var cmd *exec.Cmd
	if strings.Contains(strings.ToLower(os.Getenv("OS")), "windows") {
		cmdArgs := append([]string{"/c", "git"}, args...)
		cmd = exec.Command("cmd.exe", cmdArgs...)
	} else {
		cmdArgs := append([]string{"git"}, args...)
		cmd = exec.Command(cmdArgs[0], cmdArgs[1:]...)
	}

	op, err := cmd.CombinedOutput()
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Git command failed: %v", err))
		writeLog(fmt.Sprintf("Output: %s", string(op)))
		return
	}

	if len(op) > 0 {
		writeLog(fmt.Sprintf("git output: %s", strings.TrimSpace(string(op))))
	}
}

// 写入日志
func writeLog(message string) {
	timestamp := time.Now().Format("2006-01-02 15:04:05")
	fmt.Printf("[%s] %s\n", timestamp, message)
}
