# 修复 skills.csv 文件编码问题

# 读取文件字节
$bytes = [System.IO.File]::ReadAllBytes('skills.csv')

# 尝试不同的编码
$encodings = @(
    [System.Text.Encoding]::UTF8,
    [System.Text.Encoding]::GetEncoding('GBK'),
    [System.Text.Encoding]::GetEncoding('GB2312'),
    [System.Text.Encoding]::GetEncoding('UTF-16'),
    [System.Text.Encoding]::GetEncoding('UTF-16BE'),
    [System.Text.Encoding]::ASCII
)

# 测试每种编码
foreach ($encoding in $encodings) {
    try {
        $content = $encoding.GetString($bytes)
        Write-Host "测试编码: $($encoding.EncodingName)"
        # 显示前几行内容
        $lines = $content -split "`r`n"
        for ($i = 0; $i -lt [Math]::Min(5, $lines.Length); $i++) {
            Write-Host $lines[$i]
        }
        Write-Host '---'
    } catch {
        Write-Host "测试编码: $($encoding.EncodingName) 失败"
        Write-Host '---'
    }
}

# 尝试使用 UTF-8 with BOM 重新保存文件
Write-Host "尝试使用 UTF-8 with BOM 重新保存文件..."
try {
    # 先尝试用 GBK 解码
    $gbkEncoding = [System.Text.Encoding]::GetEncoding('GBK')
    $content = $gbkEncoding.GetString($bytes)
    
    # 然后用 UTF-8 with BOM 编码保存
    $utf8Encoding = New-Object System.Text.UTF8Encoding($true) # $true 表示包含 BOM
    [System.IO.File]::WriteAllText('skills.csv', $content, $utf8Encoding)
    
    Write-Host "文件已成功转换为 UTF-8 with BOM 编码"
    
    # 验证转换结果
    Write-Host "转换后文件内容:"
    $newContent = [System.IO.File]::ReadAllText('skills.csv', $utf8Encoding)
    $newLines = $newContent -split "`r`n"
    for ($i = 0; $i -lt [Math]::Min(5, $newLines.Length); $i++) {
        Write-Host $newLines[$i]
    }
} catch {
    Write-Host "转换失败: $($_.Exception.Message)"
}
