package com.letschat.mvp_1.DTOs;

public class UploadUrlResponse {
    private Boolean exist;
    private String mainKey;
    private String thumbKey;
    private String mainPutUrl;
    private String thumbPutUrl;
    private Long mediaId;

    public UploadUrlResponse(){}
    public UploadUrlResponse(Boolean exist,String mainKey,String thumbkey,String mainPutUrl,String thumbPutUrl,Long mediaId){
        this.exist=exist;
        this.mainKey=mainKey;
        this.thumbKey=thumbkey;
        this.mainPutUrl=mainPutUrl;
        this.thumbPutUrl=thumbPutUrl;
        this.mediaId=mediaId;
    }

    public Boolean getExist(){
        return exist;
    }
    public void setExist(Boolean exist){
        this.exist=exist;
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

    public Long getMediaId(){
        return mediaId;
    }
    public void setMediaId(Long mediaId){
        this.mediaId=mediaId;
    }
}
