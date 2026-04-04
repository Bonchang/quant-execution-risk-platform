package com.bonchang.qerp.appuser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByIdAndActiveTrue(Long id);

    List<AppUser> findByAuthTypeAndLastLoginAtBeforeAndActiveTrue(AppUserAuthType authType, LocalDateTime threshold);
}
