/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.configs;

import java.util.HashMap;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

@Configuration
public class KafkaProducerConfig {

	@Bean
	// ToDo: [SECURITY CONCERN] -- no security.protocol/SASL/SSL settings are configured for the
	// Kafka producer, so unless overridden elsewhere it will connect to the broker over plaintext
	// with no authentication, allowing anyone on the network to read or spoof bid notification
	// events.
	public ProducerFactory<String, Object> producerFactory(
			@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
		var configProperties = new HashMap<String, Object>();
		configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
		configProperties.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
		return new DefaultKafkaProducerFactory<>(configProperties);
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

}
