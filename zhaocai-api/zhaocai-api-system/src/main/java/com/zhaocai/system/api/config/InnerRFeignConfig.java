package com.zhaocai.system.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.system.api.config.decoder.InnerRFeignDecoder;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class InnerRFeignConfig {

    @Bean
    @Primary
    public Decoder decoder(ObjectMapper objectMapper) {
        return new InnerRFeignDecoder(objectMapper);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
