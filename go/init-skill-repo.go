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
	dateFormat = "2006-01-02 15:04:05"
)

// 配置结构
type InitConfig struct {
	Repos []RepoConfig `json:"repos"`
}

// 仓库配置结构
type RepoConfig struct {
	RepoName  string `json:"repoName,omitempty"`
	RepoUrl   string `json:"repoUrl"`
	LocalPath string `json:"localPath"`
}

func main() {
	// 解析命令行参数
	repoUrl := ""
	localPath := ""
	configFile := ""

	for i := 0; i < len(os.Args); i++ {
		if os.Args[i] == "-RepoUrl" && i+1 < len(os.Args) {
			repoUrl = os.Args[i+1]
		} else if os.Args[i] == "-LocalPath" && i+1 < len(os.Args) {
			localPath = os.Args[i+1]
		} else if os.Args[i] == "-ConfigFile" && i+1 < len(os.Args) {
			configFile = os.Args[i+1]
		}
	}

	writeLog("Starting skill repo initialization...")

	// 处理配置文件
	if configFile != "" {
		writeLog(fmt.Sprintf("Using configuration file: %s", configFile))
		
		// 读取配置文件
		config, err := readConfigFile(configFile)
		if err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to read configuration file: %v", err))
			os.Exit(1)
		}

		if len(config.Repos) > 0 {
			writeLog(fmt.Sprintf("Found %d repositories to initialize", len(config.Repos)))
			for i, repo := range config.Repos {
				repoName := repo.RepoName
				if repoName == "" {
					repoName = "Unknown"
				}
				repoUrlFromConfig := repo.RepoUrl
				localPathFromConfig := repo.LocalPath
				
				if repoUrlFromConfig == "" {
					writeLog("ERROR: repoUrl is required in config file")
					os.Exit(1)
				}
				if localPathFromConfig == "" {
					writeLog("ERROR: localPath is required in config file")
					os.Exit(1)
				}
				
				writeLog(fmt.Sprintf("\nInitializing repository %d/%d: %s", i+1, len(config.Repos), repoName))
				writeLog(fmt.Sprintf("Repo URL: %s", repoUrlFromConfig))
				writeLog(fmt.Sprintf("Local path: %s", localPathFromConfig))
				
				initRepo(repoUrlFromConfig, localPathFromConfig, repoName)
			}
			writeLog("\nAll repositories initialized successfully!")
			os.Exit(0)
		} else {
			writeLog("ERROR: No repositories found in configuration file")
			os.Exit(1)
		}
	} else {
		// 验证必需参数
		if repoUrl == "" {
			writeLog("ERROR: RepoUrl is required")
			writeLog("Usage: init-skill-repo -RepoUrl <repository-url> [-LocalPath <local-path>]")
			writeLog("Or: init-skill-repo -ConfigFile <config-file>")
			os.Exit(1)
		}

		// 如果没有指定本地路径，使用当前目录
		if localPath == "" {
			localPath, _ = os.Getwd()
		}

		// 从repoUrl中提取repoName
		repoName := extractRepoName(repoUrl)
		writeLog(fmt.Sprintf("Repo URL: %s", repoUrl))
		writeLog(fmt.Sprintf("Repo Name: %s", repoName))
		writeLog(fmt.Sprintf("Local path: %s", localPath))
		initRepo(repoUrl, localPath, repoName)
		writeLog("Initialization completed!")
	}
}

// 读取配置文件
func readConfigFile(configFile string) (*InitConfig, error) {
	// 读取文件内容
	data, err := ioutil.ReadFile(configFile)
	if err != nil {
		return nil, err
	}

	// 解析JSON配置文件
	config := &InitConfig{}
	err = json.Unmarshal(data, config)
	if err != nil {
		return nil, err
	}

	return config, nil
}

// 从repoUrl中提取repoName
func extractRepoName(repoUrl string) string {
	// 从repoUrl中提取repoName
	// 对于https://github.com/anthropics/skills.git，提取anthropics_skills
	// 移除.git后缀
	urlWithoutGit := strings.TrimSuffix(repoUrl, ".git")
	// 提取最后两个路径段
	parts := strings.Split(urlWithoutGit, "/")
	if len(parts) >= 2 {
		owner := parts[len(parts)-2]
		repo := parts[len(parts)-1]
		return owner + "_" + repo
	}
	// 如果提取失败，返回默认值
	writeLog(fmt.Sprintf("WARNING: Failed to extract repo name from URL: %s", repoUrl))
	return "Unknown"
}

// 初始化仓库
func initRepo(repoUrl, localPath, repoName string) {
	// 检查本地路径是否存在，不存在则创建
	if _, err := os.Stat(localPath); os.IsNotExist(err) {
		writeLog(fmt.Sprintf("Creating local directory: %s", localPath))
		if err := os.MkdirAll(localPath, 0755); err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to create local directory: %v", err))
			os.Exit(1)
		}
		writeLog(fmt.Sprintf("Local directory created successfully: %s", localPath))
	} else {
		writeLog(fmt.Sprintf("Local directory already exists: %s", localPath))
		// 检查目录是否为空
		files, err := os.ReadDir(localPath)
		if err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to read directory: %v", err))
			os.Exit(1)
		}
		if len(files) > 0 {
			writeLog(fmt.Sprintf("ERROR: Directory is not empty: %s", localPath))
			writeLog("Git clone requires an empty directory or a non-existent directory")
			return
		}
	}

	// 执行git clone命令
	writeLog(fmt.Sprintf("Cloning repository: %s...", repoName))
	writeLog(fmt.Sprintf("Repository URL: %s", repoUrl))
	writeLog(fmt.Sprintf("Local directory: %s", localPath))

	var cmd *exec.Cmd
	if strings.Contains(strings.ToLower(os.Getenv("OS")), "windows") {
		cmd = exec.Command("cmd.exe", "/c", "git", "clone", repoUrl, localPath)
	} else {
		cmd = exec.Command("git", "clone", repoUrl, localPath)
	}

	op, err := cmd.CombinedOutput()
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Git clone error: %v", err))
		writeLog(fmt.Sprintf("Output: %s", string(op)))
		writeLog("Possible causes:")
		writeLog("1. Network connection issues")
		writeLog("2. SSH authentication failure (check your SSH keys)")
		writeLog("3. Invalid repository URL")
		writeLog("4. Repository does not exist")
		writeLog("5. Firewall or proxy restrictions")
		os.Exit(1)
	}

	writeLog("Repository cloned successfully!")
	// 验证克隆结果
	if _, err := os.Stat(localPath); err == nil {
		writeLog("Cloned repository contents:")
		listDirectory(localPath)
	}
}

// 列出目录内容
func listDirectory(path string) {
	// 只列出两级目录
	walkDepth := 2
	listDirectoryRecursive(path, "", 0, walkDepth)
}

// 递归列出目录内容
func listDirectoryRecursive(path, prefix string, depth, maxDepth int) {
	if depth > maxDepth {
		return
	}

	files, err := os.ReadDir(path)
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to list directory: %v", err))
		return
	}

	for _, file := range files {
		if file.IsDir() {
			fmt.Printf("  [DIR] %s%s\n", prefix, file.Name())
			listDirectoryRecursive(filepath.Join(path, file.Name()), prefix+file.Name()+"\\", depth+1, maxDepth)
		} else {
			fmt.Printf("  [FILE] %s%s\n", prefix, file.Name())
		}
	}
}

// 写入日志
func writeLog(message string) {
	timestamp := time.Now().Format(dateFormat)
	fmt.Printf("[%s] %s\n", timestamp, message)
}
