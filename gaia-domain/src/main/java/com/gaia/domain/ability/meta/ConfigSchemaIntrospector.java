package com.gaia.domain.ability.meta;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置类的 JSON Schema 内省器（OpenAPI 风格）。
 *
 * <p>运行时反射 {@code @Ability#configClass()} 所指向的类，构造一份对应
 * 字段结构的简化 JSON Schema。规则见下：</p>
 *
 * <pre>
 *   {@code String/Boolean/Integer/Long/Double/Float}      → {"type":"string|boolean|integer|number"}
 *   {@code LocalDateTime/Date/LocalDate}                  → {"type":"string","format":"date-time|date"}
 *   {@code enum}                                          → {"type":"string","enum":[...]}
 *   {@code List<E>/Collection<E>/E[]}                     → {"type":"array","items":<schema-of-E>}
 *   {@code Map<K,V>}                                      → {"type":"object"}
 *   其他 POJO                                              → {"type":"object","properties":{...}}
 *   深度超过 {@link #MAX_DEPTH} 或自引用类型                  → {"type":"object"} 兜底
 * </pre>
 *
 * <p>输出类型为 {@code Map<String,Object>}，便于直接通过 Jackson / Spring 序列化到 wire。</p>
 */
public final class ConfigSchemaIntrospector {

    /** 防御性深度上限：超过此深度的递归统一降级为 object。 */
    private static final int MAX_DEPTH = 5;

    private ConfigSchemaIntrospector() {
    }

    /**
     * 入口：内省一个配置类为 JSON Schema。若传入 {@code null} 或 {@link Void}，
     * 返回 {@code null}（表示该能力无独立配置）。
     */
    public static Map<String, Object> introspect(Class<?> clazz) {
        if (clazz == null || clazz == Void.class) {
            return null;
        }
        return describeType(clazz, 0, new java.util.HashSet<>());
    }

    // ----------------- core -----------------

    private static Map<String, Object> describeType(Class<?> type, int depth, java.util.Set<Class<?>> visited) {
        if (type == null || depth > MAX_DEPTH) {
            return objectPlaceholder();
        }

        if (type == String.class)                       return scalar("string");
        if (type == CharSequence.class)                  return scalar("string");
        if (type == Integer.class || type == Long.class
                || type == Short.class || type == Byte.class
                || type == int.class || type == long.class
                || type == short.class || type == byte.class) return scalar("integer");
        if (type == Boolean.class || type == boolean.class) return scalar("boolean");
        if (type == Double.class || type == Float.class
                || type == double.class || type == float.class) return scalar("number");

        if (type == LocalDateTime.class || type == Date.class) {
            Map<String, Object> s = scalar("string");
            s.put("format", "date-time");
            return s;
        }
        if (type == LocalDate.class) {
            Map<String, Object> s = scalar("string");
            s.put("format", "date");
            return s;
        }

        if (type.isEnum()) {
            Map<String, Object> s = scalar("string");
            List<String> values = new ArrayList<>();
            for (Object e : type.getEnumConstants()) {
                values.add(((Enum<?>) e).name());
            }
            s.put("enum", values);
            return s;
        }

        if (type.isArray()) {
            return arrayOf(type.getComponentType(), depth + 1, visited);
        }
        if (Collection.class.isAssignableFrom(type) || List.class.isAssignableFrom(type)) {
            // raw Collection/List → 占位；具体 items 由字段 ParameterizedType 提供
            return arrayOf(Object.class, depth + 1, visited);
        }
        if (Map.class.isAssignableFrom(type)) {
            return objectPlaceholder();
        }

        // POJO
        return describePojo(type, depth, visited);
    }

    private static Map<String, Object> describeFieldType(Type type, int depth, java.util.Set<Class<?>> visited) {
        if (type instanceof Class<?>) {
            return describeType((Class<?>) type, depth, visited);
        }
        if (type instanceof ParameterizedType pt) {
            Class<?> raw = (Class<?>) pt.getRawType();
            if (Collection.class.isAssignableFrom(raw) || List.class.isAssignableFrom(raw)) {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "array");
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0) {
                    schema.put("items", describeFieldType(args[0], depth + 1, visited));
                } else {
                    schema.put("items", objectPlaceholder());
                }
                return schema;
            }
            if (Map.class.isAssignableFrom(raw)) {
                return objectPlaceholder();
            }
            return describeType(raw, depth, visited);
        }
        return objectPlaceholder();
    }

    private static Map<String, Object> arrayOf(Class<?> itemType, int depth, java.util.Set<Class<?>> visited) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("items", describeType(itemType, depth, visited));
        return schema;
    }

    private static Map<String, Object> describePojo(Class<?> pojoType, int depth, java.util.Set<Class<?>> visited) {
        if (visited.contains(pojoType) || depth > MAX_DEPTH) {
            return objectPlaceholder();
        }
        java.util.Set<Class<?>> next = new java.util.HashSet<>(visited);
        next.add(pojoType);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        // 简单对所有非 static / 非 transient / 非 serialVersionUID 字段建模
        for (Field f : pojoType.getDeclaredFields()) {
            int mods = f.getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) {
                continue;
            }
            String name = f.getName();
            if ("serialVersionUID".equals(name)) {
                continue;
            }
            properties.put(name, describeFieldType(f.getGenericType(), depth + 1, next));
        }
        if (!properties.isEmpty()) {
            schema.put("properties", properties);
        }
        return schema;
    }

    // ----------------- helpers -----------------

    private static Map<String, Object> scalar(String typeName) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", typeName);
        return s;
    }

    private static Map<String, Object> objectPlaceholder() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "object");
        return s;
    }
}
