// package com.letschat.mvp_1.DTOs;

// import java.time.LocalDateTime;

// import com.fasterxml.jackson.annotation.JsonFormat;
// import com.fasterxml.jackson.annotation.JsonProperty;

// public class ChatUpdateDTO {
//     private String purpose;
//     @JsonProperty("userchatId")
//     private String userchatId;
//     @JsonProperty("usermsgId")
//     private String usermsgId;
//     @JsonProperty("timestamp")
//     //@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
//     private LocalDateTime timestamp;

//     public ChatUpdateDTO(){}
//     public ChatUpdateDTO(String purpose,String userchatId,String msgId,LocalDateTime timestamp){
//         this.purpose=purpose;
//         this.userchatId=userchatId;
//         this.usermsgId=msgId;
//         this.timestamp=timestamp;
//     }

//     public String getpurpose(){
//         return purpose;
//     }
//     public void setpurpose(String purpose){
//         this.purpose=purpose;
//     }
    
//     public String getuserchatid(){
//         return userchatId;
//     }
//     public void setuserchatid(String chatid){
//         this.userchatId=chatid;
//     }
    
//     public String getmsgid(){
//         return usermsgId;
//     }
//     public void setmsgid(String msgid){
//         this.usermsgId=msgid;
//     }

//     public LocalDateTime gettime(){
//         return timestamp;
//     }
//     public void settime(LocalDateTime time){
//         this.timestamp=time;
//     }
// }
package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatUpdateDTO {
    private String purpose;
    @JsonProperty("userchatId")
    private String userchatId;
    @JsonProperty("usermsgId")
    private String usermsgId;
    @JsonProperty("timestamp")
    private String timestamp; // ← changed from LocalDateTime to String

    public ChatUpdateDTO(){}
    public ChatUpdateDTO(String purpose, String userchatId, String msgId, String timestamp){
        this.purpose = purpose;
        this.userchatId = userchatId;
        this.usermsgId = msgId;
        this.timestamp = timestamp;
    }

    public String getpurpose(){ return purpose; }
    public void setpurpose(String purpose){ this.purpose = purpose; }
    
    public String getuserchatid(){ return userchatId; }
    public void setuserchatid(String chatid){ this.userchatId = chatid; }
    
    public String getmsgid(){ return usermsgId; }
    public void setmsgid(String msgid){ this.usermsgId = msgid; }

    public LocalDateTime gettime(){
        if (timestamp == null) return null;
        try {
            return ZonedDateTime.parse(timestamp).toLocalDateTime(); // "2026-04-28T09:26:07.353Z"
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(timestamp); // "2026-04-28T09:26:07"
            } catch (Exception e2) {
                try {
                    return LocalDateTime.parse(timestamp,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); // "2026-04-28 09:24:06"
                } catch (Exception e3) {
                    System.out.println("timestamp parse failed: " + timestamp);
                    return null;
                }
            }
        }
    }
    public void settime(String time){ this.timestamp = time; }
}