package tech.sud.hello.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Rejects JSON numbers, booleans, arrays, and objects instead of coercing them to token strings.
 */
public final class StrictStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            return (String) context.handleUnexpectedToken(String.class, parser);
        }
        return parser.getText();
    }
}
