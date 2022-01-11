package org.khpi.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.khpi.ai.model.Entity;
import org.khpi.ai.modes.LearningWithTeacher;
import org.khpi.ai.redis.RedisClient;
import org.khpi.ai.service.LearningHandle;
import org.khpi.ai.service.TextColor;
import redis.clients.jedis.Jedis;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


public class Application {
    private static final String LOAD_OPTION = "load";
    private static final String CLEAR_OPTION = "clear";
    private static final String LIST_OPTION = "list";
    private static final String TEACHER_OPTION = "teacher";
    private static final String RECOGNITION = "recognition";
    private static final String RECOGNITION_WITH_LEARNING = "rec+learn";
    private static final String SET_NAME = "books";

    private static final LearningWithTeacher teacher = new LearningWithTeacher();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonParser jp = new JsonParser();
    private static final CommandLineParser parser = new DefaultParser();
    private static CommandLine cmd;

    public static void main(String[] args) throws ParseException {
        Type entityListType = new TypeToken<ArrayList<Entity>>() {
        }.getType();

        Options options = new Options();
        options.addOption(Option.builder()
                .longOpt(LOAD_OPTION)
                .hasArg()
                .build()
        );
        options.addOption(Option.builder()
                .longOpt(CLEAR_OPTION)
                .build()
        );
        options.addOption(Option.builder()
                .longOpt(LIST_OPTION)
                .build()
        );
        options.addOption(Option.builder()
                .longOpt(TEACHER_OPTION)
                .hasArg()
                .build()
        );
        options.addOption(Option.builder()
                .longOpt(RECOGNITION)
                .hasArg()
                .build()
        );
        options.addOption(Option.builder()
                .longOpt(RECOGNITION_WITH_LEARNING)
                .hasArg()
                .build()
        );


        cmd = parser.parse(options, args);

        try (Jedis jedis = RedisClient.openConnection();
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Redis health: " + TextColor.GREEN_BRIGHT.getAttr() + jedis.isConnected() +
                    TextColor.RESET.getAttr());

            if (cmd.hasOption(LIST_OPTION)) {
                Set<String> smembers = jedis.smembers(SET_NAME);
                smembers.forEach(member -> {
                    JsonParser jp = new JsonParser();
                    JsonElement je = jp.parse(member);
                    System.out.println(gson.toJson(je));
                });
                return;
            }

            if (cmd.hasOption(CLEAR_OPTION)) {
                jedis.flushAll();
                System.out.println(TextColor.GREEN_BRIGHT.getAttr() + "All data cleared" + TextColor.RESET.getAttr());
                return;
            }

            if (cmd.hasOption(LOAD_OPTION)) {
                String filePath = cmd.getOptionValue(LOAD_OPTION);
                String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
                List<Entity> entities = gson.fromJson(content, entityListType);
                entities.forEach(entity -> jedis.sadd(SET_NAME, gson.toJson(entity)));
                System.out.println("Downloaded books: " + entities.stream().map(Entity::getName).collect(Collectors.toList()));
            }

            if (cmd.hasOption(TEACHER_OPTION)) {
                Entity inputEntity = getInputEntity(TEACHER_OPTION);
                Set<Entity> entitiesFromDB = loadAllDataFromRedis(jedis);
                Entity foundEntity = teacher.run(entitiesFromDB, inputEntity);
                applyResults(inputEntity, foundEntity, entitiesFromDB, jedis, reader, true);
                return;
            }

            if (cmd.hasOption(RECOGNITION)) {
                Entity inputEntity = getInputEntity(RECOGNITION);
                Set<Entity> entitiesFromDB = loadAllDataFromRedis(jedis);
                Entity foundEntity = teacher.run(entitiesFromDB, inputEntity);
                System.out.println("FOUND: \n" + TextColor.GREEN_BRIGHT.getAttr() +
                        gson.toJson(foundEntity) + TextColor.RESET.getAttr());
                return;
            }

            if (cmd.hasOption(RECOGNITION_WITH_LEARNING)) {
                Entity inputEntity = getInputEntity(RECOGNITION_WITH_LEARNING);
                Set<Entity> entitiesFromDB = loadAllDataFromRedis(jedis);
                Entity foundEntity = teacher.run(entitiesFromDB, inputEntity);
                System.out.println("FOUND: \n" + TextColor.GREEN_BRIGHT.getAttr() +
                        gson.toJson(foundEntity) + TextColor.RESET.getAttr());
                applyResults(inputEntity, foundEntity, entitiesFromDB, jedis, reader, false);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Entity getInputEntity(String optionName) throws IOException {
        String filePath = cmd.getOptionValue(optionName);
        String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        Entity inputEntity = gson.fromJson(content, Entity.class);
        System.out.println("Your input: " + TextColor.YELLOW_BRIGHT.getAttr() + inputEntity + TextColor.RESET.getAttr());

        return inputEntity;
    }

    private static Set<Entity> loadAllDataFromRedis(Jedis jedis) {
        return jedis.smembers(SET_NAME).stream()
                .map(jp::parse)
                .map(jsonElement -> gson.fromJson(jsonElement, Entity.class))
                .collect(Collectors.toSet());
    }

    private static void applyResults(Entity inputEntity, Entity foundEntity, Set<Entity> entitiesFromDB, Jedis jedis,
                                     BufferedReader reader, boolean needConfirmation) throws IOException {
        String userInput;
        if (needConfirmation) {
            System.out.println("\nIs it --> " + TextColor.YELLOW_BRIGHT.getAttr() + foundEntity.getName() + TextColor.RESET.getAttr() +
                    " <--- ???\n[" + TextColor.GREEN_BRIGHT.getAttr() + "Yes" + TextColor.RESET.getAttr() + ", " +
                    TextColor.RED_BRIGHT.getAttr() + "No" + TextColor.RESET.getAttr() + "]:");
            userInput = reader.readLine().toLowerCase(Locale.ROOT);
        } else {
            userInput = "yes";
        }

        Entity entity;

        if (userInput.equals("yes")) {
            entity = LearningHandle.handleSuccess(inputEntity, foundEntity);
        } else if (userInput.equals("no")) {
            entity = LearningHandle.handleFailure(inputEntity, foundEntity);
        } else {
            System.err.println("Unknown input, please enter only yes or no");
            return;
        }

        Entity finalEntity = entity;
        Set<Entity> newSet = entitiesFromDB.stream()
                .filter(obj -> !obj.getId().equals(finalEntity.getId()))
                .collect(Collectors.toSet());
        newSet.add(entity);

        jedis.flushAll();
        newSet.forEach(obj -> jedis.sadd(SET_NAME, gson.toJson(obj)));
    }
}
