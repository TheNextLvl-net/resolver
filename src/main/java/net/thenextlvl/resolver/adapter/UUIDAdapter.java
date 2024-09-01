package net.thenextlvl.resolver.adapter;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.UUID;

/**
 * A custom deserializer for UUIDs that implements the JsonDeserializer interface.
 * <p>
 * This class handles the deserialization of JSON elements that represent UUIDs.
 * It supports UUIDs in both standard 36-character format and compact 32-character format.
 * When a UUID is in the compact format, it inserts hyphens at the appropriate positions.
 * <p>
 * Constructor:
 * - UUIDAdapter(): Constructs a new UUIDAdapter instance.
 * <p>
 * Methods:
 * - UUID deserialize(JsonElement json, Type typeOf, JsonDeserializationContext context):
 * Deserialize a JSON element into a UUID object.
 * Throws JsonParseException if the JSON format is invalid.
 */
public class UUIDAdapter implements JsonDeserializer<UUID> {
    @Override
    public UUID deserialize(JsonElement json, Type typeOf, JsonDeserializationContext context) throws JsonParseException {
        var uuid = json.getAsString();
        if (uuid.length() == 36) return UUID.fromString(uuid);
        Preconditions.checkArgument(uuid.length() == 32, "Invalid UUID");
        var parts = new String[]{
                uuid.substring(0, 8),
                uuid.substring(8, 12),
                uuid.substring(12, 16),
                uuid.substring(16, 20),
                uuid.substring(20)
        };
        return UUID.fromString(String.join("-", parts));
    }
}
