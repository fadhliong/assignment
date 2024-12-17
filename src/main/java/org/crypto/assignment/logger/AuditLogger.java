package org.crypto.assignment.logger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class AuditLogger {
    public void log(String action, Long userId, Long resourceId) {
        log.info("Audit: action={}, userId={}, resourceId={}, timestamp={}",
                action, userId, resourceId, LocalDateTime.now());
    }
}
