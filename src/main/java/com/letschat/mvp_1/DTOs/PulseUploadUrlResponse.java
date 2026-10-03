package com.letschat.mvp_1.DTOs;

public class PulseUploadUrlResponse {
    private String imagePutUrl;
    private String imageFileKey;

    private String audioPutUrl;
    private String audioFileKey;

    private String videoPutUrl;
    private String videoFileKey;

    public PulseUploadUrlResponse() {}

    public String getImagePutUrl() {
        return imagePutUrl;
    }
    public void setImagePutUrl(String imagePutUrl) {
        this.imagePutUrl = imagePutUrl;
    }

    public String getImageFileKey() {
        return imageFileKey;
    }
    public void setImageFileKey(String imageFileKey) {
        this.imageFileKey = imageFileKey;
    }

    public String getAudioPutUrl() {
        return audioPutUrl;
    }
    public void setAudioPutUrl(String audioPutUrl) {
        this.audioPutUrl = audioPutUrl;
    }

    public String getAudioFileKey() {
        return audioFileKey;
    }
    public void setAudioFileKey(String audioFileKey) {
        this.audioFileKey = audioFileKey;
    }

    public String getVideoPutUrl() {
        return videoPutUrl;
    }
    public void setVideoPutUrl(String videoPutUrl) {
        this.videoPutUrl = videoPutUrl;
    }

    public String getVideoFileKey() {
        return videoFileKey;
    }
    public void setVideoFileKey(String videoFileKey) {
        this.videoFileKey = videoFileKey;
    }
}
