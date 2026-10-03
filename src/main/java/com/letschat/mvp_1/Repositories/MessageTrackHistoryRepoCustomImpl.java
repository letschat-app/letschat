package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

@Repository
public class MessageTrackHistoryRepoCustomImpl
        implements MessageTrackHistoryRepoCustom {

    private final DatabaseClient databaseClient;

    public MessageTrackHistoryRepoCustomImpl(
            DatabaseClient databaseClient) {

        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Void> batchInsert(
            String msgid,
            String senderid,
            List<String> receiverIds,
            List<String> statuses,
            List<LocalDateTime> deliveredTimes) {

        if (receiverIds.isEmpty()) {
            return Mono.empty();
        }

        StringBuilder sql = new StringBuilder("""
            INSERT INTO "MessageTrackHistory"
            ("MessageId","SenderId","RecieverId","Status","DeliverTime","ReadTime")
            VALUES
            """);

        for (int i = 0; i < receiverIds.size(); i++) {

            if (i > 0) {
                sql.append(", ");
            }

            sql.append(
                "(:msgid").append(i)
                .append(", :senderid").append(i)
                .append(", :receiverid").append(i)
                .append(", :status").append(i)
                .append(", :delivertime").append(i)
                .append(", :readtime").append(i)
                .append(")");
        }

        DatabaseClient.GenericExecuteSpec query =
            databaseClient.sql(sql.toString());

        for (int i = 0; i < receiverIds.size(); i++) {

            query = query
                .bind("msgid" + i, msgid)
                .bind("senderid" + i, senderid)
                .bind("receiverid" + i, receiverIds.get(i))
                .bind("status" + i, statuses.get(i));

            LocalDateTime deliveredTime = deliveredTimes.get(i);
            String st = statuses.get(i);

            if (deliveredTime != null) {
                query = query.bind("delivertime" + i, deliveredTime);
            } else {
                query = query.bindNull("delivertime" + i, LocalDateTime.class);
            }

            if ("read".equals(st)) {
                query = query.bind("readtime" + i, deliveredTime != null ? deliveredTime : LocalDateTime.now());
            } else {
                query = query.bindNull("readtime" + i, LocalDateTime.class);
            }
        }

        return query
            .fetch()
            .rowsUpdated()
            .then();
    }
}

