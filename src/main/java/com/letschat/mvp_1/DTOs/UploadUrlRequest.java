package com.letschat.mvp_1.DTOs;

public class UploadUrlRequest {
    private String mime;
    private String hash;

    public UploadUrlRequest(){}
    public UploadUrlRequest(String mime,String hash){
        this.mime=mime;
        this.hash=hash;
    }

    public void setMime(String mime){
        this.mime=mime;
    }
    public String getMime(){
        return mime;
    }

    public void setHash(String hash){
        this.hash=hash;
    }
    public String getHash(){
        return hash;
    }
}
