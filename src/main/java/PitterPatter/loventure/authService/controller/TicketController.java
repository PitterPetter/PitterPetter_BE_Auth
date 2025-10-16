package PitterPatter.loventure.authService.controller;

import lombok.extern.slf4j.Slf4j;

/**
 * 티켓 관리 API (Write-Through 패턴 적용)
 * 
 * 기존의 {coupleId} 파라미터가 있는 모든 티켓 API들은 제거되었습니다.
 * 
 * 현재 구현된 API:
 * - GET /api/couples/ticket (CouplesController에 구현)
 *   - Gateway에서 Redis 캐시 미스 시 티켓 정보 조회용
 * 
 * Write-Through 패턴으로 인해 제거된 API:
 * - PUT /api/couples/ticket (자동 동기화로 대체)
 * - GET /api/couples/{coupleId}/tickets (사용 안함)
 * - POST /api/couples/{coupleId}/tickets/use (사용 안함)
 * - POST /api/couples/{coupleId}/tickets/add (사용 안함)
 * - GET /api/couples/{coupleId}/tickets/info (사용 안함)
 * - PUT /api/couples/{coupleId}/tickets (사용 안함)
 * 
 * Write-Through 패턴:
 * Gateway에서 Redis에 티켓 정보를 쓰면 자동으로 Auth Service DB에 동기화됩니다.
 * Redis Stream을 통해 비동기적으로 처리되므로 성능에 영향을 주지 않습니다.
 */
@Slf4j
public class TicketController {
    // 모든 티켓 관련 API는 CouplesController로 이동되었습니다.
    // Write-Through 패턴으로 자동 동기화되므로 별도 API가 불필요합니다.
}
