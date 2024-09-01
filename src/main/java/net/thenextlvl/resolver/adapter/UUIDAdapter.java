package net.thenextlvl.resolver.adapter;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.UUID;

public class UUIDAdapter implements JsonDeserializer<UUID> {
    @Override
    public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
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
