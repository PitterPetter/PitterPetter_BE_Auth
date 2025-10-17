package PitterPatter.loventure.authService.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import PitterPatter.loventure.authService.dto.request.RockCompletionAckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TerritoryServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${territory.service.url:http://territory-service:8083}")
    private String territoryServiceUrl;
    
    /**
     * Territory-service로 rock 완료 ACK 전송
     */
    public void sendRockCompletionAck(String coupleId, String userId) {
        try {
            RockCompletionAckRequest request = new RockCompletionAckRequest(coupleId, userId, LocalDateTime.now());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<RockCompletionAckRequest> entity = new HttpEntity<>(request, headers);
            
            String url = territoryServiceUrl + "/internal/rock-status/ack";
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Territory-service ACK 전송 성공 - coupleId: {}, userId: {}", coupleId, userId);
            } else {
                log.warn("Territory-service ACK 전송 실패 - coupleId: {}, userId: {}, status: {}", 
                        coupleId, userId, response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Territory-service ACK 전송 중 오류 발생 - coupleId: {}, userId: {}, error: {}", 
                    coupleId, userId, e.getMessage(), e);
            throw new RuntimeException("Territory-service ACK 전송 실패", e);
        }
    }
}
