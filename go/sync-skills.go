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
type SyncConfig struct {
	TargetPath string       `json:"targetPath"`
	Repos      []SyncRepoConfig `json:"repos"`
}

// 同步仓库配置结构
type SyncRepoConfig struct {
	RepoPath   string   `json:"repoPath"`
	RepoName   string   `json:"repoName,omitempty"`
	SkillNames []string `json:"skillNames"`
}

func main() {
	// 解析命令行参数
	skillName := ""
	repoPath := "E:\\develop\\code\\open-source\\github\\skills\\mattpocock\\skills"
	targetPath := "C:\\Users\\admin\\.trae-cn\\skills\\"
	configFile := ""

	for i := 0; i < len(os.Args); i++ {
		if os.Args[i] == "-SkillName" && i+1 < len(os.Args) {
			skillName = os.Args[i+1]
		} else if os.Args[i] == "-RepoPath" && i+1 < len(os.Args) {
			repoPath = os.Args[i+1]
		} else if os.Args[i] == "-TargetPath" && i+1 < len(os.Args) {
			targetPath = os.Args[i+1]
		} else if os.Args[i] == "-ConfigFile" && i+1 < len(os.Args) {
			configFile = os.Args[i+1]
		}
	}

	writeLog("Starting skill sync...")

	// 如果指定了配置文件，从配置文件读取同步任务
	if configFile != "" {
		// 检查配置文件是否存在
		if _, err := os.Stat(configFile); os.IsNotExist(err) {
			writeLog(fmt.Sprintf("ERROR: Config file does not exist: %s", configFile))
			os.Exit(1)
		}

		writeLog(fmt.Sprintf("Reading config file: %s", configFile))

		// 读取和解析 JSON 配置文件
		data, err := ioutil.ReadFile(configFile)
		if err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to read config file: %v", err))
			os.Exit(1)
		}

		// 初始化计数器
		successCount := 0
		failCount := 0

		// 尝试解析为单个配置对象
		var singleConfig SyncConfig
		if err := json.Unmarshal(data, &singleConfig); err == nil {
			// 处理单个配置对象的情况
			writeLog("Processing single config object")
			processSyncConfig(&singleConfig, &successCount, &failCount)
		} else {
			// 尝试解析为配置对象列表
			var configList []SyncConfig
			if err := json.Unmarshal(data, &configList); err == nil {
				// 处理配置对象列表的情况
				writeLog(fmt.Sprintf("Processing config list with %d items", len(configList)))
				for _, config := range configList {
					processSyncConfig(&config, &successCount, &failCount)
				}
			} else {
				writeLog(fmt.Sprintf("ERROR: Invalid JSON config file: %v", err))
				os.Exit(1)
			}
		}

		writeLog(fmt.Sprintf("Config file processing completed. Success: %d, Failed: %d", successCount, failCount))
	} else {
		// 没有指定配置文件，使用命令行参数执行同步
		syncSkill(skillName, repoPath, targetPath)
	}

	writeLog("Sync completed!")
}

// 处理同步配置
func processSyncConfig(config *SyncConfig, successCount, failCount *int) {
	groupTargetPath := config.TargetPath
	if groupTargetPath == "" {
		writeLog("WARNING: targetPath is empty, skipping")
		return
	}

	repos := config.Repos
	if len(repos) == 0 {
		writeLog("WARNING: repos is empty, skipping")
		return
	}

	for _, repo := range repos {
		repoName := repo.RepoName
		if repoName == "" {
			repoName = "Unknown"
		}
		repoPathFromConfig := repo.RepoPath
		skillNames := repo.SkillNames

		if repoPathFromConfig == "" || len(skillNames) == 0 {
			writeLog("WARNING: repoPath or skillNames is empty, skipping")
			continue
		}

		for _, skill := range skillNames {
			writeLog(fmt.Sprintf("Processing config: Source=%s (Repo: %s), Skill=%s, Target=%s", repoPathFromConfig, repoName, skill, groupTargetPath))

			result := syncSkill(skill, repoPathFromConfig, groupTargetPath)

			if result {
				*successCount++
			} else {
				*failCount++
			}
		}
	}
}

// 同步技能
func syncSkill(skillName, repoPath, targetPath string) bool {
	// 检查源仓库路径是否存在
	if _, err := os.Stat(repoPath); os.IsNotExist(err) {
		writeLog(fmt.Sprintf("ERROR: Source repo path does not exist: %s", repoPath))
		return false
	}

	// 更新源仓库（git pull）
	writeLog(fmt.Sprintf("Updating source repo: %s", repoPath))
	if err := gitPull(repoPath); err != nil {
		writeLog("WARNING: git pull failed, continuing with existing code...")
		writeLog(fmt.Sprintf("Git error: %v", err))
	}

	// 检查并创建目标目录
	if _, err := os.Stat(targetPath); os.IsNotExist(err) {
		writeLog(fmt.Sprintf("Creating target directory: %s", targetPath))
		if err := os.MkdirAll(targetPath, 0755); err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to create target directory: %v", err))
			return false
		}
	}

	// 同步单个技能
	if skillName != "" {
		sourceSkillPath := filepath.Join(repoPath, skillName)

		// 检查指定的技能是否存在
		if _, err := os.Stat(sourceSkillPath); os.IsNotExist(err) {
			writeLog(fmt.Sprintf("ERROR: Specified skill does not exist: %s", sourceSkillPath))
			return false
		}

		destSkillPath := filepath.Join(targetPath, skillName)

		// 复制前打印源目录（验证）
		writeDirectoryTree(sourceSkillPath, "BEFORE COPY - Source directory")

		// 执行复制操作
		writeLog(fmt.Sprintf("Copying skill: %s", skillName))
		writeLog(fmt.Sprintf("Source path: %s", sourceSkillPath))
		writeLog(fmt.Sprintf("Target path: %s", destSkillPath))

		// 使用类似 rsync 的方法进行跨平台支持
		writeLog("---------- sync output start ----------")
		if err := copyDirectory(sourceSkillPath, destSkillPath); err != nil {
			writeLog(fmt.Sprintf("ERROR: Copy failed: %v", err))
			return false
		}
		writeLog("---------- sync output end ----------")

		// 复制后打印目标目录（验证）
		writeDirectoryTree(destSkillPath, "AFTER COPY - Target directory")

		writeLog(fmt.Sprintf("Successfully synced skill: %s", skillName))
	} else {
		// 同步所有技能
		writeLog("Syncing all skills...")

		// 从源仓库获取所有技能目录
		skills, err := getSkillDirectories(repoPath)
		if err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to list skills: %v", err))
			return false
		}

		// 复制前打印源目录概览
		writeDirectoryTree(repoPath, "BEFORE COPY - Source repo overview")

		for _, skill := range skills {
			sourceSkillPath := filepath.Join(repoPath, skill)
			destSkillPath := filepath.Join(targetPath, skill)

			// 打印单个技能源目录
			writeDirectoryTree(sourceSkillPath, fmt.Sprintf("BEFORE COPY - Skill [%s] source", skill))

			// 执行复制操作
			writeLog(fmt.Sprintf("Copying skill: %s", skill))
			writeLog(fmt.Sprintf("Source path: %s", sourceSkillPath))
			writeLog(fmt.Sprintf("Target path: %s", destSkillPath))

			writeLog("---------- sync output start ----------")
			if err := copyDirectory(sourceSkillPath, destSkillPath); err != nil {
				writeLog(fmt.Sprintf("ERROR: Copy failed: %v", err))
				continue
			}
			writeLog("---------- sync output end ----------")

			// 复制后打印目标目录
			writeDirectoryTree(destSkillPath, fmt.Sprintf("AFTER COPY - Skill [%s] target", skill))
		}

		// 复制后打印目标目录概览
		writeDirectoryTree(targetPath, "AFTER COPY - Target directory overview")

		writeLog(fmt.Sprintf("Successfully synced all skills, total: %d", len(skills)))
	}

	return true
}

// 执行git pull命令
func gitPull(repoPath string) error {
	var cmd *exec.Cmd
	if strings.Contains(strings.ToLower(os.Getenv("OS")), "windows") {
		cmd = exec.Command("cmd.exe", "/c", "git", "pull")
	} else {
		cmd = exec.Command("git", "pull")
	}

	cmd.Dir = repoPath
	_, err := cmd.CombinedOutput()
	return err
}

// 获取技能目录列表
func getSkillDirectories(repoPath string) ([]string, error) {
	files, err := os.ReadDir(repoPath)
	if err != nil {
		return nil, err
	}

	skills := []string{}
	for _, file := range files {
		if file.IsDir() {
			skills = append(skills, file.Name())
		}
	}

	return skills, nil
}

// 复制目录
func copyDirectory(source, destination string) error {
	// 创建目标目录（如果不存在）
	if _, err := os.Stat(destination); os.IsNotExist(err) {
		if err := os.MkdirAll(destination, 0755); err != nil {
			return err
		}
	}

	// 遍历源目录
	return filepath.Walk(source, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		// 计算相对路径
		relativePath, err := filepath.Rel(source, path)
		if err != nil {
			return err
		}

		// 如果是目录，创建目标目录
		if info.IsDir() {
			targetDir := filepath.Join(destination, relativePath)
			if _, err := os.Stat(targetDir); os.IsNotExist(err) {
				if err := os.MkdirAll(targetDir, 0755); err != nil {
					return err
				}
			}
			return nil
		}

		// 如果是文件，复制到目标目录
		targetFile := filepath.Join(destination, relativePath)

		// 检查目标文件是否存在，以及源文件是否更新
		copyFile := true
		if _, err := os.Stat(targetFile); err == nil {
			sourceInfo, err := os.Stat(path)
			if err == nil {
				targetInfo, err := os.Stat(targetFile)
				if err == nil {
					// 如果目标文件存在且修改时间比源文件新，不复制
					if targetInfo.ModTime().After(sourceInfo.ModTime()) {
						copyFile = false
					}
				}
			}
		}

		if copyFile {
			data, err := ioutil.ReadFile(path)
			if err != nil {
				return err
			}

			if err := ioutil.WriteFile(targetFile, data, 0644); err != nil {
				return err
			}

			fmt.Printf("Copied: %s\n", relativePath)
		}

		return nil
	})
}

// 列出目录内容
func writeDirectoryTree(path, title string) {
	writeLog(fmt.Sprintf("========== %s ==========", title))

	if _, err := os.Stat(path); os.IsNotExist(err) {
		writeLog(fmt.Sprintf("Directory does not exist: %s", path))
		writeLog("==========================================")
		return
	}

	writeLog(fmt.Sprintf("Directory path: %s", path))
	writeLog("Directory contents:")

	fileCount := 0
	dirCount := 0

	err := filepath.Walk(path, func(walkPath string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		if walkPath == path {
			return nil
		}

		relativePath, err := filepath.Rel(path, walkPath)
		if err != nil {
			return err
		}

		if info.IsDir() {
			fmt.Printf("  [DIR] %s\n", relativePath)
			dirCount++
		} else {
			size := info.Size()
			sizeKb := float64(size) / 1024.0
			fmt.Printf("  [FILE] %s (%.2f KB)\n", relativePath, sizeKb)
			fileCount++
		}

		return nil
	})

	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to walk directory: %v", err))
	}

	writeLog(fmt.Sprintf("Stats: %d directories, %d files", dirCount, fileCount))
	writeLog("==========================================")
}

// 写入日志
func writeLog(message string) {
	timestamp := time.Now().Format(dateFormat)
	fmt.Printf("[%s] %s\n", timestamp, message)
}
