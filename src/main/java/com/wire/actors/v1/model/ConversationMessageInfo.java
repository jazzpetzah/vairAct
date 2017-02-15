package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConversationMessageInfo {
    private String messageId;
    // see com.waz.api.Message.Type for more details
    private String type;
    private Long time;

    public ConversationMessageInfo() {
    }

    @JsonProperty(required = true)
    public Long getTime() {
        return time;
    }

    @JsonProperty(required = true)
    public void setTime(Long time) {
        this.time = time;
    }

    @JsonProperty(required = true)
    public String getType() {
        return type;
    }

    @JsonProperty(required = true)
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty(required = true)
    public String getMessageId() {
        return messageId;
    }

    @JsonProperty(required = true)
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
