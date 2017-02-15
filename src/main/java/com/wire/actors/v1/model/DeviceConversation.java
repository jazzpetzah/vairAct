package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DeviceConversation {
    private String id;
    private List<ConversationMessageInfo> messagesInfo;

    public DeviceConversation() {
    }

    @JsonProperty(required = true)
    public String getId() {
        return id;
    }

    @JsonProperty(required = true)
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public List<ConversationMessageInfo> getMessagesInfo() {
        return messagesInfo;
    }

    @JsonProperty
    public void setMessagesInfo(List<ConversationMessageInfo> messagesInfo) {
        this.messagesInfo = messagesInfo;
    }
}
