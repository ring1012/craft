package com.minecraft.craft.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketProxyConfig {

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    public WebSocketService webSocketService() {
        return new HandshakeWebSocketService();
    }

    @Bean
    public SimpleUrlHandlerMapping handlerMapping(WebSocketHandler webSocketHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/whatever", webSocketHandler); // 处理 /whatever 路径的 WebSocket 请求
        return new SimpleUrlHandlerMapping(map, -1);
    }

    @Bean
    public WebSocketHandler webSocketHandler(WebSocketClient webSocketClient) {
        return session -> {
            URI targetUri = URI.create("ws://127.0.0.1:2048/whatever");

            HttpHeaders headers = new HttpHeaders();
            headers.setAll(session.getHandshakeInfo().getHeaders().toSingleValueMap());

            return webSocketClient.execute(targetUri, headers, targetSession -> {
                // 客户端 -> 目标 WebSocket 服务器
                Flux<WebSocketMessage> clientMessages = session.receive()
                        .doOnNext(message -> message.retain()); // 增加引用计数

                // 目标服务器 -> 客户端
                Flux<WebSocketMessage> targetMessages = targetSession.receive()
                        .doOnNext(message -> message.retain()); // 增加引用计数

                // 直接 `send()`，避免 `flatMap()`
                Mono<Void> sendToTarget = targetSession.send(clientMessages); // 释放引用计数

                Mono<Void> sendToClient = session.send(targetMessages); // 释放引用计数

                return Mono.when(sendToTarget, sendToClient)
                        .doOnCancel(() -> {
                            System.out.println("Connection cancelled");
                            closeSafely(session);
                            closeSafely(targetSession);
                        })
                        .doOnTerminate(() -> {
                            closeSafely(session);
                            closeSafely(targetSession);
                        })
                        .onErrorResume(e -> {
                            System.err.println("WebSocket error: " + e.getMessage());
                            closeSafely(session);
                            closeSafely(targetSession);
                            return Mono.empty();
                        });
            });
        };
    }

    // 安全关闭 WebSocket 连接
    private void closeSafely(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            session.close().subscribe();
        }
    }

    @Bean
    public WebSocketClient webSocketClient() {
        return new ReactorNettyWebSocketClient();
    }
}