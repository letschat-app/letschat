package com.letschat.mvp_1.DTOs;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PollOptionDTO {
    private Long option_id;
    private String option_text;
    private Integer count;
    private List<String> voters;

    public PollOptionDTO() {}

    public PollOptionDTO(Long option_id, String option_text, Integer count, List<String> voters) {
        this.option_id = option_id;
        this.option_text = option_text;
        this.count = count;
        this.voters = voters;
    }

    public Long getOption_id() {
        return option_id;
    }

    public void setOption_id(Long option_id) {
        this.option_id = option_id;
    }

    public String getOption_text() {
        return option_text;
    }

    public void setOption_text(String option_text) {
        this.option_text = option_text;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<String> getVoters() {
        return voters;
    }

    public void setVoters(List<String> voters) {
        this.voters = voters;
    }
}
