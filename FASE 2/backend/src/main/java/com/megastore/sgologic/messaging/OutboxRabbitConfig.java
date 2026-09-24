package com.megastore.sgologic.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxRabbitConfig {

    public static final String BUSINESS_EXCHANGE = "sgo.business.events.v1";

    @Bean
    TopicExchange businessEventsExchange() {
        return new TopicExchange(BUSINESS_EXCHANGE, true, false);
    }

    @Bean
    MessageConverter businessEventMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
