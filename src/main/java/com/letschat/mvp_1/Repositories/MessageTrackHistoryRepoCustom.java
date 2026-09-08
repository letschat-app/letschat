package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime; 
import java.util.List; 
import reactor.core.publisher.Mono; 
public interface MessageTrackHistoryRepoCustom {
     Mono<Void> batchInsert( String msgid, String senderid, List<String> receiverIds, List<String> statuses, List<LocalDateTime> deliveredTimes ); 
}
