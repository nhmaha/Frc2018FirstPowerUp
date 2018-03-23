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

package team492;

import java.util.Date;

import common.CmdPidDrive;
import common.CmdTimedDrive;
import frclib.FrcChoiceMenu;
import hallib.HalDashboard;
import trclib.TrcRobot;

public class FrcAuto implements TrcRobot.RobotMode
{
    public static enum AutoStrategy
    {
        // Different choices for autonomous
        AUTO_SWITCH,
        AUTO_SCALE,
        X_TIMED_DRIVE,
        Y_TIMED_DRIVE,
        X_DISTANCE_DRIVE,
        Y_DISTANCE_DRIVE,
        TURN_DEGREES,
        DO_NOTHING
    } // enum AutoStrategy

    public static enum StartPosition
    {
        // Different choices for start positions
        LEFT_START_POS,
        MID_START_POS,
        RIGHT_START_POS
    } // enum StartPosition

    public static enum YesOrNo
    {
        YES,
        NO
    } // enum YesOrNo

    public static enum Lane
    {
        // Different choices for crossing lanes
        LANE1,
        LANE2,
        LANE3,
        CUSTOM
    } // enum Lane

    private Robot robot;

    //
    // Menus.
    //
    private FrcChoiceMenu<AutoStrategy> autoStrategyMenu;
    private FrcChoiceMenu<StartPosition> startPositionMenu;
    private FrcChoiceMenu<YesOrNo> fastDeliveryMenu;
    private FrcChoiceMenu<Lane> laneMenu;

    private AutoStrategy autoStrategy;
    private StartPosition startPosition;
    private boolean fastDelivery;
    private Lane lane;
    private double delay;

    private double robotStartPosition;
    private double forwardDriveDistance;

    private TrcRobot.RobotCommand autoCommand;

    public FrcAuto(Robot robot)
    {
        this.robot = robot;
        //
        // Create Autonomous Mode specific menus.
        //
        autoStrategyMenu = new FrcChoiceMenu<>("Auto/AutoStrategies");
        startPositionMenu = new FrcChoiceMenu<>("Auto/StartPos");
        fastDeliveryMenu = new FrcChoiceMenu<>("Auto/FastDelivery");
        laneMenu = new FrcChoiceMenu<>("Auto/Lanes");

        //
        // Populate Autonomous Mode menus.
        //
        autoStrategyMenu.addChoice("Auto Switch", AutoStrategy.AUTO_SWITCH, true, false);
        autoStrategyMenu.addChoice("Auto Scale", AutoStrategy.AUTO_SCALE, false, false);
        autoStrategyMenu.addChoice("X Timed Drive", AutoStrategy.X_TIMED_DRIVE, false, false);
        autoStrategyMenu.addChoice("Y Timed Drive", AutoStrategy.Y_TIMED_DRIVE, false, false);
        autoStrategyMenu.addChoice("X Distance Drive", AutoStrategy.X_DISTANCE_DRIVE, false, false);
        autoStrategyMenu.addChoice("Y Distance Drive", AutoStrategy.Y_DISTANCE_DRIVE, false, false);
        autoStrategyMenu.addChoice("Turn Degrees", AutoStrategy.TURN_DEGREES, false, false);
        autoStrategyMenu.addChoice("Do Nothing", AutoStrategy.DO_NOTHING, false, true);

        startPositionMenu.addChoice("Left Side Start", StartPosition.LEFT_START_POS, true, false);
        startPositionMenu.addChoice("Middle Start", StartPosition.MID_START_POS, false, false);
        startPositionMenu.addChoice("Right Side Start", StartPosition.RIGHT_START_POS, false, true);

        fastDeliveryMenu.addChoice("Yes", YesOrNo.YES, true, false);
        fastDeliveryMenu.addChoice("No", YesOrNo.NO, false, true);

        laneMenu.addChoice("Lane 1", Lane.LANE1, false, false);
        laneMenu.addChoice("Lane 2", Lane.LANE2, true, false);
        laneMenu.addChoice("Lane 3", Lane.LANE3, false, false);
        laneMenu.addChoice("Custom Distance", Lane.CUSTOM, false, true);
    } // FrcAuto

    //
    // Implements TrcRobot.RunMode.
    //

    @Override
    public void startMode()
    {
        if (Robot.USE_TRACELOG)
            robot.startTraceLog(null);

        Date now = new Date();
        robot.tracer.traceInfo(Robot.programName, "%s: ***** Starting autonomous *****", now.toString());
        robot.tracer.traceInfo(Robot.programName, "%s_%s_%3d (%s%d) [FMSConnected=%b]", robot.eventName,
            robot.matchType.toString(), robot.matchNumber, robot.alliance.toString(), robot.location,
            robot.ds.isFMSAttached());

        robot.dashboard.clearDisplay();

        robot.encoderYPidCtrl.setOutputLimit(0.6);
        robot.encoderXPidCtrl.setOutputLimit(RobotInfo.DRIVE_MAX_XPID_POWER);

        //
        // Retrieve menu choice values.
        //
        autoStrategy = autoStrategyMenu.getCurrentChoiceObject();

        startPosition = startPositionMenu.getCurrentChoiceObject();
        switch (startPosition)
        {
            case LEFT_START_POS:
                robotStartPosition = RobotInfo.LEFT_START_POS;
                break;
            case MID_START_POS:
                robotStartPosition = RobotInfo.MID_START_POS;
                break;
            case RIGHT_START_POS:
                robotStartPosition = RobotInfo.RIGHT_START_POS;
                break;
        }

        fastDelivery = fastDeliveryMenu.getCurrentChoiceObject() == YesOrNo.YES;

        lane = laneMenu.getCurrentChoiceObject();
        switch (lane)
        {
            case LANE1:
                forwardDriveDistance = RobotInfo.FWD_DISTANCE_1;
                break;

            case LANE2:
                forwardDriveDistance = RobotInfo.FWD_DISTANCE_2;
                break;

            case LANE3:
                forwardDriveDistance = RobotInfo.FWD_DISTANCE_3;
                break;

            case CUSTOM:
                forwardDriveDistance = -1.0;
                break;
        }

        delay = HalDashboard.getNumber("Auto/Delay", 0.0);

        switch (autoStrategy)
        {
            case AUTO_SWITCH:
                autoCommand = new CmdAutoSwitch(
                    robot, delay, forwardDriveDistance, robotStartPosition, fastDelivery);
                break;

            case AUTO_SCALE:
                autoCommand = new CmdAutoScale(robot, delay, robotStartPosition, forwardDriveDistance);
                break;

            case X_TIMED_DRIVE:
                autoCommand = new CmdTimedDrive(robot, delay, robot.driveTime, robot.drivePower, 0.0, 0.0);
                break;

            case Y_TIMED_DRIVE:
                autoCommand = new CmdTimedDrive(robot, delay, robot.driveTime, 0.0, robot.drivePower, 0.0);
                break;

            case X_DISTANCE_DRIVE:
                autoCommand = new CmdPidDrive(
                    robot, robot.pidDrive, robot.encoderXPidCtrl, robot.encoderYPidCtrl, robot.gyroTurnPidCtrl,
                    delay, robot.driveDistance, 0.0, 0.0, robot.drivePowerLimit, false);
                break;

            case Y_DISTANCE_DRIVE:
                autoCommand = new CmdPidDrive(
                    robot, robot.pidDrive, robot.encoderXPidCtrl, robot.encoderYPidCtrl, robot.gyroTurnPidCtrl,
                    delay, 0.0, robot.driveDistance, 0.0, robot.drivePowerLimit, false);
                break;

            case TURN_DEGREES:
                autoCommand = new CmdPidDrive(
                    robot, robot.pidDrive, robot.encoderXPidCtrl, robot.encoderYPidCtrl, robot.gyroTurnPidCtrl,
                    delay, 0.0, 0.0, robot.turnDegrees, robot.drivePowerLimit, false);
                break;

            case DO_NOTHING:
                autoCommand = null;
                break;
        }

        robot.setVisionEnabled(true);
        robot.driveBase.resetPosition();
        robot.targetHeading = 0.0;
    } // startMode

    @Override
    public void stopMode()
    {
    	robot.diagnostics.printDiagnostics();
        robot.setVisionEnabled(false);
    } // stopMode

    @Override
    public void runPeriodic(double elapsedTime)
    {
        robot.updateDashboard();
        robot.announceSafety();
        robot.diagnostics.doPeriodicTests();
    } // runPeriodic

    @Override
    public void runContinuous(double elapsedTime)
    {
        if (autoCommand != null)
        {
            autoCommand.cmdPeriodic(elapsedTime);

            if (robot.pidDrive.isActive())
            {
                robot.encoderXPidCtrl.printPidInfo(robot.tracer, elapsedTime, robot.battery);
                robot.encoderYPidCtrl.printPidInfo(robot.tracer, elapsedTime, robot.battery);
                robot.gyroTurnPidCtrl.printPidInfo(robot.tracer, elapsedTime, robot.battery);
            }
            if(robot.elevator.elevator.isActive())
            {
                robot.elevator.elevatorPidCtrl.printPidInfo(robot.tracer, elapsedTime, robot.battery);
            }
        }
    } // runContinuous

} // class FrcAuto
