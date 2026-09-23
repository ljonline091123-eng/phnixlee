package com.zhaocai.business.common.interceptor;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class BodyReaderHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final String body;

    public BodyReaderHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = bufferedReader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, bytesRead);
            }
        }
        body = stringBuilder.toString();
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new StringReader(body));
    }

    @Override
    public ServletInputStream getInputStream() {
        return new BodyServletInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    public String getBody() {
        return body;
    }

    private class BodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream byteArrayInputStream;

        public BodyServletInputStream(byte[] body) {
            this.byteArrayInputStream = new ByteArrayInputStream(body);
        }

        @Override
        public int read() throws IOException {
            return byteArrayInputStream.read();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }
    }
}
