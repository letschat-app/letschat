package com.letschat.mvp_1.DTOs;

public class UploadUrlRequest {
    private String mime;

    public UploadUrlRequest(){}
    public UploadUrlRequest(String mime){
        this.mime=mime;
    }

    public void setMime(String mime){
        this.mime=mime;
    }

    public String getMime(){
        return mime;
    }
}
