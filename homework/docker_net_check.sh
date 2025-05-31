
#!/bin/bash

# -----------------------------
# Docker 网络自动探测与配置脚本
# 第三阶段：自动化网络修复与配置优化
# -----------------------------

# 设置要检查通信的两个容器名称
CONTAINER1="container1"
CONTAINER2="container2"
CUSTOM_NETWORK="my_bridge_net"

# Step 1: 检查 Docker 是否运行
if ! systemctl is-active --quiet docker; then
  echo "Docker 服务未运行，正在启动..."
  sudo systemctl start docker
fi

# Step 2: 显示现有网络
echo "当前 Docker 网络："
docker network ls

# Step 3: 检查两个容器是否存在
if ! docker inspect "$CONTAINER1" &>/dev/null || ! docker inspect "$CONTAINER2" &>/dev/null; then
  echo "请确保容器 $CONTAINER1 和 $CONTAINER2 存在并运行！"
  exit 1
fi

# Step 4: 获取 CONTAINER2 的 IP 地址
IP2=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$CONTAINER2")

# Step 5: 在 CONTAINER1 内 ping 容器2 的 IP
echo "正在测试 $CONTAINER1 是否能 ping 通 $CONTAINER2 ($IP2)..."
docker exec "$CONTAINER1" ping -c 2 "$IP2" > /dev/null 2>&1

if [ $? -eq 0 ]; then
  echo "✅ 容器 $CONTAINER1 与 $CONTAINER2 可以通信。"
else
  echo "⚠️ 容器通信失败，尝试创建自定义网络 $CUSTOM_NETWORK..."

  # Step 6: 创建自定义网络（如不存在）
  if ! docker network ls | grep -q "$CUSTOM_NETWORK"; then
    docker network create --driver bridge "$CUSTOM_NETWORK"
    echo "已创建自定义 bridge 网络：$CUSTOM_NETWORK"
  fi

  # Step 7: 将两个容器连接到该网络
  docker network connect "$CUSTOM_NETWORK" "$CONTAINER1" 2>/dev/null
  docker network connect "$CUSTOM_NETWORK" "$CONTAINER2" 2>/dev/null

  echo "已将 $CONTAINER1 和 $CONTAINER2 加入网络 $CUSTOM_NETWORK"

  # Step 8: 再次测试通信
  IP2_NEW=$(docker inspect -f "{{.NetworkSettings.Networks.$CUSTOM_NETWORK.IPAddress}}" "$CONTAINER2")
  docker exec "$CONTAINER1" ping -c 2 "$IP2_NEW" > /dev/null 2>&1

  if [ $? -eq 0 ]; then
    echo "✅ 网络修复成功，容器间已可通信。"
  else
    echo "❌ 网络连接仍失败，请手动检查防火墙或容器状态。"
  fi
fi

# Step 9: 显示网络连接情况
echo
echo "📌 容器网络连接概况："
docker network inspect "$CUSTOM_NETWORK" | grep Name

