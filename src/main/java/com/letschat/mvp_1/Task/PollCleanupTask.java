package com.letschat.mvp_1.Task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.letschat.mvp_1.Service.PollService;

@Component
public class PollCleanupTask {

    private final PollService pollService;

    public PollCleanupTask(PollService pollService) {
        this.pollService = pollService;
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupUnlinkedPolls() {
        pollService.deleteUnlinkedPolls()
            .doOnNext(count -> System.out.println("[PollCleanupTask] Purged " + count + " unlinked polls"))
            .doOnError(err -> System.err.println("[PollCleanupTask] Error: " + err.getMessage()))
            .subscribe();
    }
}
