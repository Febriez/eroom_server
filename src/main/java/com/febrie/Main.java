package com.febrie;

import com.febrie.http.HttpServer;
import com.febrie.util.Logging;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 애플리케이션 메인 클래스
 */
public class Main {
    private static final Logger log = Logging.getLogger(Main.class);

    @Getter
    private final static Properties config = new Properties();

    public static void main(String @NotNull [] args) {
        Logging.info(log, "결제 서버 시작 중...");

        // 포트 설정 (기본값: 8080)
        int port = 8080;

        // 명령행 인수에서 포트 설정 가능
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                Logging.info(log, "사용자 지정 포트: {}", port);
            } catch (NumberFormatException e) {
                Logging.warn(log, "유효하지 않은 포트 번호: {}, 기본 포트 사용: {}", args[0], port);
            }
        } else {
            Logging.info(log, "기본 포트 사용: {}", port);
        }

        try {
            new HttpServer(port).start();
            Logging.info(log, "서버가 http://localhost:{}/ 에서 실행 중입니다", port);

            // 콘솔에 시각적 표시
            printServerStartBanner(port);

        } catch (IOException e) {
            Logging.error(log, "서버 시작 실패: {}", e.getMessage(), e);
            System.exit(1);
        }

        // 설정 파일 로드 시도
        try (InputStream configStream = Main.class.getClassLoader().getResourceAsStream("cryptoKeys.properties")) {
            if (configStream != null) {
                config.load(configStream);
                Logging.info(log, "설정 파일을 성공적으로 로드했습니다");
            } else {
                Logging.warn(log, "cryptoKeys.properties 파일을 찾을 수 없습니다. 기본 설정을 사용합니다");
            }
        } catch (Exception e) {
            Logging.warn(log, "설정 파일 로드 중 오류 발생", e);
        }

        Logging.info(log, "시스템 정보:");
        Logging.info(log, "   OS: {}", System.getProperty("os.name"));
        Logging.info(log, "   Java 버전: {}", System.getProperty("java.version"));
        Logging.info(log, "   작업 디렉토리: {}", System.getProperty("user.dir"));

        Logging.info(log, "서버 초기화가 완료되었습니다!");
    }

    /**
     * 서버 시작 배너를 콘솔에 출력
     */
    private static void printServerStartBanner(int port) {
        System.out.println("\n" +
                "┌─────────────────────────────────────────────────┐\n" +
                "│                                                 │\n" +
                "│     토스페이먼츠 결제 서버가 시작되었습니다            │\n" +
                "│                                                 │\n" +
                "│     URL: http://localhost:" + port + "/         │\n" +
                "│                                                 │\n" +
                "│     로그: 콘솔에서 확인 가능                        │\n" +
                "│     종료: Ctrl+C                                 │\n" +
                "│                                                 │\n" +
                "└─────────────────────────────────────────────────┘\n");
    }
}
