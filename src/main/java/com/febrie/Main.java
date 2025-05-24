package com.febrie;

import com.febrie.http.HttpServer;
import com.febrie.util.Logging;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    private static final Logger log = Logging.getLogger(Main.class);
    private static final int port = 8080;

    @Getter
    private final static Properties config = new Properties();

    public static void main(String @NotNull [] args) {
        Logging.info(log, "결제 서버 시작 중...");
        try {
            new HttpServer(port).start();
            Logging.info(log, "서버가 http://localhost:{}/ 에서 실행 중입니다", port);

        } catch (IOException e) {
            Logging.error(log, "서버 시작 실패: {}", e.getMessage(), e);
            System.exit(1);
        }
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
        Logging.info(log, "서버 초기화가 완료되었습니다!");
    }
}
