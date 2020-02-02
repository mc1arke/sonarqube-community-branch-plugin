package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;

import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.AlmSettingsWsAction;

public class AlmSettingsWsTest {

    @Test
    public void testDefine() {
        List<AlmSettingsWsAction> actions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            actions.add(mock(AlmSettingsWsAction.class));
        }
        AlmSettingsWs testCase = new AlmSettingsWs(actions);

        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.Context context = mock(WebService.Context.class);
        when(context.createController(eq("api/alm_settings"))).thenReturn(newController);
        testCase.define(context);

        actions.forEach(a -> verify(a).define(eq(newController)));

        verify(newController).done();

    }
}