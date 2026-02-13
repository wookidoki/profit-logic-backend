#!/bin/bash
set -e

echo "=== Profit Logic Deployment Script ==="

# Variables
APP_DIR=/home/ubuntu/profit-logic
BACKEND_REPO=https://github.com/wookidoki/profit-logic-backend.git
FRONTEND_REPO=https://github.com/wookidoki/profit-logic-frontend.git

# 1. Install Docker if not present
if ! command -v docker &> /dev/null; then
    echo "[1/6] Installing Docker..."
    sudo apt-get update -y
    sudo apt-get install -y ca-certificates curl gnupg
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo usermod -aG docker ubuntu
    echo "Docker installed. Please re-login and re-run this script."
    exit 0
else
    echo "[1/6] Docker already installed."
fi

# 2. Install Node.js if not present (for frontend build)
if ! command -v node &> /dev/null; then
    echo "[2/6] Installing Node.js 20 LTS..."
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt-get install -y nodejs
else
    echo "[2/6] Node.js already installed: $(node --version)"
fi

# 3. Clone or update repos
echo "[3/6] Cloning/updating repositories..."
mkdir -p $APP_DIR
cd $APP_DIR

if [ -d "profit-logic-backend" ]; then
    cd profit-logic-backend && git fetch origin && git checkout develop && git pull origin develop && cd ..
else
    git clone -b develop $BACKEND_REPO
fi

if [ -d "profit-logic-frontend" ]; then
    cd profit-logic-frontend && git fetch origin && git checkout develop && git pull origin develop && cd ..
else
    git clone -b develop $FRONTEND_REPO
fi

# 4. Build frontend
echo "[4/6] Building frontend..."
cd $APP_DIR/profit-logic-frontend
npm ci --production=false
npm run build

# 5. Copy frontend dist to backend nginx volume location
echo "[5/6] Copying frontend build to Nginx..."
sudo rm -rf $APP_DIR/profit-logic-backend/frontend-dist
cp -r dist $APP_DIR/profit-logic-backend/frontend-dist

# 6. Deploy with Docker Compose
echo "[6/6] Starting Docker Compose..."
cd $APP_DIR/profit-logic-backend

# .env 파일이 없으면 안내 메시지 출력
if [ ! -f .env ]; then
    echo ""
    echo "  .env 파일이 없습니다. 아래 형식으로 생성해주세요:"
    echo ""
    echo "  MYSQL_ROOT_PASSWORD=<your-root-password>"
    echo "  MYSQL_USER=<your-db-user>"
    echo "  MYSQL_PASSWORD=<your-db-password>"
    echo "  JWT_SECRET=<your-jwt-secret>"
    echo "  SERVER_IP=<your-server-ip>"
    echo "  GEMINI_API_KEY=<your-gemini-api-key>"
    echo ""
    echo "생성 후 다시 실행해주세요."
    exit 1
fi

# Ensure GEMINI_API_KEY is in .env
if ! grep -q "GEMINI_API_KEY" .env 2>/dev/null; then
    echo ""
    echo "  .env에 GEMINI_API_KEY가 없습니다. 추가해주세요."
    echo "  (없으면 AI 챗봇이 규칙 기반으로만 동작합니다)"
fi

docker compose -f docker-compose.prod.yml down 2>/dev/null || true
docker compose -f docker-compose.prod.yml up -d --build

echo ""
echo "=== Deployment Complete ==="
echo "Frontend: http://profitlogic.cloud"
echo "Backend API: http://profitlogic.cloud/api/health"
echo ""
echo "Check status: docker compose -f docker-compose.prod.yml ps"
echo "Check logs:   docker compose -f docker-compose.prod.yml logs -f app"
