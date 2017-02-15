package com.wire.actors.v1.service.sync_engine_bridge;

import java.util.*;

import akka.actor.ActorRef;

import akka.actor.PoisonPill;
import com.waz.api.EphemeralExpiration;
import com.waz.model.Liking;
import com.waz.model.MessageId;
import com.waz.model.RConvId;
import com.waz.model.UserId;
import com.waz.provision.ActorMessage;

import com.wire.actors.v1.model.ConversationMessageInfo;
import com.wire.actors.v1.model.LoginCredentials;
import org.apache.commons.lang3.StringUtils;
import scala.concurrent.duration.FiniteDuration;

class Device extends RemoteEntity implements IDevice {
    private Optional<LoginCredentials> loginCredentials = Optional.empty();
    private Optional<String> id = Optional.empty();
    private Optional<String> fingerprint = Optional.empty();
    private IRemoteProcess hostProcess = null;
    private final ActorRef coordinatorActorRef;

    Device(IRemoteProcess hostProcess, String deviceName, ActorRef coordinatorActorRef, FiniteDuration actorTimeout) {
        super(deviceName, actorTimeout);
        this.hostProcess = hostProcess;
        this.coordinatorActorRef = coordinatorActorRef;
        if (!spawnOnHostProcess()) {
            throw new IllegalStateException(String.format(
                    "There was an error establishing a connection with a new device: "
                            + "%s on process: %s. Please check the log file %s for more details.",
                    this.name(), this.hostProcess.name(), this.hostProcess.getLog().getAbsolutePath()));
        }
    }

    private boolean spawnOnHostProcess() {
        try {
            final Object resp = askActor(this.hostProcess.ref(),
                    new ActorMessage.SpawnRemoteDevice(null, this.name()));
            if (resp instanceof ActorRef) {
                ActorRef deviceRef = (ActorRef) resp;
                this.setRef(deviceRef);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean hasLoggedInUser() {
        return this.loginCredentials.isPresent();
    }

    @Override
    public void logInWithCredentials(LoginCredentials credentials) {
        if (this.loginCredentials.isPresent()) {
            return;
        }
        final Object resp;
        try {
            resp = askActor(this.ref(),
                    new ActorMessage.Login(credentials.getEmail(), credentials.getPassword()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (resp instanceof ActorMessage.Successful$) {
            this.loginCredentials = Optional.of(credentials);
            // Wait until prekeys are generated asynchronously for this client
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalStateException(String.format(
                    "User '%s' has failed to log in into device '%s'. Please check the log file %s for more details.",
                    credentials.getEmail(), this.name(), this.hostProcess.getLog().getAbsolutePath()));
        }
    }

    @Override
    public void setLabel(String label) throws Exception {
        askActor(this.ref(), new ActorMessage.SetDeviceLabel(label));
    }

    @Override
    public void sendMessage(String convId, String message) throws Exception {
        askActor(this.ref(), new ActorMessage.SendText(new RConvId(convId), message));
    }

    @Override
    public void sendImage(String convId, String path) throws Exception {
        askActor(this.ref(), new ActorMessage.SendImage(new RConvId(convId), path));
    }

    @Override
    public void sendGiphy(String convId, String searchQuery) throws Exception {
        askActor(this.ref(), new ActorMessage.SendGiphy(new RConvId(convId), searchQuery));
    }

    @Override
    public void sendPing(String convId) throws Exception {
        askActor(this.ref(), new ActorMessage.Knock(new RConvId(convId)));
    }

    @Override
    public void typing(String convId) throws Exception {
        askActor(this.ref(), new ActorMessage.Typing(new RConvId(convId)));
    }

    @Override
    public void clearConversation(String convId) throws Exception {
        askActor(this.ref(), new ActorMessage.ClearConversation(new RConvId(convId)));
    }

    @Override
    public void muteConversation(String convId) throws Exception {
        askActor(this.ref(), new ActorMessage.MuteConv(new RConvId(convId)));
    }

    @Override
    public void unmuteConversation(String convId) throws Exception {
        askActor(this.ref(), new ActorMessage.UnmuteConv(new RConvId(convId)));
    }

    @Override
    public void archiveConversation(String convId) throws Exception {
        askActor(this.ref(), new ActorMessage.ArchiveConv(new RConvId(convId)));
    }

    @Override
    public void unarchiveConversation(String convId) throws Exception {
        askActor(this.ref(), new ActorMessage.UnarchiveConv(new RConvId(convId)));
    }

    @Override
    public void sendFile(String convId, String path, String mime) throws Exception {
        askActor(this.ref(), new ActorMessage.SendFile(new RConvId(convId), path, mime));
    }

    @Override
    public void deleteMessage(String convId, String messageId) throws Exception {
        askActor(this.ref(), new ActorMessage.DeleteMessage(new RConvId(convId), new MessageId(messageId)));
    }

    @Override
    public void deleteMessageEveryWhere(String convId, String messageId) throws Exception {
        askActor(this.ref(), new ActorMessage.RecallMessage(new RConvId(convId), new MessageId(messageId)));
    }

    @Override
    public void updateMessage(String messageId, String newMessage) throws Exception {
        askActor(this.ref(), new ActorMessage.UpdateText(new MessageId(messageId), newMessage));
    }

    @Override
    public List<ConversationMessageInfo> getConversationMessagesInfo(String convId) throws Exception {
        final List<ConversationMessageInfo> result = new ArrayList<>();
        final Object convMessages = askActor(this.ref(), new ActorMessage.GetMessages(new RConvId(convId)));
        if (!(convMessages instanceof ActorMessage.ConvMessages)) {
            return result;
        }
        for (ActorMessage.MessageInfo msgInfo : ((ActorMessage.ConvMessages) convMessages).msgs()) {
            final ConversationMessageInfo item = new ConversationMessageInfo();
            item.setMessageId(msgInfo.id().toString());
            item.setTime(msgInfo.time().toEpochMilli());
            item.setType(msgInfo.tpe().name());
            result.add(item);
        }
        return result;
    }

    @Override
    public void reactMessage(String convId, String messageId, String reactionType) throws Exception {
        final Liking.Action action;
        switch (reactionType.toUpperCase()) {
            case "LIKE":
                action = Liking.like();
                break;
            case "UNLIKE":
                action = Liking.unlike();
                break;
            default:
                throw new IllegalArgumentException("Only two reaction types are supported: 'LIKE' and 'UNLIKE'");
        }
        askActor(this.ref(),
                new ActorMessage.SetMessageReaction(new RConvId(convId), new MessageId(messageId), action));
    }

    @Override
    public void shareLocation(String convId, float lon, float lat, String address, int zoom) throws Exception {
        askActor(this.ref(), new ActorMessage.SendLocation(new RConvId(convId), lon, lat, address, zoom));
    }

    @Override
    public void setEphemeralMode(String convId, long expirationMilliseconds) throws Exception {
        askActor(this.ref(), new ActorMessage.SetEphemeral(new RConvId(convId),
                EphemeralExpiration.getForMillis(expirationMilliseconds)));
    }

    @Override
    public void markEphemeralRead(String convId, String messageId) throws Exception {
        askActor(this.ref(), new ActorMessage.MarkEphemeralRead(new RConvId(convId), new MessageId(messageId)));
    }

    @Override
    public String getId() throws Exception {
        if (!this.id.isPresent()) {
            final Object resp = askActor(this.ref(), new ActorMessage.GetDeviceId());
            if (resp instanceof ActorMessage.Successful) {
                // FIXME: This padding should happen on SE side. Please remove this workaround when it's fixed
                id = Optional.of(
                        StringUtils.leftPad(((ActorMessage.Successful) resp).response(), 16, "0")
                );
                //noinspection OptionalGetWithoutIsPresent
                return id.get();
            }
            throw new IllegalStateException(String.format(
                    "Could not get ID of '%s' device. Please check the log file %s for more details.",
                    this.name(), this.hostProcess.getLog().getAbsolutePath()));
        }
        return this.id.get();
    }

    @Override
    public String getFingerprint() throws Exception {
        if (!this.fingerprint.isPresent()) {
            final Object resp = askActor(this.ref(), new ActorMessage.GetDeviceFingerPrint());
            if (resp instanceof ActorMessage.Successful) {
                fingerprint = Optional.of(((ActorMessage.Successful) resp).response());
                //noinspection OptionalGetWithoutIsPresent
                return fingerprint.get();
            }
            throw new IllegalStateException(String.format(
                    "Could not get getFingerprint of '%s' device. Please check the log file %s for more details.",
                    this.name(), this.hostProcess.getLog().getAbsolutePath()));
        }
        return this.fingerprint.get();
    }

    @Override
    public synchronized void destroy() {
        if (this.coordinatorActorRef != null && this.hostProcess != null) {
            this.ref().tell(PoisonPill.getInstance(), null);
            this.hostProcess = null;
        }
    }

    @Override
    public void setAssetToV3() throws Exception {
        askActor(this.ref(), ActorMessage.SetAssetToV3$.MODULE$);
    }

    @Override
    public void setAssetToV2() throws Exception {
        askActor(this.ref(), ActorMessage.SetAssetToV2$.MODULE$);
    }

    @Override
    public void cancelConnection(String userId) throws Exception {
        askActor(this.ref(), new ActorMessage.CancelConnection(new UserId(userId)));
    }

    @Override
    public String getUniqueUserName() throws Exception {
        return (String) askActor(this.ref(), ActorMessage.GetUserName$.MODULE$);
    }

    @Override
    public void updateUniqueUserName(String uniqueUserName) throws Exception {
        askActor(this.ref(), new ActorMessage.UpdateProfileUserName(uniqueUserName));
    }
}
