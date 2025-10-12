package PitterPatter.loventure.authService.dto.request;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class CoupleMatchRequestDeserializer extends JsonDeserializer<CoupleMatchRequest> {
    
    @Override
    public CoupleMatchRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        // JSON 객체인 경우
        if (node.isObject() && node.has("inviteCode")) {
            String inviteCode = node.get("inviteCode").asText();
            return new CoupleMatchRequest(inviteCode);
        }
        
        // 문자열인 경우
        if (node.isTextual()) {
            String inviteCode = node.asText();
            return new CoupleMatchRequest(inviteCode);
        }
        
        throw new IOException("Cannot deserialize CoupleMatchRequest from: " + node);
    }
}
