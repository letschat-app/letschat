package com.letschat.mvp_1.DTOs;

import java.util.List;

public class CreatePollResponseDTO {
    private Long poll_id;
    private List<OptionItem> options;

    public CreatePollResponseDTO() {}

    public CreatePollResponseDTO(Long poll_id, List<OptionItem> options) {
        this.poll_id = poll_id;
        this.options = options;
    }

    public Long getPoll_id() {
        return poll_id;
    }

    public void setPoll_id(Long poll_id) {
        this.poll_id = poll_id;
    }

    public List<OptionItem> getOptions() {
        return options;
    }

    public void setOptions(List<OptionItem> options) {
        this.options = options;
    }

    public static class OptionItem {
        private Long option_id;
        private String text;

        public OptionItem() {}

        public OptionItem(Long option_id, String text) {
            this.option_id = option_id;
            this.text = text;
        }

        public Long getOption_id() {
            return option_id;
        }

        public void setOption_id(Long option_id) {
            this.option_id = option_id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
