package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

public class CreatePollRequestDTO {
    private String chatid;
    private String question;
    private Boolean allow_multiple;
    private Boolean allow_user_options;
    private Boolean anonymous;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime expires_at;

    private List<String> options;

    public CreatePollRequestDTO() {}

    public String getchatid() {
        return chatid;
    }

    public void setchatid(String chatid) {
        this.chatid = chatid;
    }

    public String getquestion() {
        return question;
    }

    public void setquestion(String question) {
        this.question = question;
    }

    public Boolean getallow_multiple() {
        return allow_multiple != null ? allow_multiple : false;
    }

    public void setallow_multiple(Boolean allow_multiple) {
        this.allow_multiple = allow_multiple;
    }

    public Boolean getallow_user_options() {
        return allow_user_options != null ? allow_user_options : false;
    }

    public void setallow_user_options(Boolean allow_user_options) {
        this.allow_user_options = allow_user_options;
    }

    public Boolean getanonymous() {
        return anonymous != null ? anonymous : false;
    }

    public void setanonymous(Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public LocalDateTime getexpires_at() {
        return expires_at;
    }

    public void setexpires_at(LocalDateTime expires_at) {
        this.expires_at = expires_at;
    }

    public List<String> getoptions() {
        return options;
    }

    public void setoptions(List<String> options) {
        this.options = options;
    }
}
