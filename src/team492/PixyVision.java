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

import java.util.ArrayList;

import org.opencv.core.Rect;

import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.SPI;
import frclib.FrcPixyCam;
import trclib.TrcPixyCam.ObjectBlock;
import trclib.TrcUtil;

public class PixyVision
{
    private static final boolean debugEnabled = false;

    // If last target rect is this old, its stale data.
    private static final double LAST_TARGET_RECT_FRESH_DURATION_SECONDS = 0.1;  // 100 msec

    public class TargetInfo
    {
        public Rect rect;
        public double xDistance;
        public double yDistance;
        public double angle;

        public TargetInfo(Rect rect, double xDistance, double yDistance, double angle)
        {
            this.rect = rect;
            this.xDistance = xDistance;
            this.yDistance = yDistance;
            this.angle = angle;
        }   //TargetInfo

        public String toString()
        {
            return String.format("Rect[%d,%d,%d,%d], xDistance=%.1f, yDistance=%.1f, angle=%.1f",
                rect.x, rect.y, rect.width, rect.height, xDistance, yDistance, angle);
        }
    }   //class TargetInfo

    public enum Orientation
    {
        NORMAL_LANDSCAPE,
        CLOCKWISE_PORTRAIT,
        ANTICLOCKWISE_PORTRAIT,
        UPSIDEDOWN_LANDSCAPE
    }   //enum Orientation

    private static final double PIXY_DISTANCE_SCALE = 2300.0;   //DistanceInInches*targetWidthdInPixels
    //CodeReview: why diagonal???
    private static final double TARGET_WIDTH_INCHES = 13.0 * Math.sqrt(2.0);// 13x13 square, diagonal is 13*sqrt(2) inches

    private FrcPixyCam pixyCamera;
    private Robot robot;
    private int signature;
    private Orientation orientation;
    private Rect lastTargetRect = null;
    private double lastTargetRectExpireTime = TrcUtil.getCurrentTime();

    private void commonInit(Robot robot, int signature, int brightness, Orientation orientation)
    {
        this.robot = robot;
        this.signature = signature;
        this.orientation = orientation;
        pixyCamera.setBrightness((byte)brightness);
    }   //commonInit

    public PixyVision(
        final String instanceName, Robot robot, int signature, int brightness, Orientation orientation,
        SPI.Port port)
    {
        pixyCamera = new FrcPixyCam(instanceName, port);
        commonInit(robot, signature, brightness, orientation);
    }   //PixyVision

    public PixyVision(
        final String instanceName, Robot robot, int signature, int brightness, Orientation orientation,
        I2C.Port port, int i2cAddress)
    {
        pixyCamera = new FrcPixyCam(instanceName, port, i2cAddress);
        commonInit(robot, signature, brightness, orientation);
    }   //PixyVision

    public void setEnabled(boolean enabled)
    {
        pixyCamera.setEnabled(enabled);
    }   //setEnabled

    public boolean isEnabled()
    {
        return pixyCamera.isEnabled();
    }   //isEnabled

    public boolean isTaskTerminatedAbnormally()
    {
        return pixyCamera.isTaskTerminatedAbnormally();
    }   //isTaskTerminatedAbnormally

    /**
     * This method gets the rectangle of the last detected target from the camera. If the camera does not have
     * any. It may mean the camera is still busy analyzing a frame or it can't find any valid target in a frame.
     * We can't tell the reason. If the camera is likely busying processing a frame, we will return the last
     * cached rectangle. Therefore, the last cached rectangle will have an expiration (i.e. cached data can be
     * stale). If the last cached data becomes stale, we will discard it and return nothing. Otherwise, we will
     * return the cached data. Of course we will return fresh data if the camera does return another rectangle,
     * in which case it will become the new cached data.
     *
     * @return rectangle of the detected target last received from the camera or last cached target if cached
     *         data has not expired. Null if no object was seen and last cached data has expired.
     */
    private Rect getTargetRect()
    {
        final String funcName = "getTargetRect";
        Rect targetRect = null;
        ObjectBlock[] detectedObjects = pixyCamera.getDetectedObjects();
        double currTime = TrcUtil.getCurrentTime();

        if (debugEnabled)
        {
            robot.globalTracer.traceInfo(
                funcName, "%s object(s) found", detectedObjects != null? "" + detectedObjects.length: "null");
        }

        if (detectedObjects != null && detectedObjects.length >= 1)
        {
            //
            // Make sure the camera detected at least one objects.
            //
            ArrayList<Rect> objectList = new ArrayList<>();
            //
            // Filter out objects that don't have the correct signature.
            //
            for (int i = 0; i < detectedObjects.length; i++)
            {
                if (signature == detectedObjects[i].signature)
                {
                    int temp;
                    //
                    // If we have the camera mounted in other orientations, we need to adjust the object rectangles
                    // accordingly.
                    //
                    switch (orientation)
                    {
                        case CLOCKWISE_PORTRAIT:
                            temp = RobotInfo.PIXYCAM_WIDTH - detectedObjects[i].centerX;
                            detectedObjects[i].centerX = detectedObjects[i].centerY;
                            detectedObjects[i].centerY = temp;
                            temp = detectedObjects[i].width;
                            detectedObjects[i].width = detectedObjects[i].height;
                            detectedObjects[i].height = temp;
                            break;

                        case ANTICLOCKWISE_PORTRAIT:
                            temp = detectedObjects[i].centerX;
                            detectedObjects[i].centerX = RobotInfo.PIXYCAM_HEIGHT - detectedObjects[i].centerY;
                            detectedObjects[i].centerY = temp;
                            temp = detectedObjects[i].width;
                            detectedObjects[i].width = detectedObjects[i].height;
                            detectedObjects[i].height = temp;
                            break;

                        case UPSIDEDOWN_LANDSCAPE:
                            detectedObjects[i].centerX = RobotInfo.PIXYCAM_WIDTH - detectedObjects[i].centerX;
                            detectedObjects[i].centerY = RobotInfo.PIXYCAM_HEIGHT - detectedObjects[i].centerY;
                            break;

                        case NORMAL_LANDSCAPE:
                            break;
                    }

                    Rect rect = new Rect(detectedObjects[i].centerX - detectedObjects[i].width/2,
                                         detectedObjects[i].centerY - detectedObjects[i].height/2,
                                         detectedObjects[i].width, detectedObjects[i].height);
                    objectList.add(rect);

                    if (debugEnabled)
                    {
                        robot.globalTracer.traceInfo(funcName, "[%d] %s", i, detectedObjects[i].toString());
                    }
                }
            }

            if (objectList.size() >= 1)
            {
                //
                // Find the largest target rect in the list.
                //
                Rect maxRect = objectList.get(0);
                for(Rect rect: objectList)
                {
                    double area = rect.width * rect.height;
                    if (area > maxRect.width * maxRect.height)
                    {
                        maxRect = rect;
                    }
                }

                targetRect = maxRect;

                if (debugEnabled)
                {
                    robot.globalTracer.traceInfo(funcName, "===TargetRect===: x=%d, y=%d, w=%d, h=%d",
                        targetRect.x, targetRect.y, targetRect.width, targetRect.height);
                }
            }

            if (targetRect == null)
            {
                robot.globalTracer.traceInfo(funcName, "===TargetRect=== None, is now null");
            }

            lastTargetRect = targetRect;
            lastTargetRectExpireTime = currTime + LAST_TARGET_RECT_FRESH_DURATION_SECONDS;
        }
        else if (currTime < lastTargetRectExpireTime)
        {
            targetRect = lastTargetRect;
        }

        return targetRect;
    }   //getTargetRect

    public TargetInfo getTargetInfo()
    {
        final String funcName = "getTargetInfo";
        TargetInfo targetInfo = null;
        Rect targetRect = getTargetRect();

        if (targetRect != null)
        {
            //
            // Physical target width:           W = 10 inches.
            // Physical target distance 1:      D1 = 20 inches.
            // Target pixel width at 20 inches: w1 = 115
            // Physical target distance 2:      D2 = 24 inches
            // Target pixel width at 24 inches: w2 = 96
            // Camera lens focal length:        f
            //    W/D1 = w1/f and W/D2 = w2/f
            // => f = w1*D1/W and f = w2*D2/W
            // => w1*D1/W = w2*D2/W
            // => w1*D1 = w2*D2 = PIXY_DISTANCE_SCALE = 2300
            //
            // Screen center X:                 Xs = 320/2 = 160
            // Target center X:                 Xt
            // Heading error:                   e = Xt - Xs
            // Turn angle:                      a
            //    tan(a) = e/f
            // => a = atan(e/f) and f = w1*D1/W
            // => a = atan((e*W)/(w1*D1))
            //
            double targetCenterX = targetRect.x + targetRect.width/2.0;
            double targetXDistance = (targetCenterX - RobotInfo.PIXYCAM_WIDTH/2.0)*TARGET_WIDTH_INCHES/targetRect.width;
            double targetYDistance = PIXY_DISTANCE_SCALE/targetRect.width;
            double targetAngle = Math.toDegrees(Math.atan(targetXDistance/targetYDistance));
            targetInfo = new TargetInfo(targetRect, targetXDistance, targetYDistance, targetAngle);

            if (debugEnabled)
            {
                robot.globalTracer.traceInfo(
                    funcName, "###TargetInfo###: xDist=%.1f, yDist=%.1f, angle=%.1f",
                    targetXDistance, targetYDistance, targetAngle);
            }
        }

        if (robot.ledIndicator != null)
        {
            if (targetInfo != null)
            {
                if(Math.abs(targetInfo.xDistance) <= 2.0)
                {
                    robot.ledIndicator.indicateAlignedToCube();
                }
                else
                {
                    robot.ledIndicator.indicateSeesCube();
                }
            }
            else
            {
                robot.ledIndicator.indicateSeesNoCube();
                robot.ledIndicator.indicateNotAlignedToCube();
            }
        }

        return targetInfo;
    }   //getTargetInfo

}   // class PixyVision
