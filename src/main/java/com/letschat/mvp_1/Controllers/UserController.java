package com.letschat.mvp_1.Controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.DTOs.UserLoginDTO;
import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.DTOs.UserSignUpDTO;
import com.letschat.mvp_1.Models.EventInfo;
import com.letschat.mvp_1.Service.ChatBoxService;
import com.letschat.mvp_1.Service.EventService;
import com.letschat.mvp_1.Service.GroupCreateService;
import com.letschat.mvp_1.Service.UserChatService;
import com.letschat.mvp_1.Service.UserLoginService;
import com.letschat.mvp_1.Service.UserProfileService;
import com.letschat.mvp_1.Service.UserSearchService;
import com.letschat.mvp_1.Service.UserSignUpService;

import reactor.core.publisher.Mono;

//@CrossOrigin(origins="*")
@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    private final UserSignUpService userSignUpService;
    private final UserLoginService userLoginService;
    private final UserSearchService userSearchService;
    private final UserChatService userChatService;
    private final ChatBoxService chatBoxService;
    private final UserProfileService userProfileService;
    private final EventService eventService;
    public UserController(UserSignUpService userSignUpService,UserLoginService userLoginService,UserSearchService userSearchService,UserChatService userChatService,GroupCreateService groupCreateService,ChatBoxService chatBoxService,UserProfileService userProfileService,EventService eventService){
        this.userSignUpService=userSignUpService;
        this.userLoginService=userLoginService;
        this.userSearchService=userSearchService;
        this.userChatService=userChatService;
        this.chatBoxService=chatBoxService;
        this.userProfileService = userProfileService;
        this.eventService=eventService;
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<String>> signup(@RequestBody UserSignUpDTO request) {
        System.out.println("creating");
        return userSignUpService.createUser(request)
        .map(message->ResponseEntity.status(201).body(message));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<String>> login(@RequestBody UserLoginDTO request){
        return userLoginService.logUser(request)
        .map(message->ResponseEntity.ok(message));
    }

    @GetMapping("/explore")
    public Mono<List<UserSearchResult>> explore(){
        System.out.println("exloring");
        return userSearchService.explore().collectList();
    }

    @GetMapping("/search/{UserId}")
    public Mono<ResponseEntity<UserSearchResult>> search(@PathVariable String UserId){
        return userSearchService.search(UserId)
        .map(user->ResponseEntity.ok(user))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/addtochat/{AddUserId}")
    public Mono<ResponseEntity<String>> addchat(@PathVariable String AddUserId,@RequestHeader("User-Id") String UserId){
        return userChatService.addchat(UserId, AddUserId)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("error")));
    }

    @GetMapping("/chatbox/{UserId}")
    public Mono<List<ChatBoxReturnDTO>> getchatbox(@PathVariable String UserId){
        return chatBoxService.getchat(UserId).collectList();
       // .doOnNext(data->System.out.println(data.getcAt()));
    }

    //@PostMapping(value="/profile/{UserId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PostMapping("/profile/{UserId}")
    public Mono<ResponseEntity<String>> setprofile(@RequestPart("file") Mono<FilePart> fileMono,@PathVariable String UserId){
        System.out.println("profile"+UserId);
        return fileMono
        .flatMap(file->userProfileService.upload(file, UserId))
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
    }

    @PostMapping("/setevent/{UserId}")
    public Mono<ResponseEntity<Long>> setevent(@RequestBody EventInfo event,@PathVariable String UserId) {      
        return eventService.addevent(event, UserId)
        .map(id->ResponseEntity.ok(id));
    }

    @GetMapping("getevents/{UserId}")
    public Mono<List<EventInfo>> getevent(@PathVariable String UserId){
        return eventService.getevents(UserId).collectList();
    }

    @PostMapping("/update")
    public Mono<ResponseEntity<String>> updateaction(@RequestBody EventInfo event){
        System.out.println("updated");
        return eventService.action(event).thenReturn(ResponseEntity.ok("ok"));
    }

    @GetMapping("/getchatinfo/{Chatid}")
    public Mono<UserSearchResult> getInfo(@PathVariable String Chatid,@RequestHeader("User-Id") String Userid){
        return userChatService.getInfo(Chatid,Userid);
    }
    
    @PostMapping("/updatemyinfo/{ChatId}")
    public Mono<ResponseEntity<String>> updateInfo(@PathVariable String ChatId,@RequestBody UserSearchResult info){
        return userChatService.updateInfo(info,ChatId)
        .map(msg->ResponseEntity.ok("ok"))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
    }
    
}
