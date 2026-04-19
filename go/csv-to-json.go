package main

import (
	"encoding/csv"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"time"
)

// 全局变量
var (
	dateFormat = "2006-01-02 15:04:05"
)

// 初始化配置结构
type InitConfig struct {
	Repos []RepoConfig `json:"repos"`
}

// 仓库配置结构
type RepoConfig struct {
	RepoName  string `json:"repoName,omitempty"`
	RepoUrl   string `json:"repoUrl"`
	LocalPath string `json:"localPath,omitempty"`
}

// 同步配置结构
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
	csvFile := "skills.csv"
	jsonFile := "sync-config.json"
	targetPath := "C:\\Users\\admin\\.trae-cn\\skills\\"
	outputType := "sync" // 默认值为 sync

	for i := 0; i < len(os.Args); i++ {
		if os.Args[i] == "-CsvFile" && i+1 < len(os.Args) {
			csvFile = os.Args[i+1]
		} else if os.Args[i] == "-JsonFile" && i+1 < len(os.Args) {
			jsonFile = os.Args[i+1]
		} else if os.Args[i] == "-TargetPath" && i+1 < len(os.Args) {
			targetPath = os.Args[i+1]
		} else if os.Args[i] == "-OutputType" && i+1 < len(os.Args) {
			outputType = os.Args[i+1]
		}
	}

	writeLog("Starting CSV to JSON conversion...")

	// 解析完整路径
	csvPath, err := filepath.Abs(csvFile)
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to get absolute path: %v", err))
		os.Exit(1)
	}

	// 检查CSV文件是否存在
	if _, err := os.Stat(csvPath); os.IsNotExist(err) {
		writeLog(fmt.Sprintf("ERROR: CSV file '%s' does not exist", csvPath))
		os.Exit(1)
	}

	writeLog(fmt.Sprintf("Reading CSV file: %s", csvPath))

	// 读取CSV文件
	csvData, err := readCsvFile(csvPath)
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to read CSV file: %v", err))
		os.Exit(1)
	}

	if len(csvData) == 0 {
		writeLog("WARNING: CSV file is empty")
		os.Exit(0)
	}

	writeLog(fmt.Sprintf("CSV file contains %d rows", len(csvData)))

	jsonPath, err := filepath.Abs(jsonFile)
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to get absolute path: %v", err))
		os.Exit(1)
	}

	if outputType == "init" {
		// 处理init模式：生成init-config.json
		writeLog("Processing in init mode: generating init-config.json")
		processInitMode(csvData, jsonPath)
	} else {
		// 处理sync模式：生成sync-config.json
		writeLog("Processing in sync mode: generating sync-config.json")
		processSyncMode(csvData, jsonPath, targetPath)
	}

	writeLog("Processing completed")
}

// 读取CSV文件
func readCsvFile(csvPath string) ([]map[string]string, error) {
	file, err := os.Open(csvPath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	reader := csv.NewReader(file)
	reader.TrimLeadingSpace = true

	// 读取表头
	headers, err := reader.Read()
	if err != nil {
		return nil, err
	}

	// 处理UTF-8 BOM
	if len(headers) > 0 && strings.HasPrefix(headers[0], "\ufeff") {
		headers[0] = strings.TrimPrefix(headers[0], "\ufeff")
	}

	// 读取数据
	csvData := []map[string]string{}
	for {
		row, err := reader.Read()
		if err != nil {
			break
		}

		rowData := make(map[string]string)
		for i := 0; i < len(headers) && i < len(row); i++ {
			rowData[headers[i]] = strings.TrimSpace(row[i])
		}
		csvData = append(csvData, rowData)
	}

	return csvData, nil
}

// 处理init模式
func processInitMode(csvData []map[string]string, jsonPath string) {
	// 从CSV数据中提取唯一的仓库信息
	repoMap := make(map[string]map[string]string)
	for _, row := range csvData {
		repoPath := strings.TrimSpace(row["repoPath"])
		repoName := strings.TrimSpace(row["repoName"])
		localPath := strings.TrimSpace(row["localPath"])
		repoUrl := strings.TrimSpace(row["repoUrl"])

		if repoPath == "" {
			writeLog("Skipping empty repoPath")
			continue
		}

		if _, ok := repoMap[repoPath]; !ok {
			repoInfo := make(map[string]string)
			if repoName != "" {
				repoInfo["repoName"] = repoName
			}
			if localPath != "" {
				repoInfo["localPath"] = localPath
			}
			if repoUrl != "" {
				repoInfo["repoUrl"] = repoUrl
			}
			repoMap[repoPath] = repoInfo
		}
	}

	// 生成init-config.json格式
	initConfig := InitConfig{}
	initConfig.Repos = []RepoConfig{}

	for repoPath, repoInfo := range repoMap {
		repoUrl := repoInfo["repoUrl"]

		// 如果CSV中没有提供repoUrl，则从repoPath生成
		if repoUrl == "" {
			// 从repoPath提取GitHub仓库信息
			parts := strings.Split(repoPath, "\\")
			if len(parts) >= 2 {
				owner := ""
				if len(parts) >= 3 {
					owner = parts[len(parts)-3]
				}
				repo := parts[len(parts)-2]
				repoUrl = fmt.Sprintf("https://github.com/%s/%s.git", owner, repo)
			}
		}

		repoConfig := RepoConfig{
			RepoName:  repoInfo["repoName"],
			RepoUrl:   repoUrl,
			LocalPath: repoInfo["localPath"],
		}
		initConfig.Repos = append(initConfig.Repos, repoConfig)
	}

	// 保存到文件
	data, err := json.MarshalIndent(initConfig, "", "  ")
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to marshal JSON: %v", err))
		os.Exit(1)
	}

	if err := ioutil.WriteFile(jsonPath, data, 0644); err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to save JSON file: %v", err))
		os.Exit(1)
	}

	writeLog(fmt.Sprintf("Init config saved to: %s", jsonPath))
	writeLog("Config content:")
	fmt.Println(string(data))
}

// 处理sync模式
func processSyncMode(csvData []map[string]string, jsonPath string, targetPath string) {
	// 读取现有JSON文件
	syncConfig := SyncConfig{}
	syncConfig.TargetPath = targetPath
	syncConfig.Repos = []SyncRepoConfig{}

	if _, err := os.Stat(jsonPath); err == nil {
		writeLog(fmt.Sprintf("Reading existing JSON file: %s", jsonPath))
		data, err := ioutil.ReadFile(jsonPath)
		if err != nil {
			writeLog(fmt.Sprintf("WARNING: Failed to read existing JSON file: %v", err))
		} else {
			if err := json.Unmarshal(data, &syncConfig); err != nil {
				writeLog("WARNING: Invalid JSON file, creating new configuration")
				syncConfig = SyncConfig{}
				syncConfig.TargetPath = targetPath
				syncConfig.Repos = []SyncRepoConfig{}
			}
		}
	} else {
		writeLog("JSON file does not exist, creating new configuration")
	}

	// 创建repo map
	repoMap := make(map[string][]string)
	repoNameMap := make(map[string]string)
	for _, repo := range syncConfig.Repos {
		repoMap[repo.RepoPath] = repo.SkillNames
		repoNameMap[repo.RepoPath] = repo.RepoName
	}

	modified := false

	// 处理CSV数据
	for _, row := range csvData {
		repoPath := strings.TrimSpace(row["repoPath"])
		skillName := strings.TrimSpace(row["skillName"])
		repoName := strings.TrimSpace(row["repoName"])

		if repoPath == "" || skillName == "" {
			writeLog(fmt.Sprintf("Skipping empty row: repoPath=%s, skillName=%s", repoPath, skillName))
			continue
		}

		if _, ok := repoMap[repoPath]; ok {
			skillNames := repoMap[repoPath]
			found := false
			for _, s := range skillNames {
				if s == skillName {
					found = true
					break
				}
			}
			if !found {
				writeLog(fmt.Sprintf("Adding skill '%s' to existing repo: %s", skillName, repoPath))
				skillNames = append(skillNames, skillName)
				repoMap[repoPath] = skillNames
				modified = true
			} else {
				writeLog(fmt.Sprintf("Skill '%s' already exists in repo: %s", skillName, repoPath))
			}
		} else {
			writeLog(fmt.Sprintf("Creating new repo config: %s, adding skill: %s", repoPath, skillName))
			skillNames := []string{skillName}
			repoMap[repoPath] = skillNames
			if repoName != "" {
				repoNameMap[repoPath] = repoName
			}
			modified = true
		}
	}

	// 更新repos
	newRepos := []SyncRepoConfig{}
	for repoPath, skillNames := range repoMap {
		repo := SyncRepoConfig{
			RepoPath:   repoPath,
			RepoName:   repoNameMap[repoPath],
			SkillNames: skillNames,
		}
		newRepos = append(newRepos, repo)
	}
	syncConfig.Repos = newRepos

	// 保存JSON
	if modified {
		data, err := json.MarshalIndent(syncConfig, "", "  ")
		if err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to marshal JSON: %v", err))
			os.Exit(1)
		}

		if err := ioutil.WriteFile(jsonPath, data, 0644); err != nil {
			writeLog(fmt.Sprintf("ERROR: Failed to save JSON file: %v", err))
			os.Exit(1)
		}

		writeLog(fmt.Sprintf("JSON config saved to: %s", jsonPath))
		writeLog("Config content:")
		fmt.Println(string(data))
	} else {
		writeLog("No updates needed")
	}
}

// 写入日志
func writeLog(message string) {
	timestamp := time.Now().Format(dateFormat)
	fmt.Printf("[%s] %s\n", timestamp, message)
}
