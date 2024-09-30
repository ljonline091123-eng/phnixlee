package com.zhaocai.system.api.config.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.common.core.web.bean.ResultData;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * ResultData 解码器
 *
 * @author chenming
 * @date 2024-08-31
 */
public class ResultDataFeignDecoder implements Decoder {

    private ObjectMapper objectMapper;

    public ResultDataFeignDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {
        String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));

        ResultData<?> resultData = objectMapper.readValue(body, objectMapper.getTypeFactory().constructType(ResultData.class));
        if (!resultData.isSuccess()) {
            throw new RuntimeException("调用接口异常;code=" + resultData.getCode() + ";msg=" + resultData.getMsg());
        }

        return objectMapper.convertValue(resultData.getData(),objectMapper.getTypeFactory().constructType(type));
    }
}
