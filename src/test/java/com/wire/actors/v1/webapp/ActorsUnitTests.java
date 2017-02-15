package com.wire.actors.v1.webapp;

import com.wire.actors.v1.model.DeviceInfo;
import com.wire.actors.v1.model.LoginCredentials;
import com.wire.actors.v1.service.sync_engine_bridge.SEBridgeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;

public class ActorsUnitTests {
    // FIXME: update these credentials to avoid reaching devices count limit on the backend
    private static final String email = "nick+nqa2@wire.com";
    private static final String password = "12345678";

    private DevicesResource devicesResource;
    private DeviceInfo dut;

    private DeviceInfo createDut() {
        final DeviceInfo info;
        try {
            info = (DeviceInfo) this.devicesResource.create(new DeviceInfo()).getEntity();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThat(info.getUuid(), not(isEmptyString()));
        assertThat(info.getName(), not(isEmptyString()));
        assertThat(info.getLabel(), isEmptyOrNullString());
        assertThat(info.getMsTTL(), equalTo(SEBridgeService.TTL_DEFAULT));
        try {
            this.devicesResource.login(info.getUuid(), new LoginCredentials(email, password));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return info;
    }

    @Before
    public void setup() {
        this.devicesResource = new DevicesResource();
        this.dut = createDut();
    }

    @Test
    public void testGetFingerprint() throws Exception {
//        final DeviceInfo info = (DeviceInfo) this.devicesResource
//                .getFingerprint(this.dut.getUuid())
//                .getEntity();
//        assertThat(info.getUuid(), not(isEmptyString()));
//        assertThat(info.getFingerprint(), not(isEmptyString()));
    }

    @After
    public void tearDown() {
        this.devicesResource.remove(this.dut.getUuid());
    }

}
