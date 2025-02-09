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
import reactor.core.scheduler.Schedulers;

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
                        .doOnNext(message -> {
                            if (!session.isOpen()) {
                                message.release(); // 避免泄漏
                                return;
                            }
                            message.retain(); // 增加引用计数
                        });

                // 目标服务器 -> 客户端
                Flux<WebSocketMessage> targetMessages = targetSession.receive()
                        .doOnNext(message -> {
                            if (!targetSession.isOpen()) {
                                message.release(); // 避免泄漏
                                return;
                            }
                            message.retain(); // 增加引用计数
                        });

                // 发送数据流
                Mono<Void> sendToTarget = targetSession.send(clientMessages)
                        .onErrorResume(e -> {
                            System.err.println("Error sending to target: " + e.getMessage());
                            return Mono.empty();
                        });

                Mono<Void> sendToClient = session.send(targetMessages)
                        .onErrorResume(e -> {
                            System.err.println("Error sending to client: " + e.getMessage());
                            return Mono.empty();
                        });

                return Mono.when(sendToTarget, sendToClient)
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
        if (session == null || !session.isOpen()) {
            return; // 避免重复关闭
        }

        try {
            session.close()
                    .subscribe(
                            null,
                            error -> System.err.println("Error closing WebSocketSession: " + error.getMessage())
                    );
        } catch (Exception e) {
            System.err.println("Exception while closing WebSocketSession: " + e.getMessage());
        }
    }


    @Bean
    public WebSocketClient webSocketClient() {
        return new ReactorNettyWebSocketClient();
    }
}