package com.febrie.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class HttpClient {

    private final URL URL;

    public HttpClient(String address) throws MalformedURLException {
        URL = URI.create(address).toURL();
    }

    public JsonObject sendRequest(@NotNull HttpMethod method, JsonObject headers, JsonObject body) throws IOException {
        HttpURLConnection cn = HttpUtils.getDefaultConnection(URL);
        JsonObject responseData = readData(cn, method, headers, body);
        cn.disconnect();
        return responseData;
    }

    private JsonObject readData(@NotNull HttpURLConnection cn, @NotNull HttpMethod method, JsonObject headers, JsonObject body) throws IOException {
        cn.setRequestMethod(method.name());
        if (headers != null)
            for (String key : headers.keySet()) cn.setRequestProperty(key, headers.get(key).getAsString());
        if (body != null) {
            cn.setDoOutput(true);
            byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
            cn.getOutputStream().write(data, 0, data.length);
        }
        int responseCode = cn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(String.valueOf(responseCode).startsWith("2") ? cn.getInputStream() : cn.getErrorStream()));
        return JsonParser.parseString(br.lines().map(String::trim).collect(Collectors.joining("\n"))).getAsJsonObject();
    }
}
