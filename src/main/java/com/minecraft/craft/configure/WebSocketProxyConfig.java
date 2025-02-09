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
            // 目标 WebSocket 服务器的地址（V2Ray代理）
            URI targetUri = URI.create("ws://127.0.0.1:2048/whatever");

            HttpHeaders headers = new HttpHeaders();
            headers.setAll(session.getHandshakeInfo().getHeaders().toSingleValueMap());
            headers.set("Upgrade", "websocket");
            headers.set("Connection", "Upgrade");
            headers.set("Host", "127.0.0.1:2048");

            return webSocketClient.execute(targetUri, headers, targetSession -> {
                // 客户端 -> 目标 WebSocket 服务器
                Flux<WebSocketMessage> clientMessages = session.receive();

                // 目标服务器 -> 客户端
                Flux<WebSocketMessage> targetMessages = targetSession.receive();

                // 直接 `transform()` 以避免 `send()` 过载
                Mono<Void> sendToTarget = targetSession.send(clientMessages);
                Mono<Void> sendToClient = session.send(targetMessages);

                return Mono.when(sendToTarget, sendToClient)
                        .doOnCancel(() -> {
                            closeSafely(session);
                            closeSafely(targetSession);
                        })
                        .doOnTerminate(() -> {
                            closeSafely(session);
                            closeSafely(targetSession);
                        })
                        .onErrorResume(e -> {
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