package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("polls")
public class Poll {
    @Id
    private Long id;
    private String chatId;
    private String question;
    private Boolean allowMultiple;
    private Boolean allowUserOptions;
    private Boolean anonymous;
    private LocalDateTime expiresAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private String msgId;

    public Poll() {}

    public Poll(Long id, String chatId, String question, Boolean allowMultiple, Boolean allowUserOptions,
                Boolean anonymous, LocalDateTime expiresAt, String createdBy, LocalDateTime createdAt, String msgId) {
        this.id = id;
        this.chatId = chatId;
        this.question = question;
        this.allowMultiple = allowMultiple;
        this.allowUserOptions = allowUserOptions;
        this.anonymous = anonymous;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.msgId = msgId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Boolean getAllowMultiple() {
        return allowMultiple;
    }

    public void setAllowMultiple(Boolean allowMultiple) {
        this.allowMultiple = allowMultiple;
    }

    public Boolean getAllowUserOptions() {
        return allowUserOptions;
    }

    public void setAllowUserOptions(Boolean allowUserOptions) {
        this.allowUserOptions = allowUserOptions;
    }

    public Boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
