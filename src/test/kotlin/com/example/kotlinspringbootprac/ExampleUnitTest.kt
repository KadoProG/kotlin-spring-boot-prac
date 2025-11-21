package com.example.kotlinspringbootprac

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * サンプルユニットテスト（MockKの使用例）
 */
class ExampleUnitTest {

    @Test
    fun `MockKを使用したサンプルテスト`() {
        // Mockオブジェクトの作成
        val mockService = mockk<TestService>()

        // モックの動作を定義
        every { mockService.getMessage() } returns "Hello, MockK!"

        // テスト実行
        val result = mockService.getMessage()

        // 検証
        assertEquals("Hello, MockK!", result)
        verify { mockService.getMessage() }
    }

    // テスト用のインターフェース
    interface TestService {
        fun getMessage(): String
    }
}
