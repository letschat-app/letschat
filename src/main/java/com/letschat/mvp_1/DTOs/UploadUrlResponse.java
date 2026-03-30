package com.letschat.mvp_1.DTOs;

public class UploadUrlResponse {
    private String mainKey;
    private String thumbKey;
    private String mainPutUrl;
    private String thumbPutUrl;

    public UploadUrlResponse(){}
    public UploadUrlResponse(String mainKey,String thumbkey,String mainPutUrl,String thumbPutUrl){
        this.mainKey=mainKey;
        this.thumbKey=thumbkey;
        this.mainPutUrl=mainPutUrl;
        this.thumbPutUrl=thumbPutUrl;
    }

    public String getMainKey() {
        return mainKey;
    }
    public void setMainKey(String mainKey) {
        this.mainKey = mainKey;
    }

    public String getThumbKey() {
        return thumbKey;
    }
    public void setThumbKey(String thumbKey) {
        this.thumbKey = thumbKey;
    }

    public String getMainPutUrl() {
        return mainPutUrl;
    }
    public void setMainPutUrl(String mainPutUrl) {
        this.mainPutUrl = mainPutUrl;
    }

    public String getThumbPutUrl() {
        return thumbPutUrl;
    }
    public void setThumbPutUrl(String thumbPutUrl) {
        this.thumbPutUrl = thumbPutUrl;
    }
}
