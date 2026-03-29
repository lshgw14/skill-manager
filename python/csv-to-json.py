#!/usr/bin/env python3
"""
CSV to JSON Config Converter

This script converts a CSV file containing skill information to a JSON configuration file.
It automatically detects and converts file encoding to UTF-8.
"""

import os
import sys
import csv
import json
import argparse
from datetime import datetime


def write_log(message):
    """Write log message with timestamp"""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] {message}")


def get_file_encoding(file_path):
    """Detect file encoding"""
    try:
        with open(file_path, 'rb') as f:
            bytes_data = f.read(4)
        
        if len(bytes_data) >= 3 and bytes_data[0] == 0xEF and bytes_data[1] == 0xBB and bytes_data[2] == 0xBF:
            return "UTF8"
        if len(bytes_data) >= 2 and bytes_data[0] == 0xFF and bytes_data[1] == 0xFE:
            return "Unicode"
        if len(bytes_data) >= 2 and bytes_data[0] == 0xFE and bytes_data[1] == 0xFF:
            return "BigEndianUnicode"
        return "Default"
    except Exception:
        return "Locked"


def convert_to_utf8(file_path):
    """Convert file to UTF-8 encoding"""
    encoding = get_file_encoding(file_path)
    write_log(f"Detected encoding: {encoding}")
    
    if encoding == "UTF8":
        write_log("File is already UTF-8, no conversion needed")
        return True
    
    if encoding == "Locked":
        write_log("WARNING: File is locked by another process, skipping encoding conversion")
        write_log("Please close the file in other applications and try again if encoding issues occur")
        return True
    
    write_log(f"Converting file from {encoding} to UTF-8...")
    
    try:
        # Read with detected encoding
        with open(file_path, 'r', encoding=encoding.lower() if encoding != "Default" else 'utf-8', errors='replace') as f:
            content = f.read()
        
        # Write with UTF-8
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        write_log("Conversion completed successfully")
        return True
    except Exception as e:
        write_log(f"ERROR: Failed to convert encoding: {e}")
        return False


def main():
    """Main function"""
    parser = argparse.ArgumentParser(description='Convert CSV to JSON config file')
    parser.add_argument('-CsvFile', default='skills.csv', help='CSV file path')
    parser.add_argument('-JsonFile', default='sync-config.json', help='JSON file path')
    parser.add_argument('-TargetPath', default='C:\\Users\\admin\\.trae-cn\\skills\\', help='Target path for skills')
    args = parser.parse_args()
    
    csv_file = args.CsvFile
    json_file = args.JsonFile
    target_path = args.TargetPath
    
    # Resolve full path
    csv_full_path = os.path.abspath(csv_file)
    
    if not os.path.exists(csv_full_path):
        write_log(f"ERROR: CSV file '{csv_full_path}' does not exist")
        sys.exit(1)
    
    # Convert to UTF-8
    if not convert_to_utf8(csv_full_path):
        write_log("ERROR: Failed to ensure UTF-8 encoding for CSV file")
        sys.exit(1)
    
    write_log(f"Reading CSV file: {csv_full_path}")
    
    # Read CSV
    csv_data = []
    with open(csv_full_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            csv_data.append(row)
    
    if not csv_data:
        write_log("WARNING: CSV file is empty")
        sys.exit(0)
    
    write_log(f"CSV file contains {len(csv_data)} rows")
    
    # Read existing JSON
    json_config = []
    if os.path.exists(json_file):
        write_log(f"Reading existing JSON file: {json_file}")
        with open(json_file, 'r', encoding='utf-8') as f:
            try:
                parsed_json = json.load(f)
                if isinstance(parsed_json, list):
                    json_config = parsed_json
                else:
                    json_config = [parsed_json]
            except json.JSONDecodeError:
                write_log("WARNING: Invalid JSON file, creating new configuration")
                json_config = []
    else:
        write_log("JSON file does not exist, creating new configuration")
    
    # Find or create target group
    target_group = None
    for group in json_config:
        if group.get('targetPath') == target_path:
            target_group = group
            break
    
    if not target_group:
        write_log(f"Creating new target group: {target_path}")
        target_group = {
            'targetPath': target_path,
            'repos': []
        }
        json_config = [target_group]
    
    # Create repo map
    repo_map = {}
    for repo in target_group.get('repos', []):
        repo_map[repo['repoPath']] = repo['skillNames']
    
    modified = False
    
    # Process CSV data
    for row in csv_data:
        repo_path = row.get('repoPath')
        skill_name = row.get('skillName')
        
        if not repo_path or not skill_name:
            write_log(f"Skipping empty row: repoPath={repo_path}, skillName={skill_name}")
            continue
        
        if repo_path in repo_map:
            if skill_name not in repo_map[repo_path]:
                write_log(f"Adding skill '{skill_name}' to existing repo: {repo_path}")
                repo_map[repo_path].append(skill_name)
                modified = True
            else:
                write_log(f"Skill '{skill_name}' already exists in repo: {repo_path}")
        else:
            write_log(f"Creating new repo config: {repo_path}, adding skill: {skill_name}")
            repo_map[repo_path] = [skill_name]
            modified = True
    
    # Update repos
    new_repos = []
    for key, value in repo_map.items():
        new_repos.append({
            'repoPath': key,
            'skillNames': value
        })
    
    target_group['repos'] = new_repos
    
    # Save JSON
    if modified:
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(json_config[0] if len(json_config) == 1 else json_config, f, indent=2, ensure_ascii=False)
        write_log(f"JSON config saved to: {json_file}")
        write_log("Config content:")
        print(json.dumps(json_config[0] if len(json_config) == 1 else json_config, indent=2, ensure_ascii=False))
    else:
        write_log("No updates needed")
    
    write_log("Processing completed")


if __name__ == '__main__':
    main()
