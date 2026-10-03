package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PollUpdateDTO {
    private String type; // "poll-update" or "poll"
    private String chatid;
    private String msgid;
    private Long poll_id;
    private String question;
    private Boolean allow_multiple;
    private Boolean allow_user_options;
    private Boolean anonymous;
    private Boolean is_expired;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime expires_at;

    private Integer total_voters;
    private Integer total_members;
    private List<PollOptionDTO> options;
    private List<Long> user_votes;

    public PollUpdateDTO() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getChatid() {
        return chatid;
    }

    public void setChatid(String chatid) {
        this.chatid = chatid;
    }

    public String getMsgid() {
        return msgid;
    }

    public void setMsgid(String msgid) {
        this.msgid = msgid;
    }

    public Long getPoll_id() {
        return poll_id;
    }

    public void setPoll_id(Long poll_id) {
        this.poll_id = poll_id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Boolean getAllow_multiple() {
        return allow_multiple;
    }

    public void setAllow_multiple(Boolean allow_multiple) {
        this.allow_multiple = allow_multiple;
    }

    public Boolean getAllow_user_options() {
        return allow_user_options;
    }

    public void setAllow_user_options(Boolean allow_user_options) {
        this.allow_user_options = allow_user_options;
    }

    public Boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public Boolean getIs_expired() {
        return is_expired;
    }

    public void setIs_expired(Boolean is_expired) {
        this.is_expired = is_expired;
    }

    public LocalDateTime getExpires_at() {
        return expires_at;
    }

    public void setExpires_at(LocalDateTime expires_at) {
        this.expires_at = expires_at;
    }

    public Integer getTotal_voters() {
        return total_voters;
    }

    public void setTotal_voters(Integer total_voters) {
        this.total_voters = total_voters;
    }

    public Integer getTotal_members() {
        return total_members;
    }

    public void setTotal_members(Integer total_members) {
        this.total_members = total_members;
    }

    public List<PollOptionDTO> getOptions() {
        return options;
    }

    public void setOptions(List<PollOptionDTO> options) {
        this.options = options;
    }

    public List<Long> getUser_votes() {
        return user_votes;
    }

    public void setUser_votes(List<Long> user_votes) {
        this.user_votes = user_votes;
    }
}
