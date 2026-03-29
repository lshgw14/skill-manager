#!/usr/bin/env python3
"""
Skill Sync Script

This script syncs skill files from Git repositories to a target directory.
Supports syncing single skill or all skills, and batch sync via config file.
"""

import os
import sys
import json
import argparse
import subprocess
from datetime import datetime


def write_log(message):
    """Write log message with timestamp"""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] {message}")


def write_directory_tree(path, title):
    """Print directory tree for verification"""
    write_log(f"========== {title} ==========")
    
    if not os.path.exists(path):
        write_log(f"Directory does not exist: {path}")
        write_log("==========================================")
        return
    
    write_log(f"Directory path: {path}")
    write_log("Directory contents:")
    
    items = []
    for root, dirs, files in os.walk(path):
        for d in dirs:
            items.append((os.path.join(root, d), 'dir'))
        for f in files:
            items.append((os.path.join(root, f), 'file'))
    
    file_count = 0
    dir_count = 0
    
    for item_path, item_type in items:
        relative_path = os.path.relpath(item_path, path).replace('\\', '/')
        if item_type == 'dir':
            print(f"  [DIR] {relative_path}")
            dir_count += 1
        else:
            size_kb = os.path.getsize(item_path) / 1024
            print(f"  [FILE] {relative_path} ({size_kb:.2f} KB)")
            file_count += 1
    
    write_log(f"Stats: {dir_count} directories, {file_count} files")
    write_log("==========================================")


def sync_skill(skill_name, repo_path, target_path):
    """Sync skill function"""
    # Check if source repo path exists
    if not os.path.exists(repo_path):
        write_log(f"ERROR: Source repo path does not exist: {repo_path}")
        return False
    
    # Update source repo (git pull)
    write_log(f"Updating source repo: {repo_path}")
    try:
        result = subprocess.run(['git', 'pull'], cwd=repo_path, capture_output=True, text=True)
        if result.returncode != 0:
            write_log(f"WARNING: git pull failed, continuing with existing code...")
            write_log(f"Git output: {result.stderr}")
    except Exception as e:
        write_log(f"WARNING: git pull error: {e}")
    
    # Check and create target directory
    if not os.path.exists(target_path):
        write_log(f"Creating target directory: {target_path}")
        os.makedirs(target_path, exist_ok=True)
    
    # Sync single skill
    if skill_name:
        source_skill_path = os.path.join(repo_path, skill_name)
        
        # Check if specified skill exists
        if not os.path.exists(source_skill_path):
            write_log(f"ERROR: Specified skill does not exist: {source_skill_path}")
            return False
        
        dest_skill_path = os.path.join(target_path, skill_name)
        
        # Print source directory before copy (verification)
        write_directory_tree(source_skill_path, "BEFORE COPY - Source directory")
        
        # Execute copy operation
        write_log(f"Copying skill: {skill_name}")
        write_log(f"Source path: {source_skill_path}")
        write_log(f"Target path: {dest_skill_path}")
        
        # Use rsync-like approach for cross-platform support
        write_log("---------- sync output start ----------")
        try:
            # Create target directory if not exists
            os.makedirs(dest_skill_path, exist_ok=True)
            
            # Walk through source directory
            for root, dirs, files in os.walk(source_skill_path):
                # Create corresponding directory in target
                rel_path = os.path.relpath(root, source_skill_path)
                target_dir = os.path.join(dest_skill_path, rel_path)
                os.makedirs(target_dir, exist_ok=True)
                
                # Copy files
                for file in files:
                    source_file = os.path.join(root, file)
                    target_file = os.path.join(target_dir, file)
                    
                    # Copy file if it's different or doesn't exist
                    if not os.path.exists(target_file) or os.path.getmtime(source_file) > os.path.getmtime(target_file):
                        import shutil
                        shutil.copy2(source_file, target_file)
                        print(f"Copied: {rel_path}/{file}")
        except Exception as e:
            write_log(f"ERROR: Copy failed: {e}")
            return False
        
        write_log("---------- sync output end ----------")
        
        # Print target directory after copy (verification)
        write_directory_tree(dest_skill_path, "AFTER COPY - Target directory")
        
        write_log(f"Successfully synced skill: {skill_name}")
    # Sync all skills
    else:
        write_log("Syncing all skills...")
        
        # Get all skill directories from source repo
        skills = []
        for item in os.listdir(repo_path):
            item_path = os.path.join(repo_path, item)
            if os.path.isdir(item_path):
                skills.append(item)
        
        # Print source directory overview before copy
        write_directory_tree(repo_path, "BEFORE COPY - Source repo overview")
        
        for skill in skills:
            source_skill_path = os.path.join(repo_path, skill)
            dest_skill_path = os.path.join(target_path, skill)
            
            # Print single skill source directory
            write_directory_tree(source_skill_path, f"BEFORE COPY - Skill [{skill}] source")
            
            # Execute copy operation
            write_log(f"Copying skill: {skill}")
            write_log(f"Source path: {source_skill_path}")
            write_log(f"Target path: {dest_skill_path}")
            
            write_log("---------- sync output start ----------")
            try:
                # Create target directory if not exists
                os.makedirs(dest_skill_path, exist_ok=True)
                
                # Walk through source directory
                for root, dirs, files in os.walk(source_skill_path):
                    # Create corresponding directory in target
                    rel_path = os.path.relpath(root, source_skill_path)
                    target_dir = os.path.join(dest_skill_path, rel_path)
                    os.makedirs(target_dir, exist_ok=True)
                    
                    # Copy files
                    for file in files:
                        source_file = os.path.join(root, file)
                        target_file = os.path.join(target_dir, file)
                        
                        # Copy file if it's different or doesn't exist
                        if not os.path.exists(target_file) or os.path.getmtime(source_file) > os.path.getmtime(target_file):
                            import shutil
                            shutil.copy2(source_file, target_file)
                            print(f"Copied: {rel_path}/{file}")
            except Exception as e:
                write_log(f"ERROR: Copy failed: {e}")
                continue
            
            write_log("---------- sync output end ----------")
            
            # Print target directory after copy
            write_directory_tree(dest_skill_path, f"AFTER COPY - Skill [{skill}] target")
        
        # Print target directory overview after copy
        write_directory_tree(target_path, "AFTER COPY - Target directory overview")
        
        write_log(f"Successfully synced all skills, total: {len(skills)}")
    
    return True


def main():
    """Main function"""
    parser = argparse.ArgumentParser(description='Sync skills from Git repositories')
    parser.add_argument('-SkillName', help='Skill name to sync')
    parser.add_argument('-RepoPath', default='E:\\develop\\code\\open-source\\github\\skills\\mattpocock\\skills', help='Source repo path')
    parser.add_argument('-TargetPath', default='C:\\Users\\admin\\.trae-cn\\skills\\', help='Target directory path')
    parser.add_argument('-ConfigFile', help='Config file path (JSON format)')
    args = parser.parse_args()
    
    skill_name = args.SkillName
    repo_path = args.RepoPath
    target_path = args.TargetPath
    config_file = args.ConfigFile
    
    write_log("Starting skill sync...")
    
    # If config file is specified, read sync tasks from config file
    if config_file:
        # Check if config file exists
        if not os.path.exists(config_file):
            write_log(f"ERROR: Config file does not exist: {config_file}")
            sys.exit(1)
        
        write_log(f"Reading config file: {config_file}")
        
        # Read and parse JSON config file
        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                configs = json.load(f)
        except json.JSONDecodeError as e:
            write_log(f"ERROR: Invalid JSON config file: {e}")
            sys.exit(1)
        
        # Initialize counters
        success_count = 0
        fail_count = 0
        
        # Iterate config items and execute sync
        if isinstance(configs, list):
            config_list = configs
        else:
            config_list = [configs]
        
        for target_group in config_list:
            target_path = target_group.get('targetPath')
            if not target_path:
                continue
            
            repos = target_group.get('repos', [])
            for repo in repos:
                repo_path = repo.get('repoPath')
                skill_names = repo.get('skillNames', [])
                
                if isinstance(skill_names, str):
                    skill_names = [skill_names]
                
                for skill_name in skill_names:
                    write_log(f"Processing config: Source={repo_path}, Skill={skill_name}, Target={target_path}")
                    
                    result = sync_skill(skill_name, repo_path, target_path)
                    
                    if result:
                        success_count += 1
                    else:
                        fail_count += 1
        
        write_log(f"Config file processing completed. Success: {success_count}, Failed: {fail_count}")
    # No config file specified, use command line parameters to execute sync
    else:
        sync_skill(skill_name, repo_path, target_path)
    
    write_log("Sync completed!")


if __name__ == '__main__':
    main()
