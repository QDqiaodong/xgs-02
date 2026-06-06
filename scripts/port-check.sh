#!/bin/bash

check_port() {
    local port=$1
    local name=$2
    local original=$port

    while lsof -i:$port > /dev/null 2>&1; do
        echo "端口 $port 被占用，尝试下一个端口..."
        port=$((port + 1))
    done

    if [ "$port" != "$original" ]; then
        echo "$name 端口调整为: $port"
    else
        echo "$name 端口: $port"
    fi

    echo "$port"
}

FRONTEND_PORT=$(check_port 8025 "前端")
BACKEND_PORT=$(check_port 9025 "后端")
MYSQL_PORT=$(check_port 3325 "MySQL")
REDIS_PORT=$(check_port 6425 "Redis")

export FRONTEND_PORT
export BACKEND_PORT
export MYSQL_PORT
export REDIS_PORT
