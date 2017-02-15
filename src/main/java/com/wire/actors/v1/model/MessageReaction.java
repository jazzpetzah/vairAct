package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageReaction {
    private String reaction;

    public MessageReaction() {
    }

    @JsonProperty(required = true)
    public String getReaction() {
        return reaction;
    }

    @JsonProperty(required = true)
    public void setReaction(String reaction) {
        this.reaction = reaction;
    }
}
