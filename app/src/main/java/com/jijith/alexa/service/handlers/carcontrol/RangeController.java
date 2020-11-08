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

public class RangeController {
    private double min;
    private double max;
    private double value;

    public RangeController(double min, double max) {
        this.min = min;
        this.max = max;
        this.value = min;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void adjustValue(double delta) {
        value += delta;
        if (value < min) {
            value = min;
        } else if (value > max) {
            value = max;
        }
    }
}