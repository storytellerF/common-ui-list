#!/bin/bash

set -e

if command -v gh &> /dev/null; then
    echo "GitHub CLI (gh) is installed."
else
    echo "GitHub CLI (gh) is not installed."
    echo "Please install it from https://cli.github.com/"
    exit 1
fi

# 遍历以 storyteller_f 开头的环境变量
for var in $(printenv | grep '^storyteller_f'); do
    # 提取变量名和值
    var_name=$(echo "$var" | cut -d '=' -f 1)
    var_value=$(echo "$var" | cut -d '=' -f 2-)

    # 使用 GitHub CLI 设置秘密
    # 注意：确保 GitHub CLI 已登录并具有权限
    ./exec-until-success.sh gh secret set "$var_name" -b "$var_value" --repo storytellerF/common-ui-list
done
