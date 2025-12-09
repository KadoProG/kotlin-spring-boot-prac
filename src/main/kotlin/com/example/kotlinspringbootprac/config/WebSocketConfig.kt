package com.example.kotlinspringbootprac.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    @Value("\${app.cors.allowed-origins}")
    private val allowedOrigins: String,
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // クライアントへのメッセージ送信先プレフィックス
        config.enableSimpleBroker("/topic", "/queue")
        // クライアントからのメッセージ送信先プレフィックス
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // WebSocket接続エンドポイント
        registry.addEndpoint("/ws")
            .setAllowedOrigins(*allowedOrigins.split(",").map { it.trim() }.toTypedArray())
            .withSockJS()
    }
}
