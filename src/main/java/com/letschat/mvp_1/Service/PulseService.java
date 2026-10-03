package com.letschat.mvp_1.Service;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.letschat.mvp_1.DTOs.ContactPulseGroupDTO;
import com.letschat.mvp_1.DTOs.MyPulseDetailDTO;
import com.letschat.mvp_1.DTOs.MyPulsesResponseDTO;
import com.letschat.mvp_1.DTOs.PulseCreateRequest;
import com.letschat.mvp_1.DTOs.PulseFeedItemDTO;
import com.letschat.mvp_1.DTOs.PulseFeedResponseDTO;
import com.letschat.mvp_1.DTOs.PulseSaveMediaRequest;
import com.letschat.mvp_1.DTOs.PulseUploadUrlRequest;
import com.letschat.mvp_1.DTOs.PulseUploadUrlResponse;
import com.letschat.mvp_1.Models.PulseInfo;
import com.letschat.mvp_1.Models.PulseMediaInfo;
import com.letschat.mvp_1.Models.UserPulseStreak;
import com.letschat.mvp_1.Repositories.PulseInfoRepo;
import com.letschat.mvp_1.Repositories.PulseMediaRepo;
import com.letschat.mvp_1.Repositories.PulseViewHistoryRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.Repositories.UserPulseStreakRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class PulseService {

    private final S3Presigner s3Presigner;
    private final PulseInfoRepo pulseInfoRepo;
    private final PulseMediaRepo pulseMediaRepo;
    private final PulseViewHistoryRepo viewHistoryRepo;
    private final UserPulseStreakRepo streakRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final UserInfoRepo userInfoRepo;

    @Value("${r2.bucket:letschat-media}")
    private String bucketName;

    @Value("${r2.endpoint:https://pub-r2.cloudflarestorage.com}")
    private String r2PublicEndpoint;

    public PulseService(S3Presigner s3Presigner, PulseInfoRepo pulseInfoRepo, PulseMediaRepo pulseMediaRepo,
                        PulseViewHistoryRepo viewHistoryRepo, UserPulseStreakRepo streakRepo,
                        UserChatInfoRepo userChatInfoRepo, UserInfoRepo userInfoRepo) {
        this.s3Presigner = s3Presigner;
        this.pulseInfoRepo = pulseInfoRepo;
        this.pulseMediaRepo = pulseMediaRepo;
        this.viewHistoryRepo = viewHistoryRepo;
        this.streakRepo = streakRepo;
        this.userChatInfoRepo = userChatInfoRepo;
        this.userInfoRepo = userInfoRepo;
    }

    public Mono<PulseUploadUrlResponse> generateUploadUrls(PulseUploadUrlRequest request) {
        return Mono.fromSupplier(() -> {
            PulseUploadUrlResponse response = new PulseUploadUrlResponse();

            if (request.isNeedsImage()) {
                String key = "temp/pulses/images/" + UUID.randomUUID() + (request.getImageMime() != null && request.getImageMime().contains("png") ? ".png" : ".jpg");
                String putUrl = createPresignedPutUrl(key, request.getImageMime() != null ? request.getImageMime() : "image/jpeg");
                response.setImageFileKey(key);
                response.setImagePutUrl(putUrl);
            }

            if (request.isNeedsAudio()) {
                String key = "temp/pulses/audios/" + UUID.randomUUID() + ".mp3";
                String putUrl = createPresignedPutUrl(key, request.getAudioMime() != null ? request.getAudioMime() : "audio/mp3");
                response.setAudioFileKey(key);
                response.setAudioPutUrl(putUrl);
            }

            if (request.isNeedsVideo()) {
                String key = "temp/pulses/videos/" + UUID.randomUUID() + ".mp4";
                String putUrl = createPresignedPutUrl(key, request.getVideoMime() != null ? request.getVideoMime() : "video/mp4");
                response.setVideoFileKey(key);
                response.setVideoPutUrl(putUrl);
            }

            return response;
        });
    }

    private String createPresignedPutUrl(String key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    public Mono<Long> saveMedia(PulseSaveMediaRequest request) {
        PulseMediaInfo media = new PulseMediaInfo();
        media.setImageFileKey(request.getImageFileKey());
        media.setAudioFileKey(request.getAudioFileKey());
        media.setVideoFileKey(request.getVideoFileKey());
        media.setCreatedAt(LocalDateTime.now());
        return pulseMediaRepo.save(media).map(PulseMediaInfo::getPulseMediaId);
    }

    public Mono<PulseInfo> createPulse(PulseCreateRequest request, String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(24);
        LocalDate today = LocalDate.now();

        // 🔥 Streak Calculation with gradual decay (-1 per missed day down to min 0)
        Mono<UserPulseStreak> updateStreakMono = streakRepo.findById(userId)
                .defaultIfEmpty(new UserPulseStreak(userId, 0, null))
                .flatMap(streakObj -> {
                    LocalDate lastDate = streakObj.getLastPulseDate();
                    int newStreak;

                    if (lastDate == null) {
                        newStreak = 1;
                    } else {
                        long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(lastDate, today);
                        if (daysPassed == 0) {
                            int current = streakObj.getCurrentStreak() != null ? streakObj.getCurrentStreak() : 1;
                            newStreak = Math.max(1, current);
                        } else if (daysPassed == 1) {
                            int current = streakObj.getCurrentStreak() != null ? streakObj.getCurrentStreak() : 0;
                            newStreak = current + 1;
                        } else {
                            int current = streakObj.getCurrentStreak() != null ? streakObj.getCurrentStreak() : 0;
                            long missedDays = daysPassed - 1;
                            int decayed = (int) Math.max(0, current - missedDays);
                            newStreak = decayed + 1;
                        }
                    }

                    return streakRepo.upsertStreak(userId, newStreak, today);
                });

        PulseInfo pulse = new PulseInfo();
        pulse.setUserId(userId);
        pulse.setType(request.getType() != null ? request.getType() : "text");
        pulse.setContent(request.getContent());
        pulse.setPulseMediaId(request.getPulseMediaId());
        pulse.setBgColor(request.getBgColor());
        pulse.setFontStyle(request.getFontStyle());
        pulse.setVisibility(request.getVisibility() != null ? request.getVisibility() : "chats_only");
        pulse.setCreatedAt(now);
        pulse.setExpiresAt(expiresAt);
        pulse.setStatus("active");

        return updateStreakMono.then(pulseInfoRepo.save(pulse));
    }

    public Mono<MyPulsesResponseDTO> getMyPulses(String userId) {
        Mono<Integer> streakMono = streakRepo.findById(userId)
                .map(s -> s.getEffectiveStreak(LocalDate.now()))
                .defaultIfEmpty(0);

        return Mono.zip(streakMono, pulseInfoRepo.findMyPulses(userId).collectList())
                .flatMap(tuple -> {
                    int streak = tuple.getT1();
                    List<PulseInfo> pulseList = tuple.getT2();

                    return Flux.fromIterable(pulseList)
                            .flatMap(pulse -> {
                                MyPulseDetailDTO dto = new MyPulseDetailDTO();
                                dto.setPulseId(pulse.getPulseId());
                                dto.setType(pulse.getType());
                                dto.setContent(pulse.getContent());
                                dto.setBgColor(pulse.getBgColor());
                                dto.setFontStyle(pulse.getFontStyle());
                                dto.setVisibility(pulse.getVisibility());
                                dto.setCreatedAt(pulse.getCreatedAt());
                                dto.setExpiresAt(pulse.getExpiresAt());
                                dto.setStatus(pulse.getStatus());

                                boolean isExpired = "expired".equals(pulse.getStatus());

                                Mono<MyPulseDetailDTO> mediaMono = (isExpired || pulse.getPulseMediaId() == null) ?
                                        Mono.just(dto) :
                                        pulseMediaRepo.findById(pulse.getPulseMediaId())
                                                .doOnNext(m -> {
                                                    dto.setImageUrl(m.getImageFileKey() != null ? formatPublicUrl(m.getImageFileKey()) : null);
                                                    dto.setAudioUrl(m.getAudioFileKey() != null ? formatPublicUrl(m.getAudioFileKey()) : null);
                                                    dto.setVideoUrl(m.getVideoFileKey() != null ? formatPublicUrl(m.getVideoFileKey()) : null);
                                                }).thenReturn(dto);

                                return mediaMono.flatMap(d ->
                                        viewHistoryRepo.findViewersForPulse(pulse.getPulseId())
                                                .collectList()
                                                .doOnNext(viewersList -> {
                                                    d.setTotalViews(viewersList.size());
                                                    d.setViewers(viewersList);
                                                })
                                                .thenReturn(d)
                                );
                            })
                            .collectList()
                            .map(myPulses -> {
                                MyPulsesResponseDTO response = new MyPulsesResponseDTO();
                                response.setUserId(userId);
                                response.setPulseStreak(streak);
                                response.setPulses(myPulses);
                                response.setActiveCount((int) myPulses.stream().filter(p -> "active".equals(p.getStatus())).count());
                                response.setExpiredCount((int) myPulses.stream().filter(p -> "expired".equals(p.getStatus())).count());
                                return response;
                            });
                });
    }

    public Mono<PulseFeedResponseDTO> getContactsFeed(String viewerUserId) {
        return userChatInfoRepo.findContactUserIds(viewerUserId)
                .collectList()
                .flatMap(contactIds -> {
                    if (contactIds.isEmpty()) {
                        PulseFeedResponseDTO empty = new PulseFeedResponseDTO();
                        empty.setViewerUserId(viewerUserId);
                        empty.setContactsCount(0);
                        empty.setFeed(List.of());
                        return Mono.just(empty);
                    }

                    LocalDateTime now = LocalDateTime.now();
                    return pulseInfoRepo.findActiveContactsPulses(contactIds, now)
                            .flatMap(pulse -> {
                                PulseFeedItemDTO item = new PulseFeedItemDTO();
                                item.setPulseId(pulse.getPulseId());
                                item.setType(pulse.getType());
                                item.setContent(pulse.getContent());
                                item.setBgColor(pulse.getBgColor());
                                item.setFontStyle(pulse.getFontStyle());
                                item.setVisibility(pulse.getVisibility());
                                item.setCreatedAt(pulse.getCreatedAt());
                                item.setExpiresAt(pulse.getExpiresAt());

                                Mono<PulseFeedItemDTO> mediaMono = pulse.getPulseMediaId() == null ?
                                        Mono.just(item) :
                                        pulseMediaRepo.findById(pulse.getPulseMediaId())
                                                .doOnNext(m -> {
                                                    item.setImageUrl(m.getImageFileKey() != null ? formatPublicUrl(m.getImageFileKey()) : null);
                                                    item.setAudioUrl(m.getAudioFileKey() != null ? formatPublicUrl(m.getAudioFileKey()) : null);
                                                    item.setVideoUrl(m.getVideoFileKey() != null ? formatPublicUrl(m.getVideoFileKey()) : null);
                                                }).thenReturn(item);

                                return mediaMono.flatMap(i ->
                                        viewHistoryRepo.hasUserViewed(pulse.getPulseId(), viewerUserId)
                                                .doOnNext(i::setHasSeen)
                                                .thenReturn(i)
                                );
                            })
                            .collectList()
                            .flatMap(items -> {
                                Map<String, List<PulseFeedItemDTO>> grouped = items.stream()
                                        .collect(Collectors.groupingBy(i -> i.getPulseId().toString())); // Temporary grouping handle

                                return Flux.fromIterable(items)
                                        .groupBy(PulseFeedItemDTO::getPulseId) // We group by pulse user
                                        .collectList()
                                        .flatMap(unused -> buildContactGroupFeed(items, viewerUserId));
                            });
                });
    }

    private Mono<PulseFeedResponseDTO> buildContactGroupFeed(List<PulseFeedItemDTO> allItems, String viewerUserId) {
        // Find creator user for each item
        return Flux.fromIterable(allItems)
                .flatMap(item -> pulseInfoRepo.findById(item.getPulseId()).map(p -> Map.entry(p.getUserId(), item)))
                .collectList()
                .flatMap(entries -> {
                    Map<String, List<PulseFeedItemDTO>> userMap = new HashMap<>();
                    for (var entry : entries) {
                        userMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
                    }

                    return Flux.fromIterable(userMap.entrySet())
                            .flatMap(entry -> {
                                String contactId = entry.getKey();
                                List<PulseFeedItemDTO> pulses = entry.getValue();

                                Mono<String> nameMono = userInfoRepo.findById(contactId).map(u -> u.getPublicName()).defaultIfEmpty(contactId);
                                Mono<String> profileMono = userInfoRepo.findById(contactId).map(u -> u.getUserProfilePath() != null ? u.getUserProfilePath() : "").defaultIfEmpty("");
                                Mono<Integer> streakMono = streakRepo.findById(contactId).map(s -> s.getEffectiveStreak(LocalDate.now())).defaultIfEmpty(0);

                                return Mono.zip(nameMono, profileMono, streakMono)
                                        .map(tuple -> {
                                            ContactPulseGroupDTO group = new ContactPulseGroupDTO();
                                            group.setContactUserId(contactId);
                                            group.setContactName(tuple.getT1());
                                            group.setContactProfile(tuple.getT2());
                                            group.setPulseStreak(tuple.getT3());
                                            group.setPulses(pulses);
                                            group.setAllSeen(pulses.stream().allMatch(PulseFeedItemDTO::isHasSeen));
                                            return group;
                                        });
                            })
                            .collectList()
                            .map(groups -> {
                                PulseFeedResponseDTO response = new PulseFeedResponseDTO();
                                response.setViewerUserId(viewerUserId);
                                response.setContactsCount(groups.size());
                                response.setFeed(groups);
                                return response;
                            });
                });
    }

    public Mono<Void> recordView(Long pulseId, String viewerUserId) {
        return viewHistoryRepo.recordView(pulseId, viewerUserId, LocalDateTime.now());
    }

    public Mono<Map<String, Object>> getPulseViews(Long pulseId, String userId) {
        return pulseInfoRepo.findById(pulseId)
                .flatMap(pulse -> {
                    if (!pulse.getUserId().equals(userId)) {
                        return Mono.just(Map.<String, Object>of(
                            "error", "Unauthorized: Only the pulse owner can view who watched this pulse"
                        ));
                    }

                    return viewHistoryRepo.findViewersForPulse(pulseId)
                            .collectList()
                            .map(viewers -> Map.of(
                                "pulseId", pulseId,
                                "totalViews", viewers.size(),
                                "viewers", viewers
                            ));
                })
                .defaultIfEmpty(Map.of("error", "Pulse not found"));
    }

    public Mono<Map<String, Object>> getAllActivePulseViews(String userId) {
        return pulseInfoRepo.findMyPulses(userId)
                .filter(p -> "active".equals(p.getStatus()))
                .flatMap(pulse -> viewHistoryRepo.findViewersForPulse(pulse.getPulseId())
                        .collectList()
                        .map(viewers -> Map.of(
                                "pulseId", pulse.getPulseId(),
                                "type", pulse.getType(),
                                "content", pulse.getContent() != null ? pulse.getContent() : "",
                                "createdAt", pulse.getCreatedAt(),
                                "expiresAt", pulse.getExpiresAt(),
                                "totalViews", viewers.size(),
                                "viewers", viewers
                        ))
                )
                .collectList()
                .map(activeViews -> Map.of(
                        "userId", userId,
                        "activePulsesCount", activeViews.size(),
                        "activePulseViews", activeViews
                ));
    }

    private String formatPublicUrl(String key) {
        if (key == null) return null;
        if (key.startsWith("http")) return key;
        return r2PublicEndpoint + "/" + key;
    }
}
