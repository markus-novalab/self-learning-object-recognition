package org.khpi.ai.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@EqualsAndHashCode
public class Entity {
    Long id;
    String name;
    Map<String, List<Double>> attributes;
    Set<String> stringAttributes;
}
