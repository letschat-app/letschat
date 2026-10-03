package com.letschat.mvp_1.DTOs;

public class PulseCreateRequest {
    private String type;         // "text", "image", "video", "audio", "image_audio"
    private String content;      // text content or caption
    private Long pulseMediaId;   // reference to PulseMediaInfo
    private String bgColor;      // hex color
    private String fontStyle;    // e.g. "bold", "sans-serif"
    private String visibility;   // "chats_only", "everyone"

    public PulseCreateRequest() {}

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public Long getPulseMediaId() {
        return pulseMediaId;
    }
    public void setPulseMediaId(Long pulseMediaId) {
        this.pulseMediaId = pulseMediaId;
    }

    public String getBgColor() {
        return bgColor;
    }
    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getFontStyle() {
        return fontStyle;
    }
    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }

    public String getVisibility() {
        return visibility;
    }
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}
