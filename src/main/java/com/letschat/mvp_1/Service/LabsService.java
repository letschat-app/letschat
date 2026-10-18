package com.letschat.mvp_1.Service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.letschat.mvp_1.DTOs.LabsExperimentDTO;
import com.letschat.mvp_1.Models.LabsEarlyAccessOptIn;
import com.letschat.mvp_1.Models.LabsExperiment;
import com.letschat.mvp_1.Models.LabsParticipation;
import com.letschat.mvp_1.Repositories.LabsEarlyAccessOptInRepo;
import com.letschat.mvp_1.Repositories.LabsExperimentRepo;
import com.letschat.mvp_1.Repositories.LabsParticipationRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class LabsService {

    private final LabsExperimentRepo labsExperimentRepo;
    private final LabsParticipationRepo labsParticipationRepo;
    private final LabsEarlyAccessOptInRepo labsEarlyAccessOptInRepo;

    public LabsService(LabsExperimentRepo labsExperimentRepo,
                       LabsParticipationRepo labsParticipationRepo,
                       LabsEarlyAccessOptInRepo labsEarlyAccessOptInRepo) {
        this.labsExperimentRepo = labsExperimentRepo;
        this.labsParticipationRepo = labsParticipationRepo;
        this.labsEarlyAccessOptInRepo = labsEarlyAccessOptInRepo;
    }

    public Mono<List<LabsExperimentDTO>> listExperiments(String userId) {
        Mono<Map<Long, String>> userVotesMono = (userId != null && !userId.isBlank())
                ? labsParticipationRepo.findAllByUserId(userId)
                    .collectMap(LabsParticipation::getExperimentId, LabsParticipation::getIntent)
                : Mono.just(Collections.emptyMap());

        Mono<Set<Long>> userOptInsMono = (userId != null && !userId.isBlank())
                ? labsEarlyAccessOptInRepo.findOptedInExperimentIds(userId).collect(Collectors.toSet())
                : Mono.just(Collections.emptySet());

        return Mono.zip(userVotesMono, userOptInsMono)
                .flatMap(tuple -> {
                    Map<Long, String> userVotes = tuple.getT1();
                    Set<Long> userOptIns = tuple.getT2();

                    return labsExperimentRepo.findAllActive()
                            .flatMap(exp -> {
                                Mono<Long> useMono = labsParticipationRepo.countByExperimentIdAndIntent(exp.getId(), "use").defaultIfEmpty(0L);
                                Mono<Long> buildMono = labsParticipationRepo.countByExperimentIdAndIntent(exp.getId(), "build").defaultIfEmpty(0L);
                                Mono<Long> bothMono = labsParticipationRepo.countByExperimentIdAndIntent(exp.getId(), "both").defaultIfEmpty(0L);

                                return Mono.zip(useMono, buildMono, bothMono)
                                        .map(counts -> {
                                            LabsExperimentDTO dto = new LabsExperimentDTO();
                                            dto.setId(exp.getId());
                                            dto.setTitle(exp.getTitle());
                                            dto.setDescription(exp.getDescription());
                                            dto.setStatus(exp.getStatus());
                                            dto.setUseCount(counts.getT1());
                                            dto.setBuildCount(counts.getT2());
                                            dto.setBothCount(counts.getT3());
                                            dto.setUserVotedIntent(userVotes.get(exp.getId()));
                                            dto.setOptedInEarlyAccess(userOptIns.contains(exp.getId()));
                                            dto.setCreatedAt(exp.getCreatedAt());
                                            return dto;
                                        });
                            })
                            .collectList();
                });
    }

    public Mono<Map<String, Object>> voteIntent(Long experimentId, String userId, String intent) {
        if (intent == null || (!intent.equalsIgnoreCase("use") && !intent.equalsIgnoreCase("build") && !intent.equalsIgnoreCase("both"))) {
            return Mono.error(new IllegalArgumentException("Intent must be 'use', 'build', or 'both'"));
        }

        String cleanIntent = intent.toLowerCase().trim();
        return labsParticipationRepo.upsertVote(experimentId, userId, cleanIntent)
                .thenReturn(Map.<String, Object>of(
                        "status", "success",
                        "experimentId", experimentId,
                        "userId", userId,
                        "intent", cleanIntent
                ));
    }

    public Mono<Map<String, Object>> optInEarlyAccess(Long experimentId, String userId) {
        return labsEarlyAccessOptInRepo.optIn(experimentId, userId)
                .thenReturn(Map.<String, Object>of(
                        "status", "success",
                        "experimentId", experimentId,
                        "userId", userId,
                        "optedIn", true
                ));
    }

    public Mono<LabsExperiment> createExperiment(String title, String description, String status) {
        if (title == null || title.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Title is required"));
        }
        if (description == null || description.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Description is required"));
        }

        LabsExperiment experiment = new LabsExperiment(title.trim(), description.trim(), status != null ? status.trim() : "active");
        return labsExperimentRepo.save(experiment);
    }

    public Mono<Map<String, Object>> getExperimentMetrics(Long experimentId) {
        Mono<Long> useMono = labsParticipationRepo.countByExperimentIdAndIntent(experimentId, "use").defaultIfEmpty(0L);
        Mono<Long> buildMono = labsParticipationRepo.countByExperimentIdAndIntent(experimentId, "build").defaultIfEmpty(0L);
        Mono<Long> bothMono = labsParticipationRepo.countByExperimentIdAndIntent(experimentId, "both").defaultIfEmpty(0L);
        Mono<Long> optInMono = labsEarlyAccessOptInRepo.countByExperimentId(experimentId).defaultIfEmpty(0L);

        return Mono.zip(useMono, buildMono, bothMono, optInMono)
                .map(tuple -> {
                    long use = tuple.getT1();
                    long build = tuple.getT2();
                    long both = tuple.getT3();
                    long totalVotes = use + build + both;
                    long optIns = tuple.getT4();

                    return Map.<String, Object>of(
                            "experimentId", experimentId,
                            "useCount", use,
                            "buildCount", build,
                            "bothCount", both,
                            "totalVotes", totalVotes,
                            "earlyAccessOptIns", optIns
                    );
                });
    }
}
