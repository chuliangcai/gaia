package com.gaia.domain.ability.meta;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.core.annotation.AnnotationUtils;

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
 *   其他 POJO                                              → {"type":"object","properties":{...},"required":[...]}
 *   深度超过 {@link #MAX_DEPTH} 或自引用类型                  → {"type":"object"} 兜底
 * </pre>
 *
 * <p>{@code required} 数组遵循 OpenAPI/JSON Schema 标准：仅包含本对象直接
 * {@code properties} 的字段名（不含嵌套对象的字段）。字段被标记为必填的两条规则:
 * <ul>
 *   <li>Java primitive 类型（{@code int/long/boolean/double/float/short/byte/char}） —
 *       语言层保证非 null，零配置视为必填；</li>
 *   <li>{@code @JsonProperty(required = true)} — Jackson 自带注解，与现有
 *       {@code @JsonClassDescription} / {@code @JsonPropertyDescription} 风格一致。</li>
 * </ul>
 * wrapper 类型（{@code Integer/Long/...}）必须显式标注才算必填。</p>
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

        // 类层级中文名（@JsonClassDescription）→ JSON Schema 标准 description 字段。
        // 用于前端表单给整个配置类/嵌套 POJO 渲染友好标题。
        JsonClassDescription clsAnn = AnnotationUtils.findAnnotation(pojoType, JsonClassDescription.class);
        if (clsAnn != null && !clsAnn.value().isEmpty()) {
            schema.put("description", clsAnn.value());
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
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
            // 必填判定（按 OpenAPI/JSON Schema 语义）:
            //   1) Java primitive 类型 — 语言层保证非 null，零配置视为必填；
            //   2) @JsonProperty(required = true) — Jackson 自带注解，与现有
            //      @JsonClassDescription/@JsonPropertyDescription 风格一致。
            // 注意:wrapper 类型(Integer/Long/...)不算 primitive，必须显式标注。
            if (f.getType().isPrimitive()) {
                required.add(name);
            } else {
                JsonProperty jp = AnnotationUtils.findAnnotation(f, JsonProperty.class);
                if (jp != null && jp.required()) {
                    required.add(name);
                }
            }
            Map<String, Object> propSchema = describeFieldType(f.getGenericType(), depth + 1, next);
            // 字段层级中文名（@JsonPropertyDescription）→ property 节点 description。
            // 前端表单 label 优先取此值，缺省时降级 humanize()。
            // 必填字段可在 wrapper 类型字段上叠加 @JsonProperty(required = true)，
            // introspector 会把字段名收进 object 节点的 required 数组。
            JsonPropertyDescription fieldAnn = AnnotationUtils.findAnnotation(f, JsonPropertyDescription.class);
            if (fieldAnn != null && !fieldAnn.value().isEmpty()) {
                propSchema.put("description", fieldAnn.value());
            }
            properties.put(name, propSchema);
        }
        if (!properties.isEmpty()) {
            schema.put("properties", properties);
        }
        if (!required.isEmpty()) {
            schema.put("required", required);
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
