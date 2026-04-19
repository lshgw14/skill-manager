package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"strings"
	"time"
)

// 全局变量
var (
	dateFormat = "2006-01-02 15:04:05"
)

// 配置结构
type BatchConfig struct {
	BatchOperations []BatchOperation `json:"batchOperations"`
}

// 批量操作结构
type BatchOperation struct {
	Type   string      `json:"type"`
	Repos  []RepoConfig `json:"repos,omitempty"`
	TargetPath string `json:"targetPath,omitempty"`
	Email  string   `json:"email,omitempty"`
	Action string   `json:"action,omitempty"`
	Server string   `json:"server,omitempty"`
	ConfigFile string `json:"configFile,omitempty"`
	GitConfig *bool  `json:"gitConfig,omitempty"`
}

// 仓库配置结构
type RepoConfig struct {
	RepoName   string   `json:"repoName,omitempty"`
	RepoUrl    string   `json:"repoUrl"`
	LocalPath  string   `json:"localPath"`
	RepoPath   string   `json:"repoPath"`
	SkillNames []string `json:"skillNames,omitempty"`
}

func main() {
	// 解析命令行参数
	configFile := "batch-config.json"

	for i := 0; i < len(os.Args); i++ {
		if os.Args[i] == "-ConfigFile" && i+1 < len(os.Args) {
			configFile = os.Args[i+1]
		}
	}

	writeLog("Starting batch operations...")
	writeLog(fmt.Sprintf("Config file: %s", configFile))

	// 检查配置文件是否存在
	if _, err := os.Stat(configFile); os.IsNotExist(err) {
		writeLog(fmt.Sprintf("ERROR: Config file does not exist: %s", configFile))
		os.Exit(1)
	}

	// 解析配置文件
	data, err := ioutil.ReadFile(configFile)
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to read config file: %v", err))
		os.Exit(1)
	}

	config := &BatchConfig{}
	if err := json.Unmarshal(data, config); err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to parse config file: %v", err))
		os.Exit(1)
	}

	// 提取批量操作列表
	batchOperations := config.BatchOperations
	if len(batchOperations) == 0 {
		writeLog("ERROR: No batch operations found in config file")
		os.Exit(1)
	}

	// 执行批量操作
	operationCount := 0
	successCount := 0
	failCount := 0

	for _, operation := range batchOperations {
		operationCount++
		writeLog("\n==========================================")
		writeLog(fmt.Sprintf("Executing operation %d of %d", operationCount, len(batchOperations)))
		writeLog("==========================================")

		operationType := operation.Type
		if operationType == "" {
			writeLog("ERROR: Operation type not specified")
			failCount++
			continue
		}

		switch strings.ToLower(operationType) {
		case "init":
			if executeInitOperation(&operation) {
				successCount++
			} else {
				failCount++
			}
		case "sync":
			if executeSyncOperation(&operation) {
				successCount++
			} else {
				failCount++
			}
		case "ssh":
			if executeSshOperation(&operation) {
				successCount++
			} else {
				failCount++
			}
		default:
			writeLog(fmt.Sprintf("ERROR: Invalid operation type: %s", operationType))
			failCount++
		}
	}

	writeLog("\n==========================================")
	writeLog("Batch operations completed")
	writeLog(fmt.Sprintf("Total operations: %d", operationCount))
	writeLog(fmt.Sprintf("Successful: %d", successCount))
	writeLog(fmt.Sprintf("Failed: %d", failCount))
	writeLog("==========================================")
}

// 执行初始化操作
func executeInitOperation(operation *BatchOperation) bool {
	writeLog("Executing init operation...")

	repos := operation.Repos
	if len(repos) == 0 {
		writeLog("ERROR: No repos specified for init operation")
		return false
	}

	repoCount := 0
	successCount := 0
	failCount := 0

	for _, repo := range repos {
		repoCount++
		repoName := repo.RepoName
		if repoName == "" {
			repoName = "Unknown"
		}
		writeLog(fmt.Sprintf("\nInitializing repo %d of %d: %s", repoCount, len(repos), repoName))

		repoUrl := repo.RepoUrl
		localPath := repo.LocalPath

		if repoUrl == "" {
			writeLog("ERROR: repoUrl not specified")
			failCount++
			continue
		}

		if localPath == "" {
			writeLog("ERROR: localPath not specified")
			failCount++
			continue
		}

		writeLog(fmt.Sprintf("Repo URL: %s", repoUrl))
		writeLog(fmt.Sprintf("Local path: %s", localPath))

		// 调用 init-skill-repo 执行初始化
		cmd := exec.Command("./init-skill-repo.exe", "-RepoUrl", repoUrl, "-LocalPath", localPath)
		cmd.Dir = "."
		op, err := cmd.CombinedOutput()
		if err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to initialize repo: %v", err))
			writeLog(fmt.Sprintf("Output: %s", string(op)))
			failCount++
		} else {
			writeLog("Repo initialized successfully!")
			successCount++
		}
	}

	writeLog(fmt.Sprintf("Init operation completed: %d successful, %d failed", successCount, failCount))
	return failCount == 0
}

// 执行同步操作
func executeSyncOperation(operation *BatchOperation) bool {
	writeLog("Executing sync operation...")

	targetPath := operation.TargetPath
	repos := operation.Repos

	if targetPath == "" {
		writeLog("ERROR: targetPath not specified for sync operation")
		return false
	}

	if len(repos) == 0 {
		writeLog("ERROR: No repos specified for sync operation")
		return false
	}

	// 为每个 repo 创建一个临时配置文件
	syncConfig := struct {
		TargetPath string       `json:"targetPath"`
		Repos      []RepoConfig `json:"repos"`
	}{
		TargetPath: targetPath,
		Repos:      repos,
	}

	// 打印同步操作的仓库信息
	writeLog("\nSyncing repositories:")
	repoIndex := 0
	for _, repo := range repos {
		repoIndex++
		repoName := repo.RepoName
		if repoName == "" {
			repoName = "Unknown"
		}
		repoPath := repo.RepoPath
		writeLog(fmt.Sprintf("%d. %s - %s", repoIndex, repoName, repoPath))
	}

	// 写入临时配置文件
	tempConfigPath := "temp-sync-config.json"
	data, err := json.MarshalIndent(syncConfig, "", "  ")
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to marshal sync config: %v", err))
		return false
	}

	if err := ioutil.WriteFile(tempConfigPath, data, 0644); err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to create temp config file: %v", err))
		return false
	}

	// 调用 sync-skills 执行同步
	cmd := exec.Command("./sync-skills.exe", "-ConfigFile", tempConfigPath)
	cmd.Dir = "."
	op, err := cmd.CombinedOutput()
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to execute sync operation: %v", err))
		writeLog(fmt.Sprintf("Output: %s", string(op)))
		// 删除临时配置文件
		os.Remove(tempConfigPath)
		return false
	} else {
		writeLog("Sync operation completed successfully!")
		// 删除临时配置文件
		os.Remove(tempConfigPath)
		return true
	}
}

// 执行SSH操作
func executeSshOperation(operation *BatchOperation) bool {
	writeLog("Executing SSH operation...")

	email := operation.Email
	action := operation.Action
	server := operation.Server
	configFile := operation.ConfigFile
	gitConfig := operation.GitConfig

	// 构建命令参数
	cmdArgs := []string{"./init-git-ssh.exe"}

	// 添加GitConfig参数
	if gitConfig != nil {
		cmdArgs = append(cmdArgs, "-GitConfig", fmt.Sprintf("%v", *gitConfig))
	}

	if configFile != "" {
		cmdArgs = append(cmdArgs, "-ConfigFile", configFile)
	} else {
		if email == "" {
			writeLog("ERROR: email not specified for SSH operation")
			return false
		}
		cmdArgs = append(cmdArgs, "-Email", email)

		if action != "" {
			cmdArgs = append(cmdArgs, "-Action", action)
		}

		if server != "" {
			cmdArgs = append(cmdArgs, "-Server", server)
		}
	}

	// 执行命令
	cmd := exec.Command(cmdArgs[0], cmdArgs[1:]...)
	cmd.Dir = "."
	op, err := cmd.CombinedOutput()
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to execute SSH operation: %v", err))
		writeLog(fmt.Sprintf("Output: %s", string(op)))
		return false
	} else {
		writeLog("SSH operation completed successfully!")
		return true
	}
}

// 写入日志
func writeLog(message string) {
	timestamp := time.Now().Format(dateFormat)
	fmt.Printf("[%s] %s\n", timestamp, message)
}
