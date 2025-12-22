package com.jlm.homework.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class TimeOutConf {

    @Bean
    RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> {
            restClientBuilder
                    .requestFactory(new BufferingClientHttpRequestFactory(
                            ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                                    .withConnectTimeout(Duration.ofSeconds(80))
                                    .withReadTimeout(Duration.ofSeconds(120))
                            )));
        };
    }
}
