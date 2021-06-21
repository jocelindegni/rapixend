package com.saankaa.rapidxend.config.redis;

import com.saankaa.rapidxend.config.AppEnvVariable;
import com.saankaa.rapidxend.model.Notification;
import com.saankaa.rapidxend.service.notification.INotificationService;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {


    @Bean
    public RedisConnectionFactory redisConnectionFactory() {


        // return new LettuceConnectionFactory(host, Integer.parseInt(port));

        //RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, Integer.parseInt(port));
        //return new JedisConnectionFactory(redisStandaloneConfiguration);
        return new LettuceConnectionFactory();
    }

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {

        // Set channel to listen
        String channel = System.getenv(AppEnvVariable.REDIS_CHANNEL) != null ? System.getenv(AppEnvVariable.REDIS_CHANNEL)
                :(System.getProperty(AppEnvVariable.REDIS_CHANNEL) != null ? System.getenv(AppEnvVariable.REDIS_CHANNEL):"notification");

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic(channel));

        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(INotificationService receiver) {

        // Set method will be called when incoming redis message
        return new MessageListenerAdapter(receiver, "onMessage");

    }

    @Bean
    RedisTemplate<String, Notification> template(RedisConnectionFactory connectionFactory) {

        // Set message serialization
        RedisTemplate<String, Notification> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return redisTemplate;

    }

}
