<#
.SYNOPSIS
    Skill Sync Script - Sync skill files from source repo to target directory

.DESCRIPTION
    This script syncs skill files from a Git repo to a target directory.
    Supports syncing single skill or all skills, supports batch sync via config file.

.PARAMETER SkillName
    Optional parameter, specify the skill name to sync.
    If not specified, sync all skills from source repo.

.PARAMETER RepoPath
    Source repo path, default: "E:\develop\code\open-source\github\skills\mattpocock\skills"

.PARAMETER TargetPath
    Target directory path, default: "C:\Users\admin\.trae-cn\skills\"

.PARAMETER ConfigFile
    Optional parameter, config file path (JSON format).
    If specified, read sync tasks from config file.

.EXAMPLE
    # Sync all skills
    .\sync-skills.ps1

.EXAMPLE
    # Sync specified skill
    .\sync-skills.ps1 -SkillName "skill-name"

.EXAMPLE
    # Sync using config file
    .\sync-skills.ps1 -ConfigFile "config.json"

.EXAMPLE
    # Specify source and target paths
    .\sync-skills.ps1 -RepoPath "D:\skills" -TargetPath "C:\Users\admin\.trae-cn\skills\"
#>

param(
    [string]$SkillName,
    [string]$RepoPath = "E:\develop\code\open-source\github\skills\mattpocock\skills",
    [string]$TargetPath = "C:\Users\admin\.trae-cn\skills\",
    [string]$ConfigFile
)

# Write log message with timestamp
function Write-Log {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp] $Message"
}

# Print directory tree for verification
function Write-DirectoryTree {
    param(
        [string]$Path,
        [string]$Title
    )
    
    Write-Log "========== $Title =========="
    
    if (-not (Test-Path $Path)) {
        Write-Log "Directory does not exist: $Path"
        Write-Log "=========================================="
        return
    }
    
    Write-Log "Directory path: $Path"
    Write-Log "Directory contents:"
    
    $items = Get-ChildItem -Path $Path -Recurse
    
    foreach ($item in $items) {
        $relativePath = $item.FullName.Substring($Path.Length).TrimStart('\', '/')
        if ($item.PSIsContainer) {
            Write-Host "  [DIR] $relativePath"
        }
        else {
            $sizeKB = [math]::Round($item.Length / 1KB, 2)
            Write-Host "  [FILE] $relativePath ($sizeKB KB)"
        }
    }
    
    $fileCount = ($items | Where-Object { -not $_.PSIsContainer }).Count
    $dirCount = ($items | Where-Object { $_.PSIsContainer }).Count
    
    Write-Log "Stats: $dirCount directories, $fileCount files"
    Write-Log "=========================================="
}

# Sync skill function
function Sync-Skill {
    param(
        [string]$SkillName,
        [string]$RepoPath,
        [string]$TargetPath
    )

    # Check if source repo path exists
    if (-not (Test-Path $RepoPath)) {
        Write-Log "ERROR: Source repo path does not exist: $RepoPath"
        return $false
    }

    # Update source repo (git pull)
    Write-Log "Updating source repo: $RepoPath"
    Push-Location $RepoPath
    try {
        git pull
        if ($LASTEXITCODE -ne 0) {
            Write-Log "WARNING: git pull failed, continuing with existing code..."
        }
    }
    catch {
        Write-Log "WARNING: git pull error: $_"
    }
    finally {
        Pop-Location
    }

    # Check and create target directory
    if (-not (Test-Path $TargetPath)) {
        Write-Log "Creating target directory: $TargetPath"
        New-Item -ItemType Directory -Path $TargetPath -Force | Out-Null
    }

    # Sync single skill
    if ($SkillName) {
        $sourceSkillPath = Join-Path $RepoPath $SkillName
        
        # Check if specified skill exists
        if (-not (Test-Path $sourceSkillPath)) {
            Write-Log "ERROR: Specified skill does not exist: $sourceSkillPath"
            return $false
        }
        
        $destSkillPath = Join-Path $TargetPath $SkillName
        
        # Print source directory before copy (verification)
        Write-DirectoryTree -Path $sourceSkillPath -Title "BEFORE COPY - Source directory"
        
        # Execute copy operation
        Write-Log "Copying skill: $SkillName"
        Write-Log "Source path: $sourceSkillPath"
        Write-Log "Target path: $destSkillPath"
        Write-Log "---------- robocopy output start ----------"
        robocopy $sourceSkillPath $destSkillPath /E /MIR /R:1 /W:1
        Write-Log "---------- robocopy output end ----------"
        
        # Check robocopy exit code
        # Exit code explanation:
        # 0-7: Success (including no files to copy, files copied, etc.)
        # 8+: Error
        if ($LASTEXITCODE -ge 8) {
            Write-Log "ERROR: robocopy failed with exit code: $LASTEXITCODE"
            return $false
        }
        
        # Print target directory after copy (verification)
        Write-DirectoryTree -Path $destSkillPath -Title "AFTER COPY - Target directory"
        
        Write-Log "Successfully synced skill: $SkillName"
    }
    # Sync all skills
    else {
        Write-Log "Syncing all skills..."
        
        # Get all skill directories from source repo
        $skills = Get-ChildItem -Path $RepoPath -Directory
        
        # Print source directory overview before copy
        Write-DirectoryTree -Path $RepoPath -Title "BEFORE COPY - Source repo overview"
        
        foreach ($skill in $skills) {
            $destSkillPath = Join-Path $TargetPath $skill.Name
            
            # Print single skill source directory
            Write-DirectoryTree -Path $skill.FullName -Title "BEFORE COPY - Skill [$($skill.Name)] source"
            
            # Execute copy operation
            Write-Log "Copying skill: $($skill.Name)"
            Write-Log "Source path: $($skill.FullName)"
            Write-Log "Target path: $destSkillPath"
            Write-Log "---------- robocopy output start ----------"
            robocopy $skill.FullName $destSkillPath /E /MIR /R:1 /W:1
            Write-Log "---------- robocopy output end ----------"
            
            # Print target directory after copy
            Write-DirectoryTree -Path $destSkillPath -Title "AFTER COPY - Skill [$($skill.Name)] target"
        }
        
        # Print target directory overview after copy
        Write-DirectoryTree -Path $TargetPath -Title "AFTER COPY - Target directory overview"
        
        Write-Log "Successfully synced all skills, total: $($skills.Count)"
    }

    return $true
}

# ==================== Main Entry ====================

Write-Log "Starting skill sync..."

# If config file is specified, read sync tasks from config file
if ($ConfigFile) {
    # Check if config file exists
    if (-not (Test-Path $ConfigFile)) {
        Write-Log "ERROR: Config file does not exist: $ConfigFile"
        exit 1
    }

    Write-Log "Reading config file: $ConfigFile"
    
    # Read and parse JSON config file
    $configs = Get-Content -Path $ConfigFile | ConvertFrom-Json

    # Initialize counters
    $successCount = 0
    $failCount = 0

    # Iterate config items and execute sync
    foreach ($targetGroup in $configs) {
        $targetPath = $targetGroup.targetPath
        
        foreach ($repo in $targetGroup.repos) {
            $repoPath = $repo.repoPath
            $skillNames = $repo.skillNames
            
            if ($skillNames -is [string]) {
                $skillNames = @($skillNames)
            }
            
            foreach ($skillName in $skillNames) {
                Write-Log "Processing config: Source=$repoPath, Skill=$skillName, Target=$targetPath"
                
                $result = Sync-Skill -SkillName $skillName -RepoPath $repoPath -TargetPath $targetPath
                
                if ($result) {
                    $successCount++
                }
                else {
                    $failCount++
                }
            }
        }
    }

    Write-Log "Config file processing completed. Success: $successCount, Failed: $failCount"
}
# No config file specified, use command line parameters to execute sync
else {
    Sync-Skill -SkillName $SkillName -RepoPath $RepoPath -TargetPath $TargetPath
}

Write-Log "Sync completed!"
