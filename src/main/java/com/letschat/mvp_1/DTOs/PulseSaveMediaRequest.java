package com.letschat.mvp_1.DTOs;

public class PulseSaveMediaRequest {
    private String imageFileKey;
    private String audioFileKey;
    private String videoFileKey;

    public PulseSaveMediaRequest() {}

    public String getImageFileKey() {
        return imageFileKey;
    }
    public void setImageFileKey(String imageFileKey) {
        this.imageFileKey = imageFileKey;
    }

    public String getAudioFileKey() {
        return audioFileKey;
    }
    public void setAudioFileKey(String audioFileKey) {
        this.audioFileKey = audioFileKey;
    }

    public String getVideoFileKey() {
        return videoFileKey;
    }
    public void setVideoFileKey(String videoFileKey) {
        this.videoFileKey = videoFileKey;
    }
}
