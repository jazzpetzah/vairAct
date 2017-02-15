package com.wire.actors.v1.service.sync_engine_bridge;

import com.wire.actors.v1.model.ConversationMessageInfo;
import com.wire.actors.v1.model.LoginCredentials;

import java.util.List;

public interface IDevice extends IRemoteEntity {
    boolean hasLoggedInUser();

    void logInWithCredentials(LoginCredentials credentials);

    void sendMessage(String convId, String message) throws Exception;

    void sendImage(String convId, String path) throws Exception;

    void sendPing(String convId) throws Exception;

    void typing(String convId) throws Exception;

    void clearConversation(String convId) throws Exception;

    void muteConversation(String convId) throws Exception;

    void unmuteConversation(String convId) throws Exception;

    void archiveConversation(String convId) throws Exception;

    void unarchiveConversation(String convId) throws Exception;

    // TODO: void sendAsset(String convId, byte[] data, String mime, String filename) throws Exception;

    void sendFile(String convId, String path, String mime) throws Exception;

    void deleteMessage(String convId, String messageId) throws Exception;

    void deleteMessageEveryWhere(String convId, String messageId) throws Exception;

    void updateMessage(String messageId, String newMessage) throws Exception;

    List<ConversationMessageInfo> getConversationMessagesInfo(String convId) throws Exception;

    void reactMessage(String convId, String messageId, String reactionType) throws Exception;

    void shareLocation(String convId, float lon, float lat, String address, int zoom) throws Exception;

    void setLabel(String label) throws Exception;

    void setEphemeralMode(String convId, long expirationMilliseconds) throws Exception;

    void markEphemeralRead(String convId, String messageId) throws Exception;

    String getId() throws Exception;

    String getFingerprint() throws Exception;

    void destroy();

    void setAssetToV3() throws Exception;

    void setAssetToV2() throws Exception;

    void sendGiphy(String convId, String searchQuery) throws Exception;

    void cancelConnection(String userId) throws Exception;

    String getUniqueUserName() throws Exception;

    void updateUniqueUserName(String uniqueUserName) throws Exception;
}
