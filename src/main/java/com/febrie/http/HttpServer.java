
package com.febrie.http;

import com.febrie.payment.PaymentAPI;
import com.febrie.payment.PaymentAPIImpl;
import com.febrie.payment.PaymentHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpServer {
    private final com.sun.net.httpserver.HttpServer server;

    public HttpServer(int port) throws IOException {
        this.server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newFixedThreadPool(10));
        setupApiHandlers();
    }

    public void start() {
        server.start();
    }

    private void setupApiHandlers() {
        PaymentAPI paymentAPI = new PaymentAPIImpl();
        server.createContext("/api/payment/process", new PaymentHandler(paymentAPI));
    }

}