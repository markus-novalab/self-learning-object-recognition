package org.khpi.ai.service;

import lombok.experimental.UtilityClass;
import org.khpi.ai.model.Entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Stream;

@UtilityClass
public class LearningHandle {
    public static Entity handleSuccess(Entity input, Entity found) {
        Map<String, List<Double>> newAttributes = new HashMap<>();
        input.getStringAttributes().addAll(found.getStringAttributes());

        found.getAttributes().forEach((key, value) -> {
            Stream<Double> inputValues = input.getAttributes().get(key).stream();
            Stream<Double> foundValues = found.getAttributes().get(key).stream();

            OptionalDouble average = Stream.concat(inputValues, foundValues)
                    .mapToDouble(Double::doubleValue)
                    .average();

            newAttributes.put(key, Collections.singletonList(average.orElse(value.get(0))));
        });

        return new Entity(found.getId(), found.getName(), newAttributes, input.getStringAttributes());
    }

    public static Entity handleFailure(Entity input, Entity found) {
        found.getStringAttributes().removeAll(input.getStringAttributes());

        return new Entity(found.getId(), found.getName(), found.getAttributes(), found.getStringAttributes());
    }
}
