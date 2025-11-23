package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("UserCommunityRelation")
public class UserCommunityRelation {
    @Id
    @Column("Id")
    private Long id;
    @Column("UserId")
    private String userId;
    @Column("CommunityId")
    private String communityId;
    @Column("At")
    private LocalDateTime at;
    @Column("Relation")
    private String relation;

    public UserCommunityRelation(){}
    public UserCommunityRelation(Long id, String userId, String communityId, LocalDateTime at, String relation) {
        this.id = id;
        this.userId = userId;
        this.communityId = communityId;
        this.at = at;
        this.relation = relation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCommunityId() {
        return communityId;
    }

    public void setCommunityId(String communityId) {
        this.communityId = communityId;
    }

    public LocalDateTime getAt() {
        return at;
    }

    public void setAt(LocalDateTime at) {
        this.at = at;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }
}
