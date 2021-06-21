package com.saankaa.rapidxend.config;

/**
 * This Env variable must be set before launch app
 * It's used for different configuration
 */
public class AppEnvVariable {

    // Redis
    public final static String REDIS_HOST = "REDIS.host";
    public final static String REDIS_PORT = "REDIS.port";
    public final static String REDIS_CHANNEL = "REDIS.channel"; // Used to listen all application event (Peering, file download...)

    // Mongo
    public final static String MONGO_DB_NAME = "MONGO.db_name";
    public final static String MONGO_CONNECTION_URL = "MONGO.connection_url";


}
