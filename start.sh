#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo "====================================="
echo "  家常食谱共享平台 - Docker 一键启动"
echo "====================================="
echo ""

if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

DOCKER_REGISTRY=${DOCKER_REGISTRY:-docker.m.daocloud.io}
FRONTEND_PORT=${FRONTEND_PORT:-8025}
BACKEND_PORT=${BACKEND_PORT:-9025}
MYSQL_PORT=${MYSQL_PORT:-3325}
REDIS_PORT=${REDIS_PORT:-6425}

check_port() {
    local port=$1
    local name=$2
    local original=$port

    while true; do
        if lsof -i:$port > /dev/null 2>&1; then
            echo -e "${YELLOW}  ⚠ 端口 $port 被占用，尝试下一个...${NC}"
            port=$((port + 1))
        else
            if [ "$port" != "$original" ]; then
                echo -e "${GREEN}  ✓ $name 端口调整为: $port${NC}"
            else
                echo -e "${GREEN}  ✓ $name 端口: $port${NC}"
            fi
            break
        fi
    done

    echo "$port"
}

echo -e "${CYAN}>>> 端口检测...${NC}"
echo ""

FRONTEND_PORT=$(check_port "$FRONTEND_PORT" "前端")
BACKEND_PORT=$(check_port "$BACKEND_PORT" "后端")
MYSQL_PORT=$(check_port "$MYSQL_PORT" "MySQL")
REDIS_PORT=$(check_port "$REDIS_PORT" "Redis")

export FRONTEND_PORT
export BACKEND_PORT
export MYSQL_PORT
export REDIS_PORT

echo ""
echo -e "${CYAN}>>> 配置信息${NC}"
echo ""
echo "  镜像仓库:     $DOCKER_REGISTRY"
echo "  前端端口:     $FRONTEND_PORT"
echo "  后端端口:     $BACKEND_PORT"
echo "  MySQL 端口:   $MYSQL_PORT"
echo "  Redis 端口:   $REDIS_PORT"
echo ""

echo -e "${CYAN}>>> 开始构建并启动容器...${NC}"
echo ""

docker compose up -d --build

echo ""
echo -e "${CYAN}>>> 等待服务就绪...${NC}"
echo ""

MAX_RETRIES=30
RETRY=0

while [ $RETRY -lt $MAX_RETRIES ]; do
    if docker compose ps frontend | grep -q "Up" && \
       docker compose ps backend | grep -q "Up" && \
       docker compose ps mysql | grep -q "Up" && \
       docker compose ps redis | grep -q "Up"; then
        break
    fi
    RETRY=$((RETRY + 1))
    sleep 2
    echo -n "  ."
done

echo ""
echo ""
echo "====================================="
echo -e "  ${GREEN}✓ 项目启动成功！${NC}"
echo "====================================="
echo ""
echo -e "  前端访问地址: ${CYAN}http://localhost:${FRONTEND_PORT}${NC}"
echo -e "  后端 API 地址: ${CYAN}http://localhost:${BACKEND_PORT}/api${NC}"
echo ""
echo "  MySQL:     localhost:$MYSQL_PORT"
echo "  Redis:     localhost:$REDIS_PORT"
echo ""
echo "  数据库名:  $MYSQL_DATABASE"
echo "  用户名:    $MYSQL_USER"
echo "  密码:      $MYSQL_PASSWORD"
echo ""
echo "  查看日志:  docker compose logs -f"
echo "  停止服务:  docker compose down"
echo "  重启服务:  docker compose restart"
echo ""
echo "====================================="
