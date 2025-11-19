# デプロイメントガイド

本番環境へのデプロイ方法を説明します。

## 1. JAR ファイルのビルド

```bash
# テストをスキップしてビルド（高速）
./gradlew build -x test

# テストも含めてビルド（推奨）
./gradlew build
```

ビルド後、JAR ファイルは `build/libs/` ディレクトリに生成されます。

```bash
# JARファイルの確認
ls -lh build/libs/
```

通常、以下のようなファイルが生成されます：

- `kotlin-spring-boot-prac-0.0.1-SNAPSHOT.jar` (実行可能 JAR)

## 2. デプロイ方法

### 方法 1: 直接実行（シンプル）

#### 2.1 サーバーへの転送

```bash
# SCPでサーバーに転送
scp build/libs/kotlin-spring-boot-prac-0.0.1-SNAPSHOT.jar user@server:/opt/app/

# または rsync
rsync -avz build/libs/kotlin-spring-boot-prac-0.0.1-SNAPSHOT.jar user@server:/opt/app/
```

#### 2.2 サーバー上で実行

```bash
# SSHでサーバーに接続
ssh user@server

# アプリケーションディレクトリに移動
cd /opt/app

# 環境変数を設定
export DB_URL=jdbc:postgresql://localhost:5432/mydb
export DB_DRIVER=org.postgresql.Driver
export DB_USERNAME=myuser
export DB_PASSWORD=mypassword
export JWT_SECRET=your-very-secure-secret-key-here
export SERVER_PORT=8080

# 本番プロファイルで実行
java -jar -Dspring.profiles.active=prod kotlin-spring-boot-prac-0.0.1-SNAPSHOT.jar
```

#### 2.3 バックグラウンド実行（nohup）

```bash
# nohupでバックグラウンド実行
nohup java -jar -Dspring.profiles.active=prod kotlin-spring-boot-prac-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# プロセスIDを確認
echo $! > app.pid

# ログを確認
tail -f app.log
```

### 方法 2: systemd サービス（推奨）

#### 2.1 サービスファイルの作成

`/etc/systemd/system/kotlin-spring-boot-prac.service` を作成：

```ini
[Unit]
Description=Kotlin Spring Boot Practice Application
After=network.target postgresql.service

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/opt/app
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /opt/app/kotlin-spring-boot-prac-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=kotlin-spring-boot-prac

# 環境変数
Environment="DB_URL=jdbc:postgresql://localhost:5432/mydb"
Environment="DB_DRIVER=org.postgresql.Driver"
Environment="DB_USERNAME=myuser"
Environment="DB_PASSWORD=mypassword"
Environment="JWT_SECRET=your-very-secure-secret-key-here"
Environment="SERVER_PORT=8080"

[Install]
WantedBy=multi-user.target
```

#### 2.2 サービスの起動

```bash
# systemdの設定をリロード
sudo systemctl daemon-reload

# サービスを有効化（起動時に自動起動）
sudo systemctl enable kotlin-spring-boot-prac

# サービスを起動
sudo systemctl start kotlin-spring-boot-prac

# ステータス確認
sudo systemctl status kotlin-spring-boot-prac

# ログ確認
sudo journalctl -u kotlin-spring-boot-prac -f
```

#### 2.3 サービスの管理コマンド

```bash
# 停止
sudo systemctl stop kotlin-spring-boot-prac

# 再起動
sudo systemctl restart kotlin-spring-boot-prac

# 無効化（自動起動を停止）
sudo systemctl disable kotlin-spring-boot-prac
```

### 方法 3: Docker コンテナ

#### 3.1 Dockerfile の作成

プロジェクトルートに `Dockerfile` を作成：

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# JARファイルをコピー
COPY build/libs/kotlin-spring-boot-prac-0.0.1-SNAPSHOT.jar app.jar

# 非rootユーザーで実行
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

#### 3.2 Docker イメージのビルド

```bash
# ビルド
./gradlew build

# Dockerイメージをビルド
docker build -t kotlin-spring-boot-prac:latest .

# イメージの確認
docker images | grep kotlin-spring-boot-prac
```

#### 3.3 Docker コンテナの実行

```bash
# コンテナを実行
docker run -d \
  --name kotlin-spring-boot-prac \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/mydb \
  -e DB_DRIVER=org.postgresql.Driver \
  -e DB_USERNAME=myuser \
  -e DB_PASSWORD=mypassword \
  -e JWT_SECRET=your-very-secure-secret-key-here \
  kotlin-spring-boot-prac:latest

# ログ確認
docker logs -f kotlin-spring-boot-prac

# 停止
docker stop kotlin-spring-boot-prac

# 削除
docker rm kotlin-spring-boot-prac
```

#### 3.4 docker-compose.yml（オプション）

```yaml
version: "3.8"

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:postgresql://db:5432/mydb
      - DB_DRIVER=org.postgresql.Driver
      - DB_USERNAME=myuser
      - DB_PASSWORD=mypassword
      - JWT_SECRET=your-very-secure-secret-key-here
    depends_on:
      - db

  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=mydb
      - POSTGRES_USER=myuser
      - POSTGRES_PASSWORD=mypassword
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

実行：

```bash
docker-compose up -d
```

### 方法 4: クラウドプラットフォーム

#### AWS (Elastic Beanstalk / EC2)

1. **Elastic Beanstalk**:

   - JAR ファイルをアップロード
   - 環境変数を設定
   - 自動デプロイ

2. **EC2**:
   - EC2 インスタンスに SSH 接続
   - 方法 1 または方法 2 を使用

#### Google Cloud Platform (Cloud Run / App Engine)

1. **Cloud Run**:

   ```bash
   # DockerイメージをGCRにプッシュ
   gcloud builds submit --tag gcr.io/PROJECT_ID/kotlin-spring-boot-prac

   # Cloud Runにデプロイ
   gcloud run deploy kotlin-spring-boot-prac \
     --image gcr.io/PROJECT_ID/kotlin-spring-boot-prac \
     --platform managed \
     --region asia-northeast1 \
     --set-env-vars DB_URL=...,JWT_SECRET=...
   ```

#### Azure (App Service)

1. Azure Portal から App Service を作成
2. JAR ファイルをデプロイ
3. 環境変数を設定

## 3. 本番環境の設定

### 3.1 環境変数の設定

本番環境では以下の環境変数を必ず設定してください：

```bash
# データベース
DB_URL=jdbc:postgresql://localhost:5432/mydb
DB_DRIVER=org.postgresql.Driver
DB_USERNAME=myuser
DB_PASSWORD=mypassword

# JWT
JWT_SECRET=your-very-secure-secret-key-here  # 必ず変更
JWT_EXPIRATION=86400000

# サーバー
SERVER_PORT=8080

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

### 3.2 セキュリティチェックリスト

- [ ] JWT_SECRET を強力なランダム文字列に変更
- [ ] データベースパスワードを強力なものに設定
- [ ] H2 Console を無効化（本番環境では使用しない）
- [ ] ログレベルを INFO 以上に設定
- [ ] HTTPS を有効化（リバースプロキシを使用）
- [ ] ファイアウォール設定を確認
- [ ] データベース接続を SSL/TLS で暗号化

### 3.3 リバースプロキシ（Nginx 例）

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 4. デプロイ後の確認

```bash
# ヘルスチェック
curl http://localhost:8080/api/v1/health

# ログ確認
tail -f /var/log/kotlin-spring-boot-prac/application.log

# プロセス確認
ps aux | grep kotlin-spring-boot-prac
```

## 5. ロールバック

問題が発生した場合のロールバック手順：

```bash
# systemdの場合
sudo systemctl stop kotlin-spring-boot-prac
sudo cp kotlin-spring-boot-prac-0.0.1-SNAPSHOT.jar.backup kotlin-spring-boot-prac-0.0.1-SNAPSHOT.jar
sudo systemctl start kotlin-spring-boot-prac

# Dockerの場合
docker stop kotlin-spring-boot-prac
docker run -d --name kotlin-spring-boot-prac kotlin-spring-boot-prac:previous-version
```
