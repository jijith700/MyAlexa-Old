/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.jijith.alexa.service.handlers.carcontrol;

import android.content.Context;

import com.amazon.aace.carControl.CarControl;

import timber.log.Timber;

/**
 * This class demonstrates a how to handle Car Control platform interfaces.
 * It uses the CarControlDataProvider class to retrieve and store values.
 */
public class CarControlHandler extends CarControl {
    public CarControlHandler(Context context) {
    }

    @Override
    public void turnPowerControllerOn(String endpointId) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        CarControlDataProvider.getBoolController(endpointId).setValue(true);

        message.append("Entity   : Power Controller").append("\n")
                .append("Endpoint : ").append(endpointId).append("\n")
                .append("Action   : Turn On");

        Timber.d(message.toString());
    }

    @Override
    public void turnPowerControllerOff(String endpointId) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        CarControlDataProvider.getBoolController(endpointId).setValue(false);

        message.append("Entity   : Power Controller").append("\n")
                .append("Endpoint : ").append(endpointId).append("\n")
                .append("Action   : Turn Off");

        Timber.d(message.toString());
    }

    @Override
    public boolean isPowerControllerOn(String endpointId) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        boolean isOn = CarControlDataProvider.getBoolController(endpointId).getValue();

        message.append("Entity   : Power Controller").append("\n")
                .append("Endpoint : ").append(endpointId).append("\n")
                .append("Action   : Query State").append("\n")
                .append("State    : ").append(isOn ? "On" : "Off");

        Timber.d(message.toString());

        return isOn;
    }

    @Override
    public void turnToggleControllerOn(String endpointId, String controllerId) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        CarControlDataProvider.getBoolController(endpointId, controllerId).setValue(true);

        message.append("Entity     : Toggle Controller").append("\n")
                .append("Endpoint   : ").append(endpointId).append("\n")
                .append("Controller : ").append(controllerId).append("\n")
                .append("Action     : Turn On");

        Timber.d(message.toString());
    }

    @Override
    public void turnToggleControllerOff(String endpointId, String controllerId) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        CarControlDataProvider.getBoolController(endpointId, controllerId).setValue(false);

        message.append("Entity     : Toggle Controller").append("\n")
                .append("Endpoint   : ").append(endpointId).append("\n")
                .append("Controller : ").append(controllerId).append("\n")
                .append("Action     : Turn Off");

        Timber.d(message.toString());
    }

    @Override
    public boolean isToggleControllerOn(String endpointId, String controllerId) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        boolean isOn = CarControlDataProvider.getBoolController(endpointId, controllerId).getValue();

        message.append("Entity     : Toggle Controller").append("\n")
                .append("Endpoint   : ").append(endpointId).append("\n")
                .append("Controller : ").append(controllerId).append("\n")
                .append("Action     : Query State").append("\n")
                .append("State      : ").append(isOn ? "On" : "Off");

        Timber.d(message.toString());

        return isOn;
    }

    @Override
    public void setRangeControllerValue(String endpointId, String controllerId, double value) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        CarControlDataProvider.getRangeController(endpointId, controllerId).setValue(value);

        message.append("Entity     : Range Controller").append("\n")
                .append("Endpoint   : ").append(endpointId).append("\n")
                .append("Controller : ").append(controllerId).append("\n")
                .append("Action     : Set Value").append("\n")
                .append("Value      : ").append(value);

        Timber.d(message.toString());
    }

    @Override
    public void adjustRangeControllerValue(String endpointId, String controllerId, double delta) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        double value = CarControlDataProvider.getRangeController(endpointId, controllerId).getValue();
        CarControlDataProvider.getRangeController(endpointId, controllerId).adjustValue(delta);
        double valueNew = CarControlDataProvider.getRangeController(endpointId, controllerId).getValue();

        message.append("Entity     : Range Controller").append("\n")
                .append("Endpoint   : ").append(endpointId).append("\n")
                .append("Controller : ").append(controllerId).append("\n")
                .append("Action     : Adjust Value").append("\n")
                .append("Value      : ").append(value).append("\n")
                .append("Delta      : ").append(delta).append("\n")
                .append("New Value  : ").append(valueNew);

        Timber.d(message.toString());
    }

    @Override
    public double getRangeControllerValue(String endpointId, String controllerId) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        double value = CarControlDataProvider.getRangeController(endpointId, controllerId).getValue();

        message.append("Entity     : Range Controller").append("\n")
                .append("Endpoint   : ").append(endpointId).append("\n")
                .append("Controller : ").append(controllerId).append("\n")
                .append("Action     : Get Value").append("\n")
                .append("Value      : ").append(value);

        Timber.d(message.toString());

        return value;
    }

    @Override
    public void setModeControllerValue(String endpointId, String controllerId, String value) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        CarControlDataProvider.getModeController(endpointId, controllerId).setMode(value);

        message.append("Entity     : Mode Controller").append("\n")
                .append("Endpoint   : ").append(endpointId).append("\n")
                .append("Controller : ").append(controllerId).append("\n")
                .append("Action     : Set Mode").append("\n")
                .append("Value      : ").append(value);

        Timber.d(message.toString());
    }

    @Override
    public void adjustModeControllerValue(String endpointId, String controllerId, int delta) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        String value = CarControlDataProvider.getModeController(endpointId, controllerId).getMode();
        CarControlDataProvider.getModeController(endpointId, controllerId).adjustMode(delta);
        String valueNew = CarControlDataProvider.getModeController(endpointId, controllerId).getMode();

        message.append("Entity     : Mode Controller").append("\n")
                .append("Endpoint   : ").append(endpointId).append("\n")
                .append("Controller : ").append(controllerId).append("\n")
                .append("Action     : Adjust Value").append("\n")
                .append("Value      : ").append(value).append("\n")
                .append("Delta      : ").append(delta).append("\n")
                .append("New Value  : ").append(valueNew);

        Timber.d(message.toString());
    }

    @Override
    public String getModeControllerValue(String endpointId, String controllerId) throws Exception {
        StringBuilder message = new StringBuilder("\n");

        String value = CarControlDataProvider.getModeController(endpointId, controllerId).getMode();

        message.append("Entity     : Mode Controller").append("\n")
                .append("Endpoint   : ").append(endpointId).append("\n")
                .append("Controller : ").append(controllerId).append("\n")
                .append("Action     : Get Value").append("\n")
                .append("Value      : ").append(value);

        Timber.d(message.toString());

        return value;
    }
}
