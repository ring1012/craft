package com.minecraft.craft.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class NginxLikeController {

    private final WebClient webClient = WebClient.create();

    @RequestMapping("/whatever")
    public Mono<ResponseEntity<byte[]>> handleWhatever(ServerWebExchange exchange) {
        // 检查是否是WebSocket请求
        HttpHeaders headers = exchange.getRequest().getHeaders();
        boolean isWebSocket = headers.containsKey("Upgrade") &&
                "websocket".equalsIgnoreCase(headers.getFirst("Upgrade"));

        if (isWebSocket) {
            System.out.println("isWebSocket");
            // WebSocket 请求由 WebSocketHandler 处理，直接返回空响应
            return Mono.empty();
        } else {
            System.out.println("http normal");
            // 非WebSocket请求，重定向到/mask-page
            return redirectToMaskPage(exchange);
        }
    }

    private Mono<ResponseEntity<byte[]>> redirectToMaskPage(ServerWebExchange exchange) {
        // 返回一个包含 "success" 的响应
        String responseBody = "success";
        return Mono.just(ResponseEntity.ok().body(responseBody.getBytes()));
    }
}