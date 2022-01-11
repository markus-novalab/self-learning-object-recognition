package org.khpi.ai.service;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class StaticHandle {
    public static double average(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0);
    }

    public static double geoMean(List<Double> values) {
        long count = values.stream()
                .filter(value -> value > 0)
                .count();

        if (count == 0) {
            throw new IllegalStateException("Count of not zero attributes cannot be 0");
        }

        Double multiplicationOfAllNonZeroValues = values.stream()
                .filter(value -> value > 0)
                .reduce((x1, x2) -> x1 * x2)
                .orElse(0.0);

        return Math.pow(multiplicationOfAllNonZeroValues, 1.0 / count);
    }
}
