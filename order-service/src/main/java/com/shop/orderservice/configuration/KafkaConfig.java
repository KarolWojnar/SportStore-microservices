package com.shop.orderservice.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.put(JsonDeserializer.TYPE_MAPPINGS,
                "com.shop.cartservice.model.dto.ProductsInCartInfoResponse:com.shop.orderservice.model.dto.ProductsInCartInfoResponse," +
                "com.shop.productservice.model.dto.TotalPriceOfProductsResponse:com.shop.orderservice.model.dto.TotalPriceOfProductsResponse," +
                "com.shop.productservice.model.dto.ProductPriceByIdResponse:com.shop.orderservice.model.dto.ProductPriceByIdResponse," +
                "com.shop.productservice.model.dto.ProductsByIdResponse:com.shop.orderservice.model.dto.ProductsByIdResponse," +
                "com.shop.productservice.model.dto.OrderProductRatedRequest:com.shop.orderservice.model.dto.OrderProductRatedRequest," +
                "com.shop.paymentservice.model.dto.CreateOrderRequest:com.shop.orderservice.model.dto.CreateOrderRequest," +
                "com.shop.paymentservice.model.dto.OrderSessionRequest:com.shop.orderservice.model.dto.OrderSessionRequest," +
                "com.shop.paymentservice.model.dto.OrderRepaymentRequest:com.shop.orderservice.model.dto.OrderRepaymentRequest," +
                "com.shop.authservice.model.dto.UserEmailResponse:com.shop.orderservice.model.dto.UserEmailResponse," +
                "com.shop.customer.model.dto.CustomerInfoResponse:com.shop.orderservice.model.dto.CustomerInfoResponse");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
