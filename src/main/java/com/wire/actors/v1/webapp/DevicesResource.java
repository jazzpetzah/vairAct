package com.wire.actors.v1.webapp;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wire.actors.v1.common.Utils;
import com.wire.actors.v1.model.*;
import com.wire.actors.v1.service.sync_engine_bridge.CachedDevice;
import com.wire.actors.v1.service.sync_engine_bridge.IDevice;
import com.wire.actors.v1.service.sync_engine_bridge.SEBridgeService;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.internal.util.ExceptionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Path("/devices")
public class DevicesResource {
    private static final SEBridgeService seBridgeService = SEBridgeService.getInstance();
    private static final Response DEVICE_NOT_FOUND_ERROR = Response.status(Response.Status.NOT_FOUND)
            .entity(new ErrorInfo("The device has not been created yet or has been already expired"))
            .build();
    private static final Function<String, Response> EXPECTATION_ERROR = msg ->
            Response.status(Response.Status.EXPECTATION_FAILED)
                    .entity(new ErrorInfo(msg))
                    .build();
    private static final Response LOGGED_IN_USER_EXPECTED_ERROR =
            EXPECTATION_ERROR.apply("The device is expected to have logged in user");
    private static final Function<Exception, Response> UNKNOWN_ERROR = e ->
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorInfo(ExceptionUtils.exceptionStackTraceAsString(e)))
                    .build();

    private Optional<CachedDevice> locateDevice(String uuid) throws Exception {
        return seBridgeService.getRegisteredDevices(uuid).stream().findFirst();
    }

    private DeviceInfo readDeviceInfo(String uuid, IDevice device) {
        final DeviceInfo result = new DeviceInfo();
        result.setUuid(uuid);
        result.setName(device.name());
        return result;
    }

    @FunctionalInterface
    public interface FunctionWithException<A, B> {
        B apply(A a) throws Exception;
    }

    private Response wrapLoggedDevice(String uuid, FunctionWithException<IDevice, Response> f) {
        try {
            final Optional<CachedDevice> dstCachedDevice = locateDevice(uuid);
            if (dstCachedDevice.isPresent()) {
                final IDevice dstDevice = dstCachedDevice.get().getDevice();
                if (!dstDevice.hasLoggedInUser()) {
                    return LOGGED_IN_USER_EXPECTED_ERROR;
                }
                return f.apply(dstDevice);
            }
            return DEVICE_NOT_FOUND_ERROR;
        } catch (Exception e) {
            return UNKNOWN_ERROR.apply(e);
        }
    }

    //region Device Control

    @POST
    @Path("/create")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response create(DeviceInfo props) {
        try {
            final CachedDevice cachedDevice;
            if (props.getName() == null) {
                cachedDevice = seBridgeService.addDevice(props.getMsTTL());
            } else {
                cachedDevice = seBridgeService.addDevice(props.getName(), props.getMsTTL());
            }
            final IDevice resultDevice = cachedDevice.getDevice();
            return Response.ok(readDeviceInfo(cachedDevice.getUUID(), resultDevice)).build();
        } catch (Exception e) {
            return UNKNOWN_ERROR.apply(e);
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDevices() {
        try {
            final List<CachedDevice> cachedDevices = seBridgeService.getRegisteredDevices();
            final DevicesInfo result = new DevicesInfo();
            final List<DeviceInfo> devicesInfo = new ArrayList<>();
            for (CachedDevice device : cachedDevices) {
                final DeviceInfo deviceInfo = new DeviceInfo();
                deviceInfo.setUuid(device.getUUID());
                deviceInfo.setName(device.getName());
                deviceInfo.setMsTTL(null);
                final ProcessInfo processInfo = new ProcessInfo();
                processInfo.setName(device.getHostProcess().getName());
                processInfo.setLogPath(device.getHostProcess().getLogPath());
                deviceInfo.setHostProcess(processInfo);
                devicesInfo.add(deviceInfo);
            }
            result.setDevices(devicesInfo);
            return Response.ok(result).build();
        } catch (Exception e) {
            return UNKNOWN_ERROR.apply(e);
        }
    }

    @DELETE
    @Path("/{uuid}")
    public Response remove(@PathParam("uuid") String uuid) {
        try {
            final Optional<CachedDevice> dstDevice = locateDevice(uuid);
            if (dstDevice.isPresent()) {
                seBridgeService.removeDevices(dstDevice.get().getUUID());
                return Response.ok().build();
            }
            return DEVICE_NOT_FOUND_ERROR;
        } catch (Exception e) {
            return UNKNOWN_ERROR.apply(e);
        }
    }

    @POST
    @Path("/{uuid}/login")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response login(@PathParam("uuid") String uuid, LoginCredentials credentials) {
        try {
            final Optional<CachedDevice> dstCachedDevice = locateDevice(uuid);
            if (dstCachedDevice.isPresent()) {
                final IDevice dstDevice = dstCachedDevice.get().getDevice();
                dstDevice.logInWithCredentials(credentials);
                return Response.ok(readDeviceInfo(dstCachedDevice.get().getUUID(), dstDevice)).build();
            }
            return DEVICE_NOT_FOUND_ERROR;
        } catch (Exception e) {
            return UNKNOWN_ERROR.apply(e);
        }
    }

    @GET
    @Path("/{uuid}/fingerprint")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFingerprint(@PathParam("uuid") String uuid) {
        return wrapLoggedDevice(uuid, x -> {
            final String fingerprint = x.getFingerprint();
            final DeviceInfo result = readDeviceInfo(uuid, x);
            result.setFingerprint(fingerprint);
            return Response.ok(result).build();
        });
    }

    @GET
    @Path("/{uuid}/user/uniqueName")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUsername(@PathParam("uuid") String uuid) {
        return wrapLoggedDevice(uuid, x -> {
            final String uniqueName = x.getUniqueUserName();
            final DeviceInfo result = readDeviceInfo(uuid, x);
            final UserInfo userInfo = new UserInfo();
            userInfo.setUniqueName(uniqueName);
            result.setUser(userInfo);
            return Response.ok(result).build();
        });
    }

    @PUT
    @Path("/{uuid}/user/uniqueName")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUsername(@PathParam("uuid") String uuid, UserInfo input) {
        return wrapLoggedDevice(uuid, x -> {
            x.updateUniqueUserName(input.getUniqueName());
            final DeviceInfo result = readDeviceInfo(uuid, x);
            final UserInfo userInfo = new UserInfo();
            userInfo.setUniqueName(input.getUniqueName());
            result.setUser(userInfo);
            return Response.ok(result).build();
        });
    }

    @GET
    @Path("/{uuid}/id")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getId(@PathParam("uuid") String uuid) {
        return wrapLoggedDevice(uuid, x -> {
            final String id = x.getId();
            final DeviceInfo result = readDeviceInfo(uuid, x);
            result.setDeviceId(id);
            return Response.ok(result).build();
        });
    }

    @PUT
    @Path("/{uuid}/label")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response setLabel(@PathParam("uuid") String uuid, DeviceInfo input) {
        return wrapLoggedDevice(uuid, x -> {
            x.setLabel(input.getLabel());
            final DeviceInfo info = readDeviceInfo(uuid, x);
            info.setLabel(input.getLabel());
            return Response.ok(info).build();
        });
    }

    @PUT
    @Path("/{uuid}/assets/version")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response setAssetsVersion(@PathParam("uuid") String uuid, AssetsVersionInfo input) {
        return wrapLoggedDevice(uuid, x -> {
            switch (input.getVersion().toLowerCase()) {
                case "2":
                    x.setAssetToV2();
                    break;
                case "3":
                    x.setAssetToV3();
                    break;
                default:
                    return EXPECTATION_ERROR.apply("Supported assets versions are '2' and '3'");
            }
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    //endregion


    //region Conversations Control


    //region Sending stuff

    @POST
    @Path("/{uuid}/conversations/{convoId}/send/message")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response sendMessage(@PathParam("uuid") String uuid,
                                @PathParam("convoId") String convoId,
                                ConversationMessage input) {
        return wrapLoggedDevice(uuid, x -> {
            x.sendMessage(convoId, input.getPayload());
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/send/giphy")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response sendGiphy(@PathParam("uuid") String uuid,
                              @PathParam("convoId") String convoId,
                              ConversationMessage input) {
        return wrapLoggedDevice(uuid, x -> {
            x.sendGiphy(convoId, input.getPayload());
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/send/location")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response sendLocation(@PathParam("uuid") String uuid,
                                 @PathParam("convoId") String convoId,
                                 LocationInfo input) {
        return wrapLoggedDevice(uuid, x -> {
            x.shareLocation(convoId, input.getLon(), input.getLat(), input.getAddress(), input.getZoom());
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/send/image")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response sendImage(@PathParam("uuid") String uuid,
                              @PathParam("convoId") String convoId,
                              ConversationMessage input) {
        return wrapLoggedDevice(uuid, x -> {
            final Optional<String> fileName = Optional.ofNullable(input.getFileName());
            final Optional<File> tmpImageFile = Utils.base64StringToFile(input.getPayload(), input.getMimeType(),
                    fileName);
            if (!tmpImageFile.isPresent()) {
                return EXPECTATION_ERROR.apply("Payload value is expected to be Base64-encoded file content");
            }
            try {
                x.sendImage(convoId, tmpImageFile.get().getAbsolutePath());
            } finally {
                if (fileName.isPresent()) {
                    if (tmpImageFile.get().getParentFile().isDirectory()) {
                        FileUtils.deleteDirectory(tmpImageFile.get().getParentFile());
                    }
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    tmpImageFile.get().delete();
                }
            }
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/send/file")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response sendFile(@PathParam("uuid") String uuid,
                             @PathParam("convoId") String convoId,
                             ConversationMessage input) {
        return wrapLoggedDevice(uuid, x -> {
            final Optional<String> fileName = Optional.ofNullable(input.getFileName());
            final Optional<File> tmpFile = Utils.base64StringToFile(input.getPayload(), input.getMimeType(), fileName);
            if (!tmpFile.isPresent()) {
                return EXPECTATION_ERROR.apply("Payload value is expected to be Base64-encoded file content");
            }
            try {
                x.sendFile(convoId, tmpFile.get().getAbsolutePath(), input.getMimeType());
            } finally {
                if (fileName.isPresent()) {
                    if (tmpFile.get().getParentFile().isDirectory()) {
                        FileUtils.deleteDirectory(tmpFile.get().getParentFile());
                    }
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    tmpFile.get().delete();
                }
            }
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/send/ping")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response sendImage(@PathParam("uuid") String uuid, @PathParam("convoId") String convoId) {
        return wrapLoggedDevice(uuid, x -> {
            x.sendPing(convoId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/send/typing")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response sendTyping(@PathParam("uuid") String uuid, @PathParam("convoId") String convoId) {
        return wrapLoggedDevice(uuid, x -> {
            x.typing(convoId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    //endregion


    @POST
    @Path("/{uuid}/conversations/{convoId}/clear")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response clearConversation(@PathParam("uuid") String uuid, @PathParam("convoId") String convoId) {
        return wrapLoggedDevice(uuid, x -> {
            x.clearConversation(convoId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/mute")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response muteConversation(@PathParam("uuid") String uuid, @PathParam("convoId") String convoId) {
        return wrapLoggedDevice(uuid, x -> {
            x.muteConversation(convoId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/unmute")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response unmuteConversation(@PathParam("uuid") String uuid, @PathParam("convoId") String convoId) {
        return wrapLoggedDevice(uuid, x -> {
            x.unmuteConversation(convoId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/archive")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response archiveConversation(@PathParam("uuid") String uuid, @PathParam("convoId") String convoId) {
        return wrapLoggedDevice(uuid, x -> {
            x.archiveConversation(convoId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/unarchive")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response unarchiveConversation(@PathParam("uuid") String uuid, @PathParam("convoId") String convoId) {
        return wrapLoggedDevice(uuid, x -> {
            x.unarchiveConversation(convoId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/ephemeral")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response unarchiveConversation(@PathParam("uuid") String uuid,
                                          @PathParam("convoId") String convoId,
                                          EphemeralProperties props) {
        return wrapLoggedDevice(uuid, x -> {
            x.setEphemeralMode(convoId, props.getMsTimeout());
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/connections/{connId}/cancel")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response cnacelConnection(@PathParam("uuid") String uuid, @PathParam("connId") String connId) {
        return wrapLoggedDevice(uuid, x -> {
            x.cancelConnection(connId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    //region Conversation Messages

    @POST
    @Path("/{uuid}/conversations/{convoId}/messages/{msgId}/delete")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteMessage(@PathParam("uuid") String uuid,
                                  @PathParam("convoId") String convoId,
                                  @PathParam("msgId") String msgId) {
        return wrapLoggedDevice(uuid, x -> {
            x.deleteMessage(convoId, msgId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/messages/{msgId}/deleteEverywhere")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteMessageEverywhere(@PathParam("uuid") String uuid,
                                            @PathParam("convoId") String convoId,
                                            @PathParam("msgId") String msgId) {
        return wrapLoggedDevice(uuid, x -> {
            x.deleteMessageEveryWhere(convoId, msgId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/messages/{msgId}/update")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateMessage(@PathParam("uuid") String uuid,
                                  @PathParam("convoId") String convoId,
                                  @PathParam("msgId") String msgId,
                                  ConversationMessage input) {
        return wrapLoggedDevice(uuid, x -> {
            x.updateMessage(msgId, input.getPayload());
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/messages/{msgId}/readEphemeral")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateMessage(@PathParam("uuid") String uuid,
                                  @PathParam("convoId") String convoId,
                                  @PathParam("msgId") String msgId) {
        return wrapLoggedDevice(uuid, x -> {
            x.markEphemeralRead(convoId, msgId);
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @POST
    @Path("/{uuid}/conversations/{convoId}/messages/{msgId}/react")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateMessage(@PathParam("uuid") String uuid,
                                  @PathParam("convoId") String convoId,
                                  @PathParam("msgId") String msgId,
                                  MessageReaction input) {
        return wrapLoggedDevice(uuid, x -> {
            x.reactMessage(convoId, msgId, input.getReaction());
            return Response.ok(readDeviceInfo(uuid, x)).build();
        });
    }

    @GET
    @Path("/{uuid}/conversations/{convoId}/messages/info")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getMessagesInfo(@PathParam("uuid") String uuid,
                                    @PathParam("convoId") String convoId) {
        return wrapLoggedDevice(uuid, x -> {
            final DeviceConversation dstConversation = new DeviceConversation();
            dstConversation.setId(convoId);
            dstConversation.setMessagesInfo(x.getConversationMessagesInfo(convoId));
            final DeviceInfo result = readDeviceInfo(uuid, x);
            result.setConversations(Collections.singletonList(dstConversation));
            return Response.ok(result).build();
        });
    }

    //endregion


    //endregion
}
