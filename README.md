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

### 3. 環境変数の設定（オプション）

機密情報や環境固有の設定は環境変数で管理できます。

プロジェクトルートに `.env` ファイルを作成し、以下の環境変数を設定できます：

```bash
# Database Configuration
DB_URL=jdbc:h2:mem:testdb
DB_DRIVER=org.h2.Driver
DB_USERNAME=sa
DB_PASSWORD=

# JWT Configuration
JWT_SECRET=your-secret-key-change-this-in-production
JWT_EXPIRATION=86400000
```

`.env` ファイルは `.gitignore` に含まれているため、Git にはコミットされません。

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

## 開発環境

- H2 Database (インメモリ) を使用
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

## 技術スタック

- Kotlin 1.9.20
- Spring Boot 3.2.0
- Spring Data JPA
- Spring Security
- H2 Database (開発環境)
- JWT (認証用)

## 次のステップ

1. エンティティクラスの作成 (User, Task, TaskAction)
2. リポジトリクラスの作成
3. サービス層の実装
4. コントローラーの実装
5. JWT 認証の実装
