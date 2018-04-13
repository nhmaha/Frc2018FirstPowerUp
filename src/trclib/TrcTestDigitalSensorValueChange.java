/*
 * Copyright (c) 2018 Titan Robotics Club (http://www.titanrobotics.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package trclib;

import java.util.function.Supplier;

/**
 * This class implements a diagnostics test to monitor if a digital sensor value has ever changed. If a sensor
 * value doesn't change, it could mean the sensor is malfunctioning or disconnected.
 * This class extends the TrcDiagnostics.Test class. It provides the runTest method that reads the sensor value
 * and compares to the initial sensor value to make sure the value has changed.
 *
 * @param <T> specifies the group enum type.
 */
public class TrcTestDigitalSensorValueChange<T> extends TrcDiagnostics.Test<T>
{
    private final Supplier<Boolean> sensor;
    private final String errorMsg;
    private boolean firstReading;

    /**
     * Constructor: Creates an instance of the object.
     *
     * @param name specifies the test name.
     * @param group specifies the test group.
     * @param sensor specifies the analog sensor object.
     * @param errorMsg specifies the error message to return if sensor value didn't change the specified amount.
     */
    public TrcTestDigitalSensorValueChange(String name, T group, Supplier<Boolean> sensor, String errorMsg)
    {
        super(name, group);
        this.sensor = sensor;
        this.errorMsg = errorMsg;
        this.firstReading = sensor.get();
    }   //TrcTestDigitalSensorValueChange

    /**
     * This method is called periodically to check the sensor value. It compares to the initial value for a change.
     *
     * @return error message if the sensor state has changed, null otherwise.
     */
    @Override
    public String runTest()
    {
        String msg = null;

        if (sensor.get() == firstReading)
        {
            msg = errorMsg;
        }

        return msg;
    }   //runTest

}   //class TrcTestDigitalSensorValueChange