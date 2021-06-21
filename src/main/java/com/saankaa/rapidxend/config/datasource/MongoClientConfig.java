package com.saankaa.rapidxend.config.datasource;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.saankaa.rapidxend.config.AppEnvVariable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MongoClientConfig {


    protected String getDatabaseName() {
        return System.getenv(AppEnvVariable.MONGO_DB_NAME);
    }

    @Bean
    public MongoClient mongoClient() {

        String value = System.getenv(AppEnvVariable.MONGO_CONNECTION_URL) != null ? System.getenv(AppEnvVariable.MONGO_CONNECTION_URL)
                : System.getProperty(AppEnvVariable.MONGO_CONNECTION_URL) != null ? System.getProperty(AppEnvVariable.MONGO_CONNECTION_URL) : "mongodb://localhost:27017";

        final ConnectionString connectionString = new ConnectionString(System.getenv(AppEnvVariable.MONGO_CONNECTION_URL) + "/" + getDatabaseName());

        final MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }

}
