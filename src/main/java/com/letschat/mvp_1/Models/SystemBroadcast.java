package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"SystemBroadcast\"")
public class SystemBroadcast {

    @Id
    @Column("id")
    private Long id;

    @Column("msg_id")
    private String msgId;

    @Column("type")
    private String type; // 'text', 'image', 'video', 'document', 'audio'

    @Column("content")
    private String content;

    @Column("caption")
    private String caption;

    @Column("created_at")
    private LocalDateTime createdAt;

    public SystemBroadcast() {}

    public SystemBroadcast(String msgId, String type, String content, String caption, LocalDateTime createdAt) {
        this.msgId = msgId;
        this.type = type != null ? type : "text";
        this.content = content;
        this.caption = caption;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getType() {
        return type != null ? type : "text";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
