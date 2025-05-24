
package com.febrie.http;

import com.febrie.payment.api.PaymentAPI;
import com.febrie.payment.api.PaymentAPIImpl;
import com.febrie.payment.handler.PaymentHandler;
import com.febrie.util.Logging;
import com.sun.net.httpserver.HttpContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class HttpServer {
    private static final Logger log = Logging.getLogger(HttpServer.class);
    
    private final int port;
    private final com.sun.net.httpserver.HttpServer server;

    public HttpServer(int port) throws IOException {
        this.port = port;
        this.server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newFixedThreadPool(10));

        // 정적 파일 핸들러 설정
        setupStaticFileHandler();

        // API 핸들러 설정
        setupApiHandlers();
    }

    public void start() {
        server.start();
        Logging.info(log, "서버가 시작되었습니다. 포트: {}", port);
        Logging.info(log, "브라우저에서 http://localhost:{}/ 로 접속하세요.", port);
    }

    private void setupStaticFileHandler() {
        // 정적 파일 제공 경로 설정
        String staticFilesPath = "src/main/resources/static";
        // 최대 파일 크기 설정 (10MB)
        final int MAX_FILE_SIZE = 10 * 1024 * 1024;

        // 정적 파일 핸들러 등록
        HttpContext context = server.createContext("/", exchange -> {
            String requestPath = exchange.getRequestURI().getPath();
            String filePath = staticFilesPath + (requestPath.equals("/") ? "/index.html" : requestPath);

            File file = new File(filePath);

            if (!file.exists() || file.isDirectory()) {
                // 파일이 없거나 디렉토리인 경우 404 반환
                String response = "404 (Not Found)";
                try {
                    exchange.sendResponseHeaders(404, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                } catch (IOException e) {
                    Logging.debug(log, "404 응답 전송 중 오류 (무시됨): {}", e.getMessage());
                } finally {
                    try {
                        exchange.getResponseBody().close();
                    } catch (IOException ignored) {
                        // 닫기 실패 무시
                    }
                }
                return;
            }

            try {
                // 파일 확장자에 따라 Content-Type 설정
                String contentType = getContentType(filePath);
                exchange.getResponseHeaders().set("Content-Type", contentType);

                // 파일 내용을 응답으로 전송
                Path path = Paths.get(filePath);
                
                // 파일 크기 확인
                long fileSize = Files.size(path);
                if (fileSize > MAX_FILE_SIZE) {
                    Logging.warn(log, "파일 크기 제한 초과: {} ({} bytes)", filePath, fileSize);
                    String errorMsg = "413 Content Too Large";
                    exchange.sendResponseHeaders(413, errorMsg.length());
                    exchange.getResponseBody().write(errorMsg.getBytes());
                    return;
                }
                
                byte[] fileContent = Files.readAllBytes(path);
                
                // 응답 헤더 설정 후 응답 본문 전송
                try {
                    exchange.sendResponseHeaders(200, fileContent.length);
                    exchange.getResponseBody().write(fileContent);
                } catch (IOException e) {
                    // 클라이언트가 연결을 중단한 경우 등의 네트워크 예외는 조용히 로깅만
                    Logging.debug(log, "클라이언트 연결 오류 (무시됨): {}", e.getMessage());
                }
                            } catch (IOException e) {
                Logging.error(log, "파일 읽기 오류: {}", filePath, e);
                try {
                    String errorMsg = "500 Internal Server Error";
                    exchange.sendResponseHeaders(500, errorMsg.length());
                    exchange.getResponseBody().write(errorMsg.getBytes());
                } catch (IOException ignored) {
                    // 응답 전송 실패는 무시 (클라이언트 연결 종료 등)
                }
            } finally {
                try {
                    exchange.getResponseBody().close();
                } catch (IOException ignored) {
                    // 닫기 실패 무시
                }
            }
        });
    }

    private void setupApiHandlers() {
        // API 구현체 생성
        PaymentAPI paymentAPI = new PaymentAPIImpl();

        // 결제 API 핸들러 등록
        server.createContext("/api/payment/process", new PaymentHandler(paymentAPI));
    }

    private @NotNull String getContentType(@NotNull String filePath) {
        if (filePath.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        } else if (filePath.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        } else if (filePath.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        } else if (filePath.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        } else if (filePath.endsWith(".png")) {
            return "image/png";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filePath.endsWith(".gif")) {
            return "image/gif";
        } else if (filePath.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            return "application/octet-stream";
        }
    }
}