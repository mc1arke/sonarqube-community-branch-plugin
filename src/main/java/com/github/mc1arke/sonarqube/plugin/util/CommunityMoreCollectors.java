package com.github.mc1arke.sonarqube.plugin.util;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public final class CommunityMoreCollectors {

    private static <K, V> Supplier<Map<K, V>> newHashMapSupplier(int expectedSize) {
        return () -> {
            return expectedSize == 0 ? new HashMap() : new HashMap(expectedSize);
        };
    }


    public static <K, E> Collector<E, Map<K, E>, ImmutableMap<K, E>> uniqueIndex(Function<? super E, K> keyFunction) {
        return uniqueIndex(keyFunction, Function.identity());
    }

    public static <K, E> Collector<E, Map<K, E>, ImmutableMap<K, E>> uniqueIndex(Function<? super E, K> keyFunction, int expectedSize) {
        return uniqueIndex(keyFunction, Function.identity(), expectedSize);
    }

    public static <K, E, V> Collector<E, Map<K, V>, ImmutableMap<K, V>> uniqueIndex(Function<? super E, K> keyFunction, Function<? super E, V> valueFunction) {
        return uniqueIndex(keyFunction, valueFunction, 0);
    }

    public static <K, E, V> Collector<E, Map<K, V>, ImmutableMap<K, V>> uniqueIndex(Function<? super E, K> keyFunction, Function<? super E, V> valueFunction, int expectedSize) {
        verifyKeyAndValueFunctions(keyFunction, valueFunction);
        BiConsumer<Map<K, V>, E> accumulator = (map, element) -> {
            K key = Objects.requireNonNull(keyFunction.apply(element), "Key function can't return null");
            V value = Objects.requireNonNull(valueFunction.apply(element), "Value function can't return null");
            putAndFailOnDuplicateKey(map, key, value);
        };
        BinaryOperator<Map<K, V>> merger = (m1, m2) -> {
            Iterator var2 = m2.entrySet().iterator();

            while(var2.hasNext()) {
                Map.Entry<K, V> entry = (Map.Entry)var2.next();
                putAndFailOnDuplicateKey(m1, entry.getKey(), entry.getValue());
            }

            return m1;
        };
        return Collector.of(newHashMapSupplier(expectedSize), accumulator, merger, ImmutableMap::copyOf, Collector.Characteristics.UNORDERED);
    }

    private static void verifyKeyAndValueFunctions(Function<?, ?> keyFunction, Function<?, ?> valueFunction) {
        Objects.requireNonNull(keyFunction, "Key function can't be null");
        Objects.requireNonNull(valueFunction, "Value function can't be null");
    }

    private static <K, V> void putAndFailOnDuplicateKey(Map<K, V> map, K key, V value) {
        V existingValue = map.put(key, value);
        if (existingValue != null) {
            throw new IllegalArgumentException(String.format("Duplicate key %s", key));
        }
    }


}
