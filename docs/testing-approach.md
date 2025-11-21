# テストコード記述のアプローチ

このプロジェクトでは、各層に応じた適切なテストアプローチを使用します。

## 1. コントローラーのテスト

### アプローチ: `@SpringBootTest` + `@AutoConfigureMockMvc`

**理由:**
- Spring Securityの設定が含まれるため、完全なアプリケーションコンテキストが必要
- 統合テストとして、エンドポイント全体の動作を検証
- 認証が必要なエンドポイントのテストも可能

**使用例:**
- `AuthControllerTest`: 認証不要なエンドポイント（register, login）
- `UserControllerTest`: 認証が必要なエンドポイント（/v1/users/me）

**特徴:**
- 実際のHTTPリクエスト/レスポンスをシミュレート
- Spring Securityの設定が適用される
- データベースも使用される（H2インメモリ）

## 2. サービスのテスト

### アプローチ: `@ExtendWith(MockKExtension::class)` + MockK

**理由:**
- ビジネスロジックのユニットテスト
- 依存関係をモックして、サービス層のロジックのみをテスト
- 高速に実行可能

**使用例:**
- `UserServiceTest`: ユーザー登録・ログインのビジネスロジック
- `JwtServiceTest`: JWTトークン生成・検証のロジック

**特徴:**
- データベースや外部依存をモック
- テストの実行が高速
- ビジネスロジックの詳細な検証が可能

## 3. リポジトリのテスト

### アプローチ: `@DataJpaTest`

**理由:**
- データベース層のみをテスト
- 実際のデータベース（H2）を使用してクエリを検証
- トランザクション管理が自動

**使用例:**
- `UserRepositoryTest`: カスタムクエリメソッドの検証

**特徴:**
- 軽量なデータベースコンテキスト
- 各テスト後にロールバック
- 実際のSQLクエリを検証可能

## 4. DTO/バリデーションのテスト

### アプローチ: シンプルなユニットテスト

**理由:**
- バリデーションアノテーションの動作確認
- データ変換ロジックの検証

**使用例:**
- `RegisterRequestTest`: バリデーションルールの確認
- `LoginRequestTest`: バリデーションルールの確認

## テストのディレクトリ構造

```
src/test/kotlin/com/example/kotlinspringbootprac/
├── controller/
│   ├── AuthControllerTest.kt          # @SpringBootTest
│   ├── UserControllerTest.kt           # @SpringBootTest + @WithMockUser
│   └── HealthControllerTest.kt         # @SpringBootTest
├── service/
│   ├── UserServiceTest.kt              # MockK
│   └── JwtServiceTest.kt                # MockK
├── repository/
│   └── UserRepositoryTest.kt            # @DataJpaTest
└── dto/
    ├── RegisterRequestTest.kt           # シンプルなユニットテスト
    └── LoginRequestTest.kt              # シンプルなユニットテスト
```

## テストの実行順序

1. **ユニットテスト**（サービス層、DTO）: 高速、依存関係なし
2. **リポジトリテスト**: データベース層の検証
3. **統合テスト**（コントローラー）: エンドポイント全体の検証

## モックの使い分け

- **MockK**: KotlinプロジェクトではMockKを推奨（KotlinらしいAPI）
- **@MockBean**: Spring Bootの統合テストで使用（@SpringBootTest内）
- **@Mock**: MockKのモックオブジェクト作成

## テストカバレッジ

各層で以下のカバレッジを目指します：

- **コントローラー**: すべてのエンドポイント、エラーハンドリング
- **サービス**: すべてのビジネスロジック、エッジケース
- **リポジトリ**: カスタムクエリメソッド
- **DTO**: バリデーションルール

