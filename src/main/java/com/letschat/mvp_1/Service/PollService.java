package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.letschat.mvp_1.DTOs.*;
import com.letschat.mvp_1.Models.Poll;
import com.letschat.mvp_1.Models.PollOption;
import com.letschat.mvp_1.Models.PollVote;
import com.letschat.mvp_1.Repositories.MessageInfoRepo;
import com.letschat.mvp_1.Repositories.PollOptionRepo;
import com.letschat.mvp_1.Repositories.PollRepo;
import com.letschat.mvp_1.Repositories.PollVoteRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PollService {

    private final PollRepo pollRepo;
    private final PollOptionRepo pollOptionRepo;
    private final PollVoteRepo pollVoteRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final MessageInfoRepo messageInfoRepo;
    private final TransactionalOperator transactionalOperator;
    private final DatabaseClient databaseClient;

    public PollService(PollRepo pollRepo, PollOptionRepo pollOptionRepo, PollVoteRepo pollVoteRepo,
                       UserChatInfoRepo userChatInfoRepo, MessageInfoRepo messageInfoRepo,
                       TransactionalOperator transactionalOperator, DatabaseClient databaseClient) {
        this.pollRepo = pollRepo;
        this.pollOptionRepo = pollOptionRepo;
        this.pollVoteRepo = pollVoteRepo;
        this.userChatInfoRepo = userChatInfoRepo;
        this.messageInfoRepo = messageInfoRepo;
        this.transactionalOperator = transactionalOperator;
        this.databaseClient = databaseClient;
    }

    public Mono<CreatePollResponseDTO> createPoll(String userId, CreatePollRequestDTO dto) {
        if (dto.getchatid() == null || dto.getchatid().isBlank()) {
            return Mono.error(new IllegalArgumentException("chatid is required"));
        }
        if (dto.getquestion() == null || dto.getquestion().trim().isEmpty() || dto.getquestion().length() > 300) {
            return Mono.error(new IllegalArgumentException("question must be between 1 and 300 characters"));
        }
        List<String> options = dto.getoptions();
        if (options == null || options.size() < 2 || options.size() > 10) {
            return Mono.error(new IllegalArgumentException("options count must be between 2 and 10"));
        }

        Set<String> lowerSet = new HashSet<>();
        List<String> cleanOptions = new ArrayList<>();
        for (String opt : options) {
            if (opt == null || opt.trim().isEmpty() || opt.trim().length() > 100) {
                return Mono.error(new IllegalArgumentException("each option must be between 1 and 100 characters"));
            }
            String trimmed = opt.trim();
            if (!lowerSet.add(trimmed.toLowerCase())) {
                return Mono.error(new IllegalArgumentException("duplicate options are not allowed"));
            }
            cleanOptions.add(trimmed);
        }

        LocalDateTime now = LocalDateTime.now();
        if (dto.getexpires_at() != null) {
            if (dto.getexpires_at().isBefore(now)) {
                return Mono.error(new IllegalArgumentException("expires_at must be in the future"));
            }
            if (dto.getexpires_at().isAfter(now.plusDays(30))) {
                return Mono.error(new IllegalArgumentException("expires_at cannot be more than 30 days away"));
            }
        }

        return userChatInfoRepo.joinedOrNot(dto.getchatid(), userId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User is not a member of chatid")))
            .flatMap(cId -> {
                Poll poll = new Poll(null, dto.getchatid(), dto.getquestion().trim(),
                    dto.getallow_multiple(), dto.getallow_user_options(), dto.getanonymous(),
                    Boolean.TRUE.equals(dto.getis_hidden_voting()),
                    dto.getexpires_at(), userId, now, null);

                return pollRepo.save(poll)
                    .flatMap(savedPoll -> {
                        List<PollOption> optionEntities = cleanOptions.stream()
                            .map(text -> new PollOption(null, savedPoll.getId(), text, userId, now))
                            .toList();

                        return pollOptionRepo.saveAll(optionEntities)
                            .collectList()
                            .map(savedOptions -> {
                                List<CreatePollResponseDTO.OptionItem> items = savedOptions.stream()
                                    .map(o -> new CreatePollResponseDTO.OptionItem(o.getId(), o.getOptionText()))
                                    .toList();
                                return new CreatePollResponseDTO(savedPoll.getId(), items);
                            });
                    });
            })
            .as(transactionalOperator::transactional);
    }

    public Mono<Void> linkPollToMessage(Long pollId, String msgId, String senderId, String chatId) {
        return pollRepo.linkPollToMessage(pollId, msgId, senderId, chatId)
            .flatMap(updatedRows -> {
                if (updatedRows == 0) {
                    return Mono.error(new IllegalStateException("Failed to link poll " + pollId + " to msg " + msgId));
                }
                return Mono.empty();
            });
    }

    public Mono<PollUpdateDTO> buildPollUpdateDTO(Long pollId, String msgId, String requestingUserId) {
        return pollRepo.findById(pollId)
            .flatMap(poll -> {
                String effectiveMsgId = msgId != null ? msgId : poll.getMsgId();
                boolean isExpired = poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt());

                Mono<Long> totalMembersMono = databaseClient.sql("SELECT COUNT(*) FROM \"UserChat\" WHERE \"ChatId\" = :chatId AND \"Status\" = 'allowed'")
                    .bind("chatId", poll.getChatId())
                    .map((row, metadata) -> row.get(0, Long.class))
                    .first()
                    .defaultIfEmpty(0L);

                Mono<List<PollOption>> optionsMono = pollOptionRepo.findByPollId(pollId).collectList();
                Mono<List<PollVoteRepo.VoteRecord>> votesMono = pollVoteRepo.findVotesByPollId(pollId).collectList();
                Mono<List<Long>> userVotesMono = requestingUserId != null ?
                    pollVoteRepo.findUserVotes(pollId, requestingUserId).collectList() : Mono.just(Collections.emptyList());

                return Mono.zip(totalMembersMono, optionsMono, votesMono, userVotesMono)
                    .map(tuple -> {
                        long totalMembers = tuple.getT1();
                        List<PollOption> options = tuple.getT2();
                        List<PollVoteRepo.VoteRecord> votes = tuple.getT3();
                        List<Long> userVotes = tuple.getT4();

                        Set<String> uniqueVoters = votes.stream().map(PollVoteRepo.VoteRecord::userId).collect(Collectors.toSet());
                        Map<Long, List<String>> optionVotersMap = new HashMap<>();
                        for (PollVoteRepo.VoteRecord v : votes) {
                            optionVotersMap.computeIfAbsent(v.optionId(), k -> new ArrayList<>()).add(v.userId());
                        }

                        List<PollOptionDTO> optionDTOs = options.stream().map(opt -> {
                            List<String> votersForOpt = optionVotersMap.getOrDefault(opt.getId(), Collections.emptyList());
                            int count = votersForOpt.size();
                            List<String> votersPayload = Boolean.TRUE.equals(poll.getAnonymous()) ? null : votersForOpt;
                            return new PollOptionDTO(opt.getId(), opt.getOptionText(), count, votersPayload);
                        }).toList();

                        PollUpdateDTO dto = new PollUpdateDTO();
                        dto.setType("poll-update");
                        dto.setChatid(poll.getChatId());
                        dto.setMsgid(effectiveMsgId);
                        dto.setPoll_id(poll.getId());
                        dto.setQuestion(poll.getQuestion());
                        dto.setAllow_multiple(poll.getAllowMultiple());
                        dto.setAllow_user_options(poll.getAllowUserOptions());
                        dto.setAnonymous(poll.getAnonymous());
                        dto.setIs_hidden_voting(poll.getIsHiddenVoting());
                        dto.setIs_expired(isExpired);
                        dto.setExpires_at(poll.getExpiresAt());
                        dto.setTotal_voters(uniqueVoters.size());
                        dto.setTotal_members((int) Math.max(2, totalMembers));
                        dto.setOptions(optionDTOs);
                        if (!userVotes.isEmpty()) {
                            dto.setUser_votes(userVotes);
                        }
                        return dto;
                    });
            });
    }

    public Mono<PollUpdateDTO> votePoll(String userId, String chatId, String msgId, List<Long> optionIds, Long parsedPollId) {
        Mono<Poll> pollMono = parsedPollId != null ? pollRepo.findById(parsedPollId) : pollRepo.findByMsgId(msgId);

        return userChatInfoRepo.joinedOrNot(chatId, userId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User is not a member of chatid")))
            .then(pollMono)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Poll not found")))
            .flatMap(poll -> {
                if (poll.getMsgId() == null || !poll.getChatId().equals(chatId)) {
                    return Mono.error(new IllegalArgumentException("Invalid poll message link"));
                }

                if (poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt())) {
                    return Mono.error(new IllegalStateException("Poll has expired"));
                }

                List<Long> cleanOptionIds = optionIds != null ? optionIds.stream().distinct().toList() : Collections.emptyList();

                if (!Boolean.TRUE.equals(poll.getAllowMultiple()) && cleanOptionIds.size() > 1) {
                    return Mono.error(new IllegalArgumentException("Multiple choices are not allowed for this poll"));
                }

                return pollOptionRepo.findByPollId(poll.getId())
                    .collectList()
                    .flatMap(validOptions -> {
                        Set<Long> validOptionIds = validOptions.stream().map(PollOption::getId).collect(Collectors.toSet());
                        for (Long optId : cleanOptionIds) {
                            if (!validOptionIds.contains(optId)) {
                                return Mono.error(new IllegalArgumentException("Option ID " + optId + " does not belong to poll"));
                            }
                        }

                        LocalDateTime now = LocalDateTime.now();
                        List<PollVote> newVotes = cleanOptionIds.stream()
                            .map(optId -> new PollVote(poll.getId(), optId, userId, now))
                            .toList();

                        return pollVoteRepo.deleteByPollIdAndUserId(poll.getId(), userId)
                            .then(newVotes.isEmpty() ? Mono.empty() : pollVoteRepo.saveAll(newVotes).then())
                            .thenReturn(poll.getId());
                    });
            })
            .as(transactionalOperator::transactional)
            .flatMap(pollId -> buildPollUpdateDTO(pollId, msgId, userId));
    }

    public Mono<PollUpdateDTO> addPollOption(String userId, String chatId, String msgId, String optionText, Long parsedPollId) {
        if (optionText == null || optionText.trim().isEmpty() || optionText.trim().length() > 100) {
            return Mono.error(new IllegalArgumentException("option_text must be between 1 and 100 characters"));
        }
        String cleanText = optionText.trim();

        Mono<Poll> pollMono = parsedPollId != null ? pollRepo.findById(parsedPollId) : pollRepo.findByMsgId(msgId);

        return userChatInfoRepo.joinedOrNot(chatId, userId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User is not a member of chatid")))
            .then(pollMono)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Poll not found")))
            .flatMap(poll -> {
                if (!Boolean.TRUE.equals(poll.getAllowUserOptions())) {
                    return Mono.error(new IllegalArgumentException("User-added options are not allowed for this poll"));
                }
                if (poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt())) {
                    return Mono.error(new IllegalStateException("Poll has expired"));
                }

                return pollOptionRepo.countByPollId(poll.getId())
                    .flatMap(count -> {
                        if (count >= 20) {
                            return Mono.error(new IllegalArgumentException("Poll option limit of 20 reached"));
                        }
                        return pollOptionRepo.existsByPollIdAndOptionTextIgnoreCase(poll.getId(), cleanText)
                            .flatMap(exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    return Mono.error(new IllegalArgumentException("Duplicate option text"));
                                }
                                PollOption newOpt = new PollOption(null, poll.getId(), cleanText, userId, LocalDateTime.now());
                                return pollOptionRepo.save(newOpt).thenReturn(poll.getId());
                            });
                    });
            })
            .as(transactionalOperator::transactional)
            .flatMap(pollId -> buildPollUpdateDTO(pollId, msgId, userId));
    }

    public Mono<List<LoadMessageDTO>> enrichPollMessages(List<LoadMessageDTO> msgs, String requestingUserId) {
        if (msgs == null || msgs.isEmpty()) return Mono.just(Collections.emptyList());

        List<String> pollMsgIds = msgs.stream()
            .filter(m -> "poll".equals(m.gettype()) && m.getmsgid() != null)
            .map(LoadMessageDTO::getmsgid)
            .toList();

        if (pollMsgIds.isEmpty()) {
            return Mono.just(msgs);
        }

        return pollRepo.findByMsgIdIn(pollMsgIds).collectList()
            .flatMap(polls -> {
                if (polls.isEmpty()) return Mono.just(msgs);

                Map<String, Poll> pollByMsgId = polls.stream().collect(Collectors.toMap(Poll::getMsgId, p -> p, (p1, p2) -> p1));
                Set<Long> pollIds = polls.stream().map(Poll::getId).collect(Collectors.toSet());
                String chatId = msgs.get(0).getchatid();

                Mono<Long> totalMembersMono = databaseClient.sql("SELECT COUNT(*) FROM \"UserChat\" WHERE \"ChatId\" = :chatId AND \"Status\" = 'allowed'")
                    .bind("chatId", chatId)
                    .map((row, metadata) -> row.get(0, Long.class))
                    .first()
                    .defaultIfEmpty(0L);

                Mono<List<PollOption>> optionsMono = pollOptionRepo.findByPollIdIn(pollIds).collectList();
                Mono<List<PollVoteRepo.VoteRecord>> votesMono = pollVoteRepo.findVotesByPollIdIn(pollIds).collectList();
                Mono<List<PollVoteRepo.UserVoteRecord>> userVotesMono = requestingUserId != null ?
                    pollVoteRepo.findUserVotesByPollIdsAndUserId(pollIds, requestingUserId).collectList() : Mono.just(Collections.emptyList());

                return Mono.zip(totalMembersMono, optionsMono, votesMono, userVotesMono)
                    .map(tuple -> {
                        long totalMembers = tuple.getT1();
                        List<PollOption> options = tuple.getT2();
                        List<PollVoteRepo.VoteRecord> votes = tuple.getT3();
                        List<PollVoteRepo.UserVoteRecord> userVotes = tuple.getT4();

                        Map<Long, List<PollOption>> optionsByPollId = options.stream().collect(Collectors.groupingBy(PollOption::getPollId));
                        Map<Long, List<PollVoteRepo.VoteRecord>> votesByPollId = votes.stream().collect(Collectors.groupingBy(PollVoteRepo.VoteRecord::pollId));
                        Map<Long, List<Long>> userVotesByPollId = userVotes.stream().collect(Collectors.groupingBy(PollVoteRepo.UserVoteRecord::pollId, Collectors.mapping(PollVoteRepo.UserVoteRecord::optionId, Collectors.toList())));

                        for (LoadMessageDTO msg : msgs) {
                            if ("poll".equals(msg.gettype())) {
                                Poll poll = pollByMsgId.get(msg.getmsgid());
                                if (poll != null) {
                                    Long pollId = poll.getId();
                                    boolean isExpired = poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt());

                                    List<PollOption> pollOpts = optionsByPollId.getOrDefault(pollId, Collections.emptyList());
                                    List<PollVoteRepo.VoteRecord> pollVotes = votesByPollId.getOrDefault(pollId, Collections.emptyList());
                                    List<Long> uVotes = userVotesByPollId.getOrDefault(pollId, Collections.emptyList());

                                    Set<String> uniqueVoters = pollVotes.stream().map(PollVoteRepo.VoteRecord::userId).collect(Collectors.toSet());
                                    Map<Long, List<String>> optionVotersMap = new HashMap<>();
                                    for (PollVoteRepo.VoteRecord v : pollVotes) {
                                        optionVotersMap.computeIfAbsent(v.optionId(), k -> new ArrayList<>()).add(v.userId());
                                    }

                                    List<PollOptionDTO> optionDTOs = pollOpts.stream().map(opt -> {
                                        List<String> votersForOpt = optionVotersMap.getOrDefault(opt.getId(), Collections.emptyList());
                                        int count = votersForOpt.size();
                                        List<String> votersPayload = Boolean.TRUE.equals(poll.getAnonymous()) ? null : votersForOpt;
                                        return new PollOptionDTO(opt.getId(), opt.getOptionText(), count, votersPayload);
                                    }).toList();

                                    PollUpdateDTO pollDTO = new PollUpdateDTO();
                                    pollDTO.setType("poll");
                                    pollDTO.setChatid(msg.getchatid());
                                    pollDTO.setMsgid(msg.getmsgid());
                                    pollDTO.setPoll_id(pollId);
                                    pollDTO.setQuestion(poll.getQuestion());
                                    pollDTO.setAllow_multiple(poll.getAllowMultiple());
                                    pollDTO.setAllow_user_options(poll.getAllowUserOptions());
                                    pollDTO.setAnonymous(poll.getAnonymous());
                                    pollDTO.setIs_hidden_voting(poll.getIsHiddenVoting());
                                    pollDTO.setIs_expired(isExpired);
                                    pollDTO.setExpires_at(poll.getExpiresAt());
                                    pollDTO.setTotal_voters(uniqueVoters.size());
                                    pollDTO.setTotal_members((int) Math.max(2, totalMembers));
                                    pollDTO.setOptions(optionDTOs);
                                    if (!uVotes.isEmpty()) {
                                        pollDTO.setUser_votes(uVotes);
                                    }

                                    msg.setpoll(pollDTO);
                                }
                            }
                        }
                        return msgs;
                    });
            });
    }

    public Mono<Integer> deleteUnlinkedPolls() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        return pollRepo.deleteUnlinkedPolls(cutoff);
    }
}
