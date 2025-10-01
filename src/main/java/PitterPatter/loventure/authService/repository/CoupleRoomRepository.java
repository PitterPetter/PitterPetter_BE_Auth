package PitterPatter.loventure.authService.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CoupleRoomRepository extends JpaRepository<CoupleRoom, String> {

    @Query("SELECT c FROM CoupleRoom c WHERE c.inviteCode = :inviteCode")
    Optional<CoupleRoom> findByInviteCode(@Param("inviteCode") String inviteCode);

    @Query("SELECT c FROM CoupleRoom c WHERE c.coupleId = :coupleId")
    Optional<CoupleRoom> findByCoupleId(@Param("coupleId") String coupleId);

    Optional<CoupleRoom> findByCreatorUserIdAndStatus(String creatorUserId, CoupleRoom.CoupleStatus status);

    Optional<CoupleRoom> findByPartnerUserIdAndStatus(String partnerUserId, CoupleRoom.CoupleStatus status);


    boolean existsByInviteCode(String inviteCode);

    boolean existsByCreatorUserIdAndStatus(String creatorUserId, CoupleRoom.CoupleStatus status);

    boolean existsByPartnerUserIdAndStatus(String partnerUserId, CoupleRoom.CoupleStatus status);
}

