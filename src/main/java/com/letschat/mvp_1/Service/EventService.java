package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Models.EventInfo;
import com.letschat.mvp_1.Repositories.EventInfoRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class EventService {
    private final EventInfoRepo eventInfoRepo;
    public EventService(EventInfoRepo eventInfoRepo){
        this.eventInfoRepo=eventInfoRepo;
    }

    public Mono<Long> addevent(EventInfo event,String Userid){
        System.out.println("adding event");
        System.out.println(event.getEventTitle());
        event.setUserId(Userid);
        return eventInfoRepo.save(event)
        .flatMap(evnt->{
            System.out.println(evnt.getStartTime()+"and"+evnt.getEndTime());
            return Mono.just(evnt.getEventId());
        });
    }

    public Flux<EventInfo> getevents(String Userid){
        System.out.println("returning");
        return eventInfoRepo.findByUserId(Userid)
        .doOnNext(ev->System.out.println(ev.getStartTime()));
    }
}
