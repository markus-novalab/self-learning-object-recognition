package org.khpi.ai.redis;

import lombok.experimental.UtilityClass;
import redis.clients.jedis.Jedis;

@UtilityClass
public class RedisClient {

    public static Jedis openConnection() {
        String host = "localhost";
        int port = 6379;
        Jedis jedis = new Jedis(host, port, false);

        if (!jedis.isConnected()) {
            throw new IllegalStateException(String.format("Cannot connect to Redis with configs:" +
                    "host: %s, port: %s, ssl: %s", host, port, false));
        }

        return jedis;
    }
}
