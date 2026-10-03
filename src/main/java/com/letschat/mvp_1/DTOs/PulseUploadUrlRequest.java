package com.letschat.mvp_1.DTOs;

public class PulseUploadUrlRequest {
    private boolean needsImage;
    private String imageMime;

    private boolean needsAudio;
    private String audioMime;

    private boolean needsVideo;
    private String videoMime;

    public PulseUploadUrlRequest() {}

    public boolean isNeedsImage() {
        return needsImage;
    }
    public void setNeedsImage(boolean needsImage) {
        this.needsImage = needsImage;
    }

    public String getImageMime() {
        return imageMime;
    }
    public void setImageMime(String imageMime) {
        this.imageMime = imageMime;
    }

    public boolean isNeedsAudio() {
        return needsAudio;
    }
    public void setNeedsAudio(boolean needsAudio) {
        this.needsAudio = needsAudio;
    }

    public String getAudioMime() {
        return audioMime;
    }
    public void setAudioMime(String audioMime) {
        this.audioMime = audioMime;
    }

    public boolean isNeedsVideo() {
        return needsVideo;
    }
    public void setNeedsVideo(boolean needsVideo) {
        this.needsVideo = needsVideo;
    }

    public String getVideoMime() {
        return videoMime;
    }
    public void setVideoMime(String videoMime) {
        this.videoMime = videoMime;
    }
}
