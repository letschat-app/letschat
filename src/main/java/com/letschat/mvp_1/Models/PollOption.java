package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("poll_options")
public class PollOption {
    @Id
    private Long id;
    private Long pollId;
    private String optionText;
    private String createdBy;
    private LocalDateTime createdAt;

    public PollOption() {}

    public PollOption(Long id, Long pollId, String optionText, String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.pollId = pollId;
        this.optionText = optionText;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPollId() {
        return pollId;
    }

    public void setPollId(Long pollId) {
        this.pollId = pollId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
