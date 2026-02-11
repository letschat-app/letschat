package com.letschat.mvp_1.Models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("MediaInfo")
public class MediaInfo {
    @Id
    private Long mediaId;
    private String fileName;
    private String fileType;
    private String mimeType;
    private String fileHash;
    private String fileKey;
    private String thumbnailKey;
    private Long fileSize;
    private Instant createdAt;
    private String userId;

    
}
