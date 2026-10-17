package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Repositories.PulseInfoRepo;
import com.letschat.mvp_1.Repositories.PulseMediaRepo;
import com.letschat.mvp_1.Repositories.PulseViewHistoryRepo;

import reactor.core.publisher.Mono;

@Component
public class PulsePurgeService {

    private final PulseMediaRepo pulseMediaRepo;
    private final PulseInfoRepo pulseInfoRepo;
    private final PulseViewHistoryRepo viewHistoryRepo;

    @Value("${r2.bucket:letschat-media}")
    private String bucketName;

    public PulsePurgeService(PulseMediaRepo pulseMediaRepo, PulseInfoRepo pulseInfoRepo,
                             PulseViewHistoryRepo viewHistoryRepo) {
        this.pulseMediaRepo = pulseMediaRepo;
        this.pulseInfoRepo = pulseInfoRepo;
        this.viewHistoryRepo = viewHistoryRepo;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void executePurge() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("[PULSE PURGE] Starting 6-Hour Cleanup for 24-hr expired pulse media at " + now);
        LocalDateTime cutoff = now.minusDays(1).withNano(0);

        // Phase 1: Hard delete Day 2 expired pulses & view history
        pulseInfoRepo.findExpiredPulsesOlderThan(cutoff)
            .flatMap(pulse -> viewHistoryRepo.deleteByPulseId(pulse.getPulseId())
                    .then(pulseInfoRepo.deleteById(pulse.getPulseId()))
            )
            .then(
                // Phase 2: Delete expired pulse media from Cloud R2 & mark expired
                pulseInfoRepo.findExpiredPulsesWithMedia(now)
                    .flatMap(pulse -> {
                        if (pulse.getPulseMediaId() == null) {
                            return pulseInfoRepo.markExpired(pulse.getPulseId());
                        }

                        return pulseMediaRepo.findById(pulse.getPulseMediaId())
                            .flatMap(media -> pulseMediaRepo.deleteById(media.getPulseMediaId())
                                    .then(pulseInfoRepo.markExpired(pulse.getPulseId()))
                            )
                            .switchIfEmpty(pulseInfoRepo.markExpired(pulse.getPulseId()));
                    })
                    .then()
            )
            .doOnSuccess(v -> System.out.println("[PULSE PURGE] Complete! Expired 24-hr pulse media purged."))
            .doOnError(e -> System.out.println("[PULSE PURGE] Error: " + e.getMessage()))
            .subscribe();
    }
}
