package com.zhaocai.system.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.system.api.config.decoder.ResultDataFeignDecoder;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内部数据结果外部配置
 *
 * @author chenming
 * @date 2024-08-31
 */
//@Configuration
public class ResultDataFeignConfig {

  //  @Bean
    public Decoder resultDataFeign(ObjectMapper objectMapper) {
        return new ResultDataFeignDecoder(objectMapper);
    }
}
