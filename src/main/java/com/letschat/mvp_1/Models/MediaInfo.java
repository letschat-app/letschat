package com.letschat.mvp_1.Models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"MediaInfo\"")
public class MediaInfo {

    @Id
    private Long mediaId;
    private String fileName;
    private String fileType;
    private String mimeType;
    private String fileHash;
    @Column("file_key")
    private String fileKey;
    private String thumbnailKey;
    private Long fileSize;
    private Instant createdAt;
    private String userId;
    private Integer noOfPages;
    private Integer duration;

    public MediaInfo(){}
    public MediaInfo(Long mediaId, String fileName, String fileType, String mimeType,
                 String fileHash, String fileKey, String thumbnailKey,
                 Long fileSize, Instant createdAt, String userId,
                 Integer noOfPages, Integer duration) {

        this.mediaId = mediaId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.fileHash = fileHash;
        this.fileKey = fileKey;
        this.thumbnailKey = thumbnailKey;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
        this.userId = userId;
        this.noOfPages = noOfPages;
        this.duration = duration;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getThumbnailKey() {
        return thumbnailKey;
    }

    public void setThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getNoOfPages() {
        return noOfPages;
    }

    public void setNoOfPages(Integer noOfPages) {
        this.noOfPages = noOfPages;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}