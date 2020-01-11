/*
 * Copyright (C) 2019 Michael Clarke
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.github.mc1arke.sonarqube.plugin.scanner;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonarqube.ws.client.WsRequest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ScannerWsClientWrapperTest {

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testRuntimeExceptionPropagated() {
        ScannerWsClient scannerWsClient = mock(ScannerWsClient.class);
        doThrow(new IllegalStateException("Whoops")).when(scannerWsClient).call(any());

        expectedException.expectMessage(IsEqual.equalTo("Whoops"));
        expectedException.expect(IllegalStateException.class);

        new ScannerWsClientWrapper(scannerWsClient).call(mock(WsRequest.class));
    }

    @Test
    public void testErrorPropagated() {
        ScannerWsClient scannerWsClient = mock(ScannerWsClient.class);
        doThrow(new ClassFormatError("Whoops")).when(scannerWsClient).call(any());

        expectedException.expectMessage(IsEqual.equalTo("Whoops"));
        expectedException.expect(ClassFormatError.class);

        new ScannerWsClientWrapper(scannerWsClient).call(mock(WsRequest.class));
    }

    @Test
    public void testCheckedExceptionWrapped() {
        ScannerWsClient scannerWsClient = mock(ScannerWsClient.class);
        doAnswer(i -> {
            throw new IOException("Whoops");
        }).when(scannerWsClient).call(any());

        expectedException.expectMessage(IsEqual.equalTo("Could not execute ScannerWsClient"));
        expectedException.expect(IllegalStateException.class);
        expectedException.expectCause(new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(Object item) {
                return item instanceof InvocationTargetException &&
                       ((InvocationTargetException) item).getCause() instanceof IOException &&
                       "Whoops".equals(((InvocationTargetException) item).getCause().getMessage());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Exception checked");
            }
        });

        new ScannerWsClientWrapper(scannerWsClient).call(mock(WsRequest.class));
    }


    @Test
    public void testNonInvocationExceptionWrapped() {
        expectedException.expectMessage(IsEqual.equalTo("Could not execute ScannerWsClient"));
        expectedException.expect(IllegalStateException.class);
        expectedException.expectCause(new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(Object item) {
                return item instanceof NoSuchMethodException &&
                       "java.lang.Object.call(org.sonarqube.ws.client.WsRequest)"
                               .equals(((NoSuchMethodException) item).getMessage());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("NoSuchMethodException");
            }
        });

        Object scannerWsClient = new Object();
        new ScannerWsClientWrapper(scannerWsClient).call(mock(WsRequest.class));
    }
}
