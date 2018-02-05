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
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package team492;

import trclib.TrcEvent;
import trclib.TrcPidActuator;
import trclib.TrcPidController;
import frclib.FrcCANTalon;
import frclib.FrcCANTalonLimitSwitch;

public class Elevator
{
    public TrcPidActuator elevator;
    public FrcCANTalon elevatorMotor;
    public TrcPidController elevatorPidCtrl;
    
    private double elevatorPower = 0.0;

    public Elevator()
    {
        elevatorMotor = new FrcCANTalon("elevatorMotor", RobotInfo.CANID_ELEVATOR); // the name and the device number
        elevatorMotor.ConfigFwdLimitSwitchNormallyOpen(false);
        elevatorMotor.ConfigRevLimitSwitchNormallyOpen(false);
        // elevatorMotor.setSoftLimitEnabled(true, true);
        // elevatorMotor.setSoftLowerLimit(RobotInfo.ELEVATOR_MIN_HEIGHT);
        // elevatorMotor.setSoftLowerLimit(RobotInfo.ELEVATOR_MAX_HEIGHT);
        elevatorPidCtrl = new TrcPidController(
            "elevatorPidController",
            new TrcPidController.PidCoefficients(RobotInfo.ELEVATOR_KP, RobotInfo.ELEVATOR_KI, RobotInfo.ELEVATOR_KD),
            RobotInfo.ELEVATOR_TOLERANCE, this::getPosition);
        elevator = new TrcPidActuator(
            "elevator", elevatorMotor,
            new FrcCANTalonLimitSwitch("elevatorLowerLimit", elevatorMotor, false),
            elevatorPidCtrl, RobotInfo.ELEVATOR_MIN_HEIGHT, RobotInfo.ELEVATOR_MAX_HEIGHT,
            this::getGravityCompensation);
        elevator.setPositionScale(RobotInfo.ELEVATOR_INCHES_PER_COUNT);
    }

    public void setManualOverride(boolean manualOverride)
    {
        elevator.setManualOverride(manualOverride);
    }   //setManualOverride

    public void zeroCalibrate()
    {
        elevator.zeroCalibrate(RobotInfo.ELEVATOR_CAL_POWER);
    }   //zeroCalibrate

    /**
     * 
     * @param pos (Altitude in inches)
     * @param event (TrcEvent event)
     * @param timeout 
     * Set the position for Elevator to move to in inches using PID control.
     */
    public void setPosition(double pos, TrcEvent event, double timeout)
    {
        elevator.setTarget(pos, event, timeout);
    }   //setPosition

    public void setPower(double power)
    {
        double pos = getPosition();

        if (!elevator.isManualOverride() &&
        	(power > 0.0 && pos >= RobotInfo.ELEVATOR_MAX_HEIGHT ||
            power < 0.0 && pos <= RobotInfo.ELEVATOR_MIN_HEIGHT))
        {
            power = 0.0;
        }

        elevator.setPower(power);
        elevatorPower = power;
    }   //setPower

    // get the current power the elevator actuator is running at.
    public double getPower()
    {
        return elevatorPower;
    }

    // get the current altitude of the elevator relative to encoder zero. (in inches)
    public double getPosition()
    {
        return elevator.getPosition();
    }

    public double getGravityCompensation()
    {
        return RobotInfo.ELEVATOR_GRAVITY_COMPENSATION;
    }
}
