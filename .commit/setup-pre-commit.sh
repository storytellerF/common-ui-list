destination_dir=".git/hooks"
# 生成时间戳，格式为YYYYMMDD-HHMMSS
timestamp=$(date +%Y%m%d-%H%M%S)

# 构造含有时间戳的目标文件名
destination_file="${destination_dir}/pre-commit.$timestamp"

target_file="${destination_dir}/pre-commit"

[ -f "$target_file" ] && cp "$target_file" "$destination_file"
cp .commit/pre-commit.template "$target_file"
xattr -d com.apple.provenance "$target_file"
chmod +x "$target_file"