package PitterPatter.loventure.authService.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import PitterPatter.loventure.authService.dto.response.StatusUpdateMessage;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.CoupleRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseService {
    
    private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();
    private final CoupleRoomRepository coupleRoomRepository;
    
    public void addConnection(String userId, SseEmitter emitter) {
        connections.put(userId, emitter);
        log.info("SSE 연결 추가 - userId: {}", userId);
        
        // 연결 종료 시 정리
        emitter.onCompletion(() -> removeConnection(userId));
        emitter.onTimeout(() -> removeConnection(userId));
        emitter.onError((ex) -> removeConnection(userId));
    }
    
    public void removeConnection(String userId) {
        connections.remove(userId);
        log.info("SSE 연결 제거 - userId: {}", userId);
    }
    
    public void sendStatusUpdate(String userId, StatusUpdateMessage data) {
        SseEmitter emitter = connections.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("status-update")
                    .data(data));
                log.info("SSE 상태 업데이트 전송 - userId: {}", userId);
            } catch (Exception e) {
                log.error("SSE 전송 실패 - userId: {}, error: {}", userId, e.getMessage());
                removeConnection(userId);
            }
        }
    }
    
    public void sendStatusUpdateToCouple(String coupleId, StatusUpdateMessage data) {
        CoupleRoom coupleRoom = coupleRoomRepository.findByCoupleId(coupleId).orElse(null);
        if (coupleRoom != null) {
            // 생성자에게 전송
            sendStatusUpdate(coupleRoom.getCreatorUserId(), data);
            
            // 참여자에게 전송
            if (coupleRoom.getPartnerUserId() != null) {
                sendStatusUpdate(coupleRoom.getPartnerUserId(), data);
            }
            
            log.info("커플 SSE 상태 업데이트 전송 완료 - coupleId: {}, creatorId: {}, partnerId: {}", 
                    coupleId, coupleRoom.getCreatorUserId(), coupleRoom.getPartnerUserId());
        } else {
            log.warn("커플룸을 찾을 수 없음 - coupleId: {}", coupleId);
        }
    }
    
    public int getConnectionCount() {
        return connections.size();
    }
    
    public boolean isUserConnected(String userId) {
        return connections.containsKey(userId);
    }
}
