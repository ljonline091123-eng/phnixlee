package com.zhaocai.common.signature.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Jackson 工具类
 *
 * @author chenming
 * @date 2024-07-24
 */
@Slf4j
public class JacksonUtil {
    private static ObjectMapper objectMapper = null;

    static {
        objectMapper = new ObjectMapper()
                // 过滤空值
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 转换对象时忽略不存在的字段
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Object 转换为 Map
     * @param object
     * @return
     */
    public static Map<String,?> object2Map(Object object) {
        // 先将 DTO 转换为 String Json
        try {
            String jsonString = objectMapper.writeValueAsString(object);

            return objectMapper.readValue(jsonString, new TypeReference<Map<String, ?>>() {});
        } catch (JsonProcessingException e) {
            log.error("[JacksonUtil] - Object 转换为 Map 失败,cause by:{}",e.getMessage(),e);
            throw new RuntimeException("object2Map - Object 转换为 Map 失败",e);
        }
    }

    /**
     * Object 转换为 JsonString
     * @param object
     * @return
     */
    public static String toJsonString(Object object) {
        try {
            return object == null ? "" : objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("[JacksonUtil] - Object 转换为 String 失败,cause by:{}",e.getMessage(),e);
            throw new RuntimeException("toJsonString - Object 转换为 String 失败",e);
        }
    }

    /**
     * Object 转换为 JsonNode 后再转成 JsonString
     * @param object
     * @return
     */
    public static String toNodeJsonString(Object object) {
        try {
            object = object == null ? "" : objectMapper.writeValueAsString(object);
            JsonNode jsonNode = objectMapper.valueToTree(object);
            return jsonNode.toString();
        } catch (JsonProcessingException e) {
            log.error("[JacksonUtil] - Object 转换为 JsonNode 后再转成 String 失败,cause by:{}",e.getMessage(),e);
            throw new RuntimeException("toJsonString - Object 转换为 JsonNode 后再转成 String 失败",e);
        }
    }

    /**
     * String 转换为 JsonNode
     * @param jsonString
     * @return
     */
    public static ObjectNode toObjectNode(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return objectMapper.createObjectNode();
        }

        try {
            return (ObjectNode) objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            log.error("[JacksonUtil] - String 转换为 ObjectNode 失败,cause by:{}",e.getMessage(),e);
            throw new RuntimeException("toObjectNode - String 转换为 ObjectNode 失败",e);
        }
    }

    /**
     * 转换为 List
     * @param jsonNode
     * @param classType
     * @return
     */
    public static <T> List<T> toList(JsonNode jsonNode,Class<T> classType) {
        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, classType);
        return objectMapper.convertValue(jsonNode, listType);
    }


    /**
     * 转换为 Object 对象
     * @param jsonString
     * @param classType
     * @return
     * @param <T>
     */
    public static <T> T toObject(String jsonString,Class<T> classType) {
        try {
            return objectMapper.readValue(jsonString,classType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("toObject - String 转换为 Object 对象失败",e);
        }
    }
}
