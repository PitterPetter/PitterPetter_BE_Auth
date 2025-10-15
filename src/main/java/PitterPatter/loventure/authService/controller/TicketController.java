package PitterPatter.loventure.authService.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.dto.TicketBalanceResponse;
import PitterPatter.loventure.authService.dto.TicketEarnResponse;
import PitterPatter.loventure.authService.dto.TicketInfo;
import PitterPatter.loventure.authService.dto.TicketResponse;
import PitterPatter.loventure.authService.dto.TicketUseResponse;
import PitterPatter.loventure.authService.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/couples/{coupleId}/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket", description = "티켓 관리 API")
public class TicketController {

    private final TicketService ticketService;

    /**
     * 티켓 잔액만 조회 (빠른 응답)
     * Region Service에서 사용
     */
    @GetMapping("/daily")
    @Operation(summary = "티켓 잔액 조회", description = "커플의 티켓 잔액을 조회합니다.")
    public ResponseEntity<Map<String, Integer>> getDailyTicketBalance(
            @PathVariable String coupleId) {
        
        int balance = ticketService.getDailyTicketBalance(coupleId);
        return ResponseEntity.ok(Map.of("ticket", balance));
    }

    /**
     * 티켓 전체 정보 조회
     * 클라이언트(프론트엔드)에서 사용
     */
    @GetMapping
    @Operation(summary = "티켓 정보 조회", description = "커플의 티켓 관련 모든 정보를 조회합니다.")
    public ResponseEntity<TicketBalanceResponse> getTicketInfo(
            @PathVariable String coupleId) {
        
        return ResponseEntity.ok(ticketService.getTicketBalanceResponse(coupleId));
    }

    /**
     * 티켓 사용 (내부 API)
     * Region Service에서 지역 해금 시 호출
     */
    @PostMapping("/use")
    @Operation(summary = "[내부] 티켓 사용", description = "지역 해금 시 티켓을 차감합니다. (내부 서비스 전용)")
    public ResponseEntity<TicketUseResponse> useTicket(
            @PathVariable String coupleId) {
        
        ticketService.useTicket(coupleId);
        int remainingBalance = ticketService.getDailyTicketBalance(coupleId);
        
        return ResponseEntity.ok(TicketUseResponse.builder()
            .success(true)
            .remainingBalance(remainingBalance)
            .message("티켓이 사용되었습니다.")
            .build());
    }

    /**
     * 티켓 추가 (내부 API)
     * Course Service에서 코스 저장 시 호출
     */
    @PostMapping("/add")
    @Operation(summary = "[내부] 티켓 추가", description = "코스 저장 시 오늘 티켓을 추가합니다. (내부 서비스 전용)")
    public ResponseEntity<TicketEarnResponse> addTodayTicket(
            @PathVariable String coupleId) {
        
        boolean added = ticketService.addTodayTicket(coupleId);
        int newBalance = ticketService.getDailyTicketBalance(coupleId);
        
        return ResponseEntity.ok(TicketEarnResponse.builder()
            .earned(added)
            .newBalance(newBalance)
            .message(added ? "티켓이 추가되었습니다." : "오늘은 이미 티켓을 사용했습니다.")
            .build());
    }

    // ========== Gateway 연동용 API ==========

    /**
     * Gateway 연동용 티켓 정보 조회
     * Redis 우선 조회, 없으면 DB에서 조회 후 Redis에 저장
     */
    @GetMapping("/info")
    @Operation(summary = "[Gateway] 티켓 정보 조회", description = "Gateway에서 호출하는 티켓 정보 조회 API")
    public ResponseEntity<TicketResponse> getTicketInfoForGateway(
            @PathVariable String coupleId) {
        
        TicketInfo ticketInfo = ticketService.getTicketInfo(coupleId);
        TicketResponse response = TicketResponse.builder()
            .status("success")
            .data(ticketInfo)
            .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gateway 연동용 티켓 정보 업데이트
     * Gateway에서 티켓 상태 변경 후 호출
     */
    @PutMapping
    @Operation(summary = "[Gateway] 티켓 정보 업데이트", description = "Gateway에서 호출하는 티켓 정보 업데이트 API")
    public ResponseEntity<TicketResponse> updateTicketInfo(
            @PathVariable String coupleId,
            @RequestBody TicketInfo ticketInfo) {
        
        ticketService.updateTicketInfo(coupleId, ticketInfo);
        TicketResponse response = TicketResponse.builder()
            .status("success")
            .data(ticketInfo)
            .build();
        
        return ResponseEntity.ok(response);
    }
}
