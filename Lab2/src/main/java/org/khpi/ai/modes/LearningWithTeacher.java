package org.khpi.ai.modes;

import org.khpi.ai.model.Entity;
import org.khpi.ai.service.StaticHandle;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LearningWithTeacher {

    private final Comparator<Map.Entry<Entity, Double>> comparator = (first, second) -> {
        if (first.getValue() > second.getValue()) {
            return 1;
        } else if (first.getValue().equals(second.getValue())) {
            return 0;
        } else {
            return -1;
        }
    };

    public Entity run(Set<Entity> allEntities, Entity inputEntity) {
        Map<Entity, Integer> similarityByEntity = new HashMap<>();
        Map<String, Double> averagesIn = inputEntity.getAttributes().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> StaticHandle.average(inputEntity.getAttributes().get(entry.getKey()))));

        allEntities.forEach(entity -> {
            long count = entity.getAttributes().entrySet().stream()
                    .filter(entry -> {
                        String propertyName = entry.getKey();
                        return computeEvaluationFunction(allEntities, averagesIn.get(propertyName), propertyName, entity);
                    })
                    .count();

            count += computeStringAttributes(inputEntity, entity);

            similarityByEntity.put(entity, (int) count);
        });

        Optional<Integer> max = similarityByEntity.values().stream()
                .max(Comparator.naturalOrder());

        if (max.isPresent()) {
            return similarityByEntity.entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getValue(), max.get()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cannot find entity by max F function number"))
                    .getKey();

        }

        throw new IllegalStateException("Cannot find entity by max F function number");
    }

    private boolean computeEvaluationFunction(Set<Entity> allEntities, double inValue, String attribute, Entity searchedEntity) {
        Map<Entity, Double> difference = new HashMap<>();
        allEntities.forEach(entity -> difference.put(entity, computeEvaluationFunction(StaticHandle.average(entity.getAttributes().get(attribute)), inValue)));

        Map.Entry<Entity, Double> entityDoubleEntry = difference.entrySet().stream()
                .min(comparator)
                .orElse(null);

        return entityDoubleEntry != null && entityDoubleEntry.getKey().equals(searchedEntity);
    }

    private double computeEvaluationFunction(double storedValue, double averageOrGeoMean) {
        return Math.abs(averageOrGeoMean - storedValue);
    }

    private int computeStringAttributes(Entity input, Entity stored) {
        return (int) stored.getStringAttributes().stream()
                .filter(attr -> input.getStringAttributes().contains(attr))
                .count();
    }
}
