package com.dataquadinc.dto;

import com.dataquadinc.model.UserType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RoleDeserializer extends JsonDeserializer<Set<UserType>> {

    @Override
    public Set<UserType> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Set<UserType> roles = new HashSet<>();
        JsonNode node = p.getCodec().readTree(p);
        
        if (node.isArray()) {
            for (JsonNode element : node) {
                roles.add(UserType.fromValue(element.asText()));
            }
        } else if (node.isTextual()) {
            roles.add(UserType.fromValue(node.asText()));
        }
        
        return roles;
    }
}
