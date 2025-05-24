package com.febrie.payment;

import com.febrie.payment.api.PaymentAPI;
import com.febrie.payment.api.PaymentAPIImpl;
import com.febrie.payment.controller.PaymentController;
import com.febrie.payment.handler.PaymentHandler;
import com.febrie.util.Logging;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

@Slf4j
public class PaymentServer {

    private static final String GAME_SERVER_URL = "http://localhost:8080"; // 게임 서버 URL 설정

    private final HttpServer server;

    public PaymentServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        try {
            // API 구현체 초기화 (게임 서버 URL은 내부에서 설정)
            PaymentAPI paymentAPI = new PaymentAPIImpl();

            // 핸들러 등록
            server.createContext("/api/payment/verify", new PaymentController(paymentAPI));
            server.createContext("/api/payment/process", new PaymentHandler(paymentAPI));

            Logging.info(log, "결제 서버 초기화 완료. 포트: {}", port);
            Logging.info(log, "게임 서버 URL: {}", GAME_SERVER_URL);
        } catch (Exception e) {
            log.error("❌ 서버 초기화 중 오류 발생: {}", e.getMessage(), e);
            throw new IOException("결제 서버 초기화 실패: " + e.getMessage(), e);
        }
    }

    public void start() {
        server.start();
        log.info("결제 서버 시작됨");
        log.info("결제 처리 엔드포인트: http://localhost:{}/api/payment/process", server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        log.info("결제 서버 종료됨");
    }

    public static void main(String @NotNull [] args) {
        try {
            // 포트 설정 (기본값: 8000)
            int port = 8000;

            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    log.warn("올바르지 않은 포트 번호: {}. 기본 포트(8000)를 사용합니다.", args[0]);
                }
            }

            log.info("결제 서버를 포트 {}에서 시작합니다...", port);
            PaymentServer server = new PaymentServer(port);
            server.start();

            // 종료 훅 등록
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("서버 종료 신호를 받았습니다. 서버를 안전하게 종료합니다...");
                server.stop();
            }));

        } catch (Exception e) {
            log.error("서버 시작 실패: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
