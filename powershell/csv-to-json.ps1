param(
    [string]$CsvFile = ".\skills.csv",
    [string]$JsonFile = ".\sync-config.json",
    [string]$TargetPath = "C:\Users\admin\.trae-cn\skills\",
    [string]$OutputType = "sync"  # sync or init
)

function Write-Log {
    param([string]$Message)
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
}

function Get-FileEncoding {
    param([string]$FilePath)
    
    try {
        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
            return "UTF8"
        }
        if ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
            return "Unicode"
        }
        if ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFE -and $bytes[1] -eq 0xFF) {
            return "BigEndianUnicode"
        }
        return "Default"
    }
    catch {
        return "Locked"
    }
}

function ConvertTo-Utf8 {
    param([string]$FilePath)
    
    $encoding = Get-FileEncoding -FilePath $FilePath
    Write-Log "Detected encoding: $encoding"
    
    if ($encoding -eq "UTF8") {
        Write-Log "File is already UTF-8, no conversion needed"
        return $true
    }
    
    if ($encoding -eq "Locked") {
        Write-Log "WARNING: File is locked by another process, skipping encoding conversion"
        Write-Log "Please close the file in other applications and try again if encoding issues occur"
        return $true
    }
    
    Write-Log "Converting file from $encoding to UTF-8..."
    
    try {
        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        $sourceEncoding = $null
        
        if ($encoding -eq "Default") {
            # 尝试使用常见编码进行解码
            $encodingsToTry = @(
                [System.Text.Encoding]::Default,
                [System.Text.Encoding]::GetEncoding("GBK"),
                [System.Text.Encoding]::GetEncoding("GB2312"),
                [System.Text.Encoding]::UTF8
            )
            
            $content = $null
            foreach ($enc in $encodingsToTry) {
                try {
                    $content = $enc.GetString($bytes)
                    # 简单验证内容是否有效
                    if ($content -match "[a-zA-Z0-9]" -or $content.Length -gt 0) {
                        $sourceEncoding = $enc
                        Write-Log "Successfully decoded with encoding: $($enc.EncodingName)"
                        break
                    }
                } catch {
                    # 忽略解码错误，尝试下一个编码
                }
            }
            
            if (-not $sourceEncoding) {
                # 如果所有编码都失败，使用默认编码
                $sourceEncoding = [System.Text.Encoding]::Default
                Write-Log "Using default encoding as fallback"
            }
        } else {
            $sourceEncoding = [System.Text.Encoding]::$encoding
        }
        
        $content = $sourceEncoding.GetString($bytes)
        $utf8Bytes = [System.Text.Encoding]::UTF8.GetBytes($content)
        [System.IO.File]::WriteAllBytes($FilePath, $utf8Bytes)
        Write-Log "Conversion completed successfully"
        return $true
    } catch {
        Write-Log "ERROR: Failed to convert encoding: $_"
        return $false
    }
}

$CsvFullPath = Resolve-Path $CsvFile -ErrorAction SilentlyContinue
if (-not $CsvFullPath) {
    $CsvFullPath = $CsvFile
}

if (-not (Test-Path $CsvFullPath)) {
    Write-Log "ERROR: CSV file '$CsvFullPath' does not exist"
    exit 1
}

$encodingResult = ConvertTo-Utf8 -FilePath $CsvFullPath
if (-not $encodingResult) {
    Write-Log "ERROR: Failed to ensure UTF-8 encoding for CSV file"
    exit 1
}

Write-Log "Reading CSV file: $CsvFullPath"
$csvData = Import-Csv -Path $CsvFullPath -Encoding UTF8

if (-not $csvData) {
    Write-Log "WARNING: CSV file is empty"
    exit 0
}

Write-Log "CSV file contains $($csvData.Count) rows"

$jsonConfig = $null
$jsonFullPath = $JsonFile

if (Test-Path $JsonFile) {
    $jsonFullPath = Resolve-Path $JsonFile
    Write-Log "Reading existing JSON file: $jsonFullPath"
    $jsonContent = Get-Content -Path $jsonFullPath -Raw
    $parsedJson = $jsonContent | ConvertFrom-Json
    
    if ($parsedJson -is [System.Array]) {
        $jsonConfig = @($parsedJson)
    } else {
        $jsonConfig = @($parsedJson)
    }
} else {
    Write-Log "JSON file does not exist, creating new configuration"
    $jsonConfig = @()
}

$targetGroup = $jsonConfig | Where-Object { $_.targetPath -eq $TargetPath }

if (-not $targetGroup) {
    Write-Log "Creating new target group: $TargetPath"
    $targetGroup = [PSCustomObject]@{
        targetPath = $TargetPath
        repos = @()
    }
    $jsonConfig = @($targetGroup)
}

if ($OutputType -eq "init") {
    # 处理 init 模式：生成 init-config.json
    Write-Log "Processing in init mode: generating init-config.json"
    
    # 从 CSV 数据中提取唯一的仓库信息
    $repoMap = @{}
    foreach ($row in $csvData) {
        $repoPath = $row.repoPath
        $repoName = $row.repoName
        $localPath = $row.localPath
        $repoUrl = $row.repoUrl
        
        if ([string]::IsNullOrWhiteSpace($repoPath)) {
            Write-Log "Skipping empty repoPath"
            continue
        }
        
        if (-not $repoMap.ContainsKey($repoPath)) {
            $repoMap[$repoPath] = @{
                repoName = $repoName
                localPath = $localPath
                repoUrl = $repoUrl
            }
        }
    }
    
    # 生成 init-config.json 格式
    $initConfig = @{
        repos = @()
    }
    
    foreach ($key in $repoMap.Keys) {
        $repoInfo = $repoMap[$key]
        $repoUrl = $repoInfo.repoUrl
        
        # 如果 CSV 中没有提供 repoUrl，则从 repoPath 生成
        if ([string]::IsNullOrWhiteSpace($repoUrl)) {
            $repoUrl = "https://github.com/$(($key -split '\\')[-3])/$(($key -split '\\')[-2]).git"
        }
        
        $repoConfig = [PSCustomObject]@{
            repoName = $repoInfo.repoName
            repoUrl = $repoUrl
            localPath = $repoInfo.localPath
        }
        $initConfig.repos += $repoConfig
    }
    
    # 保存到文件
    $jsonOutput = $initConfig | ConvertTo-Json -Depth 10
    $jsonOutput | Out-File -FilePath $JsonFile -Encoding UTF8
    Write-Log "Init config saved to: $JsonFile"
    Write-Log "Config content:"
    Write-Host $jsonOutput
} else {
    # 处理 sync 模式：生成 sync-config.json
    Write-Log "Processing in sync mode: generating sync-config.json"
    
    $repoMap = @{}
    $repoNameMap = @{}
    foreach ($repo in $targetGroup.repos) {
        $repoMap[$repo.repoPath] = @($repo.skillNames)
        if ($repo.repoName) {
            $repoNameMap[$repo.repoPath] = $repo.repoName
        }
    }
    
    $modified = $false
    
    foreach ($row in $csvData) {
        $repoPath = $row.repoPath
        $skillName = $row.skillName
        $repoName = $row.repoName
        
        if ([string]::IsNullOrWhiteSpace($repoPath) -or [string]::IsNullOrWhiteSpace($skillName)) {
            Write-Log "Skipping empty row: repoPath=$repoPath, skillName=$skillName"
            continue
        }
        
        if ($repoMap.ContainsKey($repoPath)) {
            if ($repoMap[$repoPath] -notcontains $skillName) {
                Write-Log "Adding skill '$skillName' to existing repo: $repoPath"
                $repoMap[$repoPath] += $skillName
                $modified = $true
            } else {
                Write-Log "Skill '$skillName' already exists in repo: $repoPath"
            }
            
            # Keep repoName unchanged from existing config
            # Do not update repoName from CSV
        } else {
            Write-Log "Creating new repo config: $repoPath, adding skill: $skillName"
            $repoMap[$repoPath] = @($skillName)
            if ($repoName) {
                $repoNameMap[$repoPath] = $repoName
            }
            $modified = $true
        }
    }
    
    $newRepos = @()
    foreach ($key in $repoMap.Keys) {
        $repoConfig = [PSCustomObject]@{
            repoPath = $key
            skillNames = @($repoMap[$key])
        }
        
        if ($repoNameMap.ContainsKey($key)) {
            $repoConfig | Add-Member -NotePropertyName "repoName" -NotePropertyValue $repoNameMap[$key]
        }
        
        $newRepos += $repoConfig
    }
    
    $targetGroup.repos = $newRepos
    
    if ($modified) {
        $jsonOutput = $jsonConfig | ConvertTo-Json -Depth 10
        $jsonOutput | Out-File -FilePath $JsonFile -Encoding UTF8
        Write-Log "JSON config saved to: $JsonFile"
        Write-Log "Config content:"
        Write-Host $jsonOutput
    } else {
        Write-Log "No updates needed"
    }
}

Write-Log "Processing completed"
