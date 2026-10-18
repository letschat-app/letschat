package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Service;

import com.letschat.mvp_1.DTOs.UsernameCheckResultDTO;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Mono;

@Service
public class UsernameValidationService {

    private final UserInfoRepo userInfoRepo;

    public UsernameValidationService(UserInfoRepo userInfoRepo) {
        this.userInfoRepo = userInfoRepo;
    }

    public Mono<UsernameCheckResultDTO> checkUsername(String username, String excludeUserId) {
        if (username == null || username.trim().length() < 3) {
            return Mono.just(new UsernameCheckResultDTO(
                username != null ? username : "",
                10.0,
                "BLOCK",
                "Username must be at least 3 characters long"
            ));
        }

        String rawClean = username.trim();
        String normInput = UsernameSimilarityEvaluator.normalize(rawClean);

        if (UsernameSimilarityEvaluator.isReserved(rawClean)) {
            return Mono.just(new UsernameCheckResultDTO(
                rawClean,
                10.0,
                "BLOCK",
                "Username already taken"
            ));
        }

        if (normInput.length() < 2) {
            return Mono.just(new UsernameCheckResultDTO(
                rawClean,
                10.0,
                "BLOCK",
                "Username contains invalid characters"
            ));
        }

        // 1. Direct exact match check (case-insensitive)
        return userInfoRepo.existsExactName(rawClean.toLowerCase(), excludeUserId)
            .flatMap(exactExists -> {
                if (exactExists) {
                    return Mono.just(new UsernameCheckResultDTO(
                        rawClean,
                        10.0,
                        "BLOCK",
                        "Username already taken"
                    ));
                }

                // 2. Fuzzy similarity check
                String rawPrefix = rawClean.length() >= 2 ? rawClean.substring(0, 2).toLowerCase() : rawClean.toLowerCase();
                String normPrefix = normInput.length() >= 2 ? normInput.substring(0, 2) : normInput;
                int minLen = Math.max(2, rawClean.length() - 4);
                int maxLen = rawClean.length() + 4;

                return userInfoRepo.findSimilarCandidates(rawPrefix, normPrefix, minLen, maxLen, excludeUserId)
                    .map(candidate -> {
                        String normCandidate = UsernameSimilarityEvaluator.normalize(candidate);
                        return UsernameSimilarityEvaluator.computeScore(normInput, normCandidate);
                    })
                    .reduce(0.0, Math::max)
                    .map(maxScore -> {
                        String decision;
                        String message;

                        if (maxScore >= 10.0) {
                            decision = "BLOCK";
                            message = "Username already taken";
                        } else if (maxScore >= UsernameSimilarityEvaluator.BLOCK_THRESHOLD) {
                            decision = "BLOCK";
                            message = "Too similar to an existing username. Please choose another username.";
                        } else if (maxScore >= UsernameSimilarityEvaluator.WARN_THRESHOLD) {
                            decision = "WARN";
                            message = "Username is somewhat similar to an existing username. Confirm to proceed or pick an alternative.";
                        } else {
                            decision = "ALLOW";
                            message = "Username is available";
                        }

                        return new UsernameCheckResultDTO(rawClean, maxScore, decision, message);
                    });
            });
    }
}
