package com.febrie.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 로깅 유틸리티 클래스
 * 애플리케이션 전반에 걸쳐 일관된 로깅 스타일 제공
 */
public class Logging {
    
    /**
     * 지정된 클래스에 대한 로거 인스턴스를 가져옵니다.
     * 
     * @param clazz 로거를 가져올 클래스
     * @return 로거 인스턴스
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * 정보 수준 로그 메시지를 기록합니다.
     * 
     * @param logger 로거 인스턴스
     * @param message 메시지 형식
     * @param args 메시지 인자
     */
    public static void info(Logger logger, String message, Object... args) {
        logger.info(message, args);
    }

    /**
     * 경고 수준 로그 메시지를 기록합니다.
     * 
     * @param logger 로거 인스턴스
     * @param message 메시지 형식
     * @param args 메시지 인자
     */
    public static void warn(Logger logger, String message, Object... args) {
        logger.warn(message, args);
    }

    /**
     * 오류 수준 로그 메시지를 기록합니다.
     * 
     * @param logger 로거 인스턴스
     * @param message 메시지 형식
     * @param args 메시지 인자
     */
    public static void error(Logger logger, String message, Object... args) {
        logger.error(message, args);
    }

    /**
     * 디버그 수준 로그 메시지를 기록합니다.
     * 
     * @param logger 로거 인스턴스
     * @param message 메시지 형식
     * @param args 메시지 인자
     */
    public static void debug(Logger logger, String message, Object... args) {
        logger.debug(message, args);
    }

    /**
     * 예외와 함께 오류 수준 로그 메시지를 기록합니다.
     * 
     * @param logger 로거 인스턴스
     * @param message 메시지 형식
     * @param t 예외 객체
     */
    public static void error(Logger logger, String message, Throwable t) {
        logger.error(message, t);
    }
}
