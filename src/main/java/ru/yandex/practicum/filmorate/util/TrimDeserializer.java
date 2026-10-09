package ru.yandex.practicum.filmorate.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Тримит строку при десериализации JSON — до того, как сработают Bean Validation-констрейнты.
 * Нужен потому, что Jackson сам строки не тримит, а {@code @Email} / {@code @NotBlank} проверяют
 * уже то, что лежит в поле.
 */
public class TrimDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        return value == null ? null : value.trim();
    }
}