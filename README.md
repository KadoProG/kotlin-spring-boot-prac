# Kotlin Spring Boot Practice

Kotlin + Spring Boot で実装された TODO アプリケーション API

## 環境要件

- Java 21
- Gradle 8.5 (Gradle Wrapper を使用)

## セットアップ

### 1. Java 21 のインストール確認

```bash
java -version
```

Java 21 がインストールされていることを確認してください。

### 2. プロジェクトのビルド

```bash
./gradlew build
```

### 3. 環境変数の設定

機密情報や環境固有の設定は環境変数で管理できます。

プロジェクトルートに `.env` ファイルを作成し、`.env.example` を参考に設定してください：

```bash
cp .env.example .env
# .envファイルを編集して、使用するデータベースに合わせて設定を変更
```

`.env` ファイルは `.gitignore` に含まれているため、Git にはコミットされません。

詳細は「データベース設定」セクションを参照してください。

### 4. アプリケーションの起動

環境変数を使用する場合：

```bash
export $(cat .env | xargs) && ./gradlew bootRun
```

環境変数を使用しない場合（デフォルト値が使用されます）：

```bash
./gradlew bootRun
```

アプリケーションは `http://localhost:8080/api` で起動します。

## API エンドポイント

### ヘルスチェック

```bash
curl http://localhost:8080/api/v1/health
```

## プロジェクト構造

```
kotlin-spring-boot-prac/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/example/kotlinspringbootprac/
│   │   │       ├── KotlinSpringBootPracApplication.kt
│   │   │       ├── config/
│   │   │       └── controller/
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── kotlin/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## データベース設定

### MySQL を使用する場合

1. Docker Compose で MySQL を起動：

```bash
docker-compose up -d
```

2. `.env` ファイルを作成し、以下の設定を追加：

```bash
DB_URL=jdbc:mysql://localhost:3306/kotlinspringboot?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_DRIVER=com.mysql.cj.jdbc.Driver
DB_USERNAME=springuser
DB_PASSWORD=springpass
DB_PLATFORM=org.hibernate.dialect.MySQLDialect
```

3. phpMyAdmin にアクセス（データベース管理ツール）：
   - URL: `http://localhost:8080`
   - Username: `springuser`
   - Password: `springpass`

### H2 Database を使用する場合（開発用）

デフォルトでは H2 Database（インメモリ）が使用されます。

- H2 Console: `http://localhost:8080/api/h2-console`
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: (空)

## コードフォーマット

このプロジェクトでは [ktlint](https://ktlint.github.io/) を使用してコードフォーマットを統一しています。

### 全コードを自動フォーマット

```bash
./gradlew ktlintFormat
```

プロジェクト内のすべての Kotlin ファイルが自動的にフォーマットされます。

### フォーマットチェックのみ（修正しない）

```bash
./gradlew ktlintCheck
```

フォーマット違反があるかどうかをチェックするだけで、ファイルは修正されません。CI/CD パイプラインなどで使用できます。

## Git Hooks (lefthook)

このプロジェクトでは [lefthook](https://github.com/evilmartians/lefthook) を使用して Git フックを管理しています。

### セットアップ

1. lefthook のインストール（未インストールの場合）：

```bash
# macOS (Homebrew)
brew install lefthook

# その他の環境
# https://github.com/evilmartians/lefthook#installation を参照
```

2. Git フックのインストール：

```bash
lefthook install
```

### 設定内容

- **pre-commit**: コミット前に自動的に `ktlintFormat` を実行してコードをフォーマットします
- **pre-push**: プッシュ前に `gradle build` を実行してビルドが成功することを確認します

### フックのスキップ（緊急時のみ）

特定のコミットでフックをスキップする必要がある場合：

```bash
# pre-commit をスキップ
git commit --no-verify

# pre-push をスキップ
git push --no-verify
```

**注意**: 通常はフックをスキップしないことを推奨します。

## 技術スタック

- Kotlin 1.9.20
- Spring Boot 3.2.0
- Spring Data JPA
- Spring Security
- MySQL / H2 Database
- JWT (認証用)
- Docker Compose (データベース用)

## 次のステップ

1. エンティティクラスの作成 (User, Task, TaskAction)
2. リポジトリクラスの作成
3. サービス層の実装
4. コントローラーの実装
5. JWT 認証の実装
