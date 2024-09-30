package com.zhaocai.system.api.config.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.common.core.domain.R;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class InnerRFeignDecoder implements Decoder {

    private ObjectMapper objectMapper;

    public InnerRFeignDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {
        String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));

        R<?> resultData = objectMapper.readValue(body, objectMapper.getTypeFactory().constructType(R.class));
        if (resultData.getCode() != R.SUCCESS) {
            throw new RuntimeException("调用接口异常;code=" + resultData.getCode() + ";msg=" + resultData.getMsg());
        }

        return objectMapper.convertValue(resultData.getData(),objectMapper.getTypeFactory().constructType(type));
    }
}
