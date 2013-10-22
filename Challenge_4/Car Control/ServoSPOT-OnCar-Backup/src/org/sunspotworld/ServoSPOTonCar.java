/*
 * Copyright (c) 2006-2010 Sun Microsystems, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to 
 * deal in the Software without restriction, including without limitation the 
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
 * sell copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 **/
package org.sunspotworld;

import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.Servo;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.Utils;
import java.io.IOException;
import com.sun.squawk.util.MathUtils;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.sunspotworld.common.Globals;
import org.sunspotworld.common.TwoSidedArray;
import org.sunspotworld.lib.BlinkenLights;


/**
 * This class is used to move a servo car consisting of two servos - one for
 * left wheel and the other for right wheel. To combine these servos properly,
 * this servo car moves forward/backward, turn right/left and rotate
 * clockwise/counterclockwise.
 *
 * The current implementation has 3 modes and you can change these "moving mode"
 * by pressing sw1. Mode 1 is "Normal" mode moving the car according to the tilt
 * of the remote controller. Mode 2 is "Reverse" mode moving the car in a
 * direction opposite to Mode 1. Mode 3 is "Rotation" mode only rotating the car
 * clockwise or counterclockwise according to the tilt.
 *
 * @author Tsuyoshi Miyake <Tsuyoshi.Miyake@Sun.COM>
 * @author Yuting Zhang<ytzhang@bu.edu>
 */
public class ServoSPOTonCar extends MIDlet implements ISwitchListener {

    private static final int SERVO_CENTER_VALUE = 1500; // go straight (wheels, servo1), or stop (motor, servo2)
    private static final int SERVO1_MAX_VALUE = 2000; // max turn right
    private static final int SERVO1_MIN_VALUE = 1000; // max turn left
    private static final int SERVO2_MAX_VALUE = 2000; // go backwards fast
    private static final int SERVO2_MIN_VALUE = 1000; // go forward fast
    private static final int SERVO1_HIGH = 500; //steering step high
    private static final int SERVO1_LOW = 300; //steering step low
    private static final int SERVO2_HIGH = 50; //speeding step high
    private static final int SERVO2_LOW = 30; //speeding step low
    private static int SET_SPEED = 1500;
    private static double CAR_LENGTH = 10.0; // in 'IR' units, whatever those are
    private static double CONFIDENCE_DISTANCE = 10.0; // in 'IR' units
    // Devices
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    //private ISwitch sw = eDemo.getSwitches()[EDemoBoard.SW1];
    private IAnalogInput irRightFront = eDemo.getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput irLeftFront = eDemo.getAnalogInputs()[EDemoBoard.A1];
    private IAnalogInput irRightRear = eDemo.getAnalogInputs()[EDemoBoard.A2];
    private IAnalogInput irLeftRear = eDemo.getAnalogInputs()[EDemoBoard.A3];
    private ISwitch sw1 = eDemo.getSwitches()[EDemoBoard.SW1];
    private ISwitch sw2 = eDemo.getSwitches()[EDemoBoard.SW2];
    private int STOP = 0;
    private ITriColorLED[] leds = eDemo.getLEDs();
    private ITriColorLEDArray myLEDs = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    // 1st servo for left & right direction 
    private Servo servo1 = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    // 2nd servo for forward & backward direction
    private Servo servo2 = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]);
    private BlinkenLights progBlinker = new BlinkenLights(0, 3);
    private BlinkenLights velocityBlinker = new BlinkenLights(4, 7);
    private int current1 = SERVO_CENTER_VALUE;
    private int current2 = SERVO_CENTER_VALUE;
    private int step1 = SERVO1_LOW;
    private int step2 = SERVO2_LOW;
    private int direction = 0;
    private double sensor_distance = 31.75; // cm
    //private int servo1ForwardValue;
    //private int servo2ForwardValue;
    private int servo1Left = SERVO_CENTER_VALUE + SERVO1_LOW;
    private int servo1Right = SERVO_CENTER_VALUE - SERVO1_LOW;
    private int servo2Forward = SERVO_CENTER_VALUE + SERVO2_LOW;
    private int servo2Back = SERVO_CENTER_VALUE - SERVO2_LOW;
    private int default_turn_cycles = 5;
    boolean center = true;
    boolean reverse_LEDs = true;
    int ledIndex = 0;
    int leftLED = 0;
    int rightLED = 0;
    int leftRED = 0;
    int leftGREEN = 0;
    int leftBLUE = 0;
    int rightRED = 0;
    int rightGREEN = 0;
    int rightBLUE = 0;
    int confidence_distance = 10;
    

    public ServoSPOTonCar() {
    }

    public void switchReleased(SwitchEvent sw) {
        // do nothing
    }

    public int calculateIndex(double theta, boolean reverse) {
        theta = theta * 57.29; // convert radians to degrees
        double thetaRatio = 0.0;
        int index = 0;
        int thetaOffset = 0;
        if (theta > 45.0) {
            theta = 45.0;
        } else if (theta < -45.0) {
            theta = -45.0;
        }
        if (theta >= 0.0) {
            thetaRatio = theta / 45.1;
            thetaOffset = (int) (thetaRatio * 4);
            index = (3 - thetaOffset);
        } else {
            thetaRatio = -theta / 45.1;
            thetaOffset = (int) (thetaRatio * 4);
            index = (4 + thetaOffset);
        }
        if (reverse) {
            return invertLEDIndex(index);
        } else {
            return index;
        }

    }

    public int invertLEDIndex(int LED_index) { // 0 - 7, 1 - 6,
        if (LED_index == 7) {
            return 0;
        } else {
            return (-(LED_index - 7));
        }
    }

    public double distanceToWall(double reading, double theta) { //IR
        return Math.cos(theta) * reading;
    }

    public double getWallAngle(double readingFront, double readingBack) { //IR
        return MathUtils.atan2((readingBack - readingFront), CAR_LENGTH);
    }

    public void setLED_RED(int value, ITriColorLED led) {
        int red = value;
        int green = led.getGreen();
        int blue = led.getBlue();
        led.setRGB(red, green, blue);
    }

    public void setLED_BLUE(int value, ITriColorLED led) {
        int red = led.getRed();
        int green = led.getGreen();
        int blue = value;
        led.setRGB(red, green, blue);
    }

    public void setLED_GREEN(int value, ITriColorLED led) {
        int red = led.getRed();
        int green = value;
        int blue = led.getBlue();
        led.setRGB(red, green, blue);
    }

    public int calcRedBlue(double confidence1, double confidence2) {
        double conf1 = confidence1 * confidence1;
        double conf2 = confidence2 * confidence2;
        double confSquared = confidence1 * confidence2;
        int intensity = 255 - (int) (256.0 * confSquared);

        if (conf1 < .50 || conf2 < .50 ) {
            intensity = intensity / 2;
        }
        if (conf1 < .35 || conf2 < .35) {
            intensity = intensity / 2;
        }
        if (conf1 < .20 || conf2 < .20) {
            intensity = 0;
        }
        if (intensity > 255) {
            intensity = 255;
        } else if (intensity < 0) {
            intensity = 0;
        }
        return intensity;

    }

    public int calcGreen(double confidence1, double confidence2) {
        double conf1 = confidence1 * confidence1;
        double conf2 = confidence2 * confidence2;
        double confSquared = confidence1 * confidence2;
        int intensity = (int) (256.0 * confSquared);
        if (conf1 < .60 || conf2< .60) {
            intensity = intensity / 2;
        }
        if (conf1 < .45 || conf2 < .45) {
            intensity = 0;
        }
        if (intensity > 255) {
            intensity = 255;
        } else if (intensity < 0) {
            intensity = 0;
        }
        return intensity;

    }

    public void switchPressed(SwitchEvent sw) {
        if (sw.getSwitch() == sw1) {
            if (SET_SPEED > 1000) {
                SET_SPEED -= 100;
            } else {
                SET_SPEED = 1500;
            }
            /*if (direction == 0) {
             direction = 1;
             } else if (direction == 1 || direction == -1) {
             direction = 0;
             }*/


        } else if (sw.getSwitch() == sw2) {
            STOP = 1;
            servo2.setValue(1500);
            //if (direction == 0) {
            //  direction = -1;
            //} else if (direction == 1 || direction == -1) {
            //   direction = 0;
            //}
        }
    }

    /**
     * BASIC STARTUP CODE *
     */
    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);
        BootloaderListenerService.getInstance().start();


        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.getLED(i).setColor(LEDColor.GREEN);

        }
        Utils.sleep(500);
        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.getLED(i).setOff();
        }

        /*for (int i = 0; i < myLEDs.size(); i++) {
         myLEDs.getLED(i).setColor(LEDColor.BLUE);
         }
         * */
        //setServoForwardValue();
        //progBlinker.startCylon();
        //velocityBlinker.startCylon();
        // timeout 1000
        /*
         //TwoSidedArray robot = new TwoSidedArray(getAppProperty("buddyAddress"), Globals.READ_TIMEOUT);
         try {
         robot.startInput();
         } catch (Exception e) {
         e.printStackTrace();
         }
         */


        //velocityBlinker.setColor(LEDColor.BLUE);
        //progBlinker.setColor(LEDColor.BLUE);


        boolean error = false;
        while (STOP != 1) {
            //boolean timeoutError = robot.isTimeoutError();
            //int st = 0;
            int rl = 0;
            //if (!timeoutError) {
            //rl = robot.getVal(0);
            /*System.out.println(" Right front sensor distance: " + getDistance(irRightFront));
             System.out.println(" Left front sensor distance: " + getDistance(irLeftFront));
             System.out.println(" Right rear sensor distance: " + getDistance(irRightRear));
             System.out.println(" Right rear sensor distance: " + getDistance(irLeftRear));*/
            //rl = leftOrRight();
            double RR = getDistance(irRightRear);
            double RF = getDistance(irRightFront);
            double LF = getDistance(irLeftFront);
            double LR = getDistance(irLeftRear);

            double thetaRight = getWallAngle(RR, RF);
            double thetaLeft = getWallAngle(LF, LR);
            double confidenceRF = (10 / RF);
            double confidenceRR = (10 / RR);
            double confidenceLF = (10 / LF);
            double confidenceLR = (10 / LR);
            double distanceLeft = (distanceToWall(LR, thetaLeft) + distanceToWall(LF, thetaLeft)) / 2;
            double distanceRight = (distanceToWall(RR, thetaRight) + distanceToWall(RF, thetaRight)) / 2;

            leftLED = calculateIndex(thetaLeft, reverse_LEDs);
            //.out.println("Left LED: " + leftLED);
            rightLED = calculateIndex(thetaRight, reverse_LEDs);
            //System.out.println("Right LED: " + rightLED);
            leftRED = calcRedBlue(confidenceLF, confidenceLR);
            //System.out.println("leftRed" + leftRED);
            leftGREEN = calcGreen(confidenceLF, confidenceLR);
            //System.out.println("leftGreen" + leftGREEN);
            leftBLUE = 0;
            //System.out.println("leftBLUE" + leftBLUE);
            rightBLUE = calcRedBlue(confidenceRF, confidenceRR);
            //System.out.println("rightBLUE" + rightBLUE);
            rightGREEN = calcGreen(confidenceRF, confidenceRR);
            //System.out.println("rightGREEN" + rightGREEN);
            rightRED = 0;
            //System.out.println("rightRED" + rightRED);
            ITriColorLED currentLED;

            for (int i = 0; i < myLEDs.size(); i++) {
                currentLED = myLEDs.getLED(i);

                int newRed = 0;
                int newGreen = 0;
                int newBlue = 0;

                if (i == leftLED && i == rightLED) {
                    newRed = (leftRED);
                    if (leftGREEN > rightGREEN){
                        newGreen = leftGREEN;
                    }
                    else 
                        newGreen = rightGREEN;
                    newBlue = (leftBLUE);
                } else if (i == leftLED) {
                    newRed = leftRED;
                    newGreen = leftGREEN;
                    newBlue = leftBLUE;
                } else if (i == rightLED) {
                    newRed = rightRED;
                    newGreen = rightGREEN;
                    newBlue = rightBLUE;
                } else {
                    newRed = 0;
                    newGreen = 0;
                    newBlue = 0;
                }
                currentLED.setRGB(newRed, newGreen, newBlue);
                currentLED.setOn();
                //System.out.println("Setting LED" + currentLED + "to: " + newRed + ":" + newGreen + ":" + newBlue);
            }

        


         /*System.out.println("Theta Right: " + (thetaRight * 57.2957795));
         System.out.println("Theta Left: " + (thetaLeft * 57.2957795));
         System.out.println("Confidence Right: " + confidenceRight);
         System.out.println("Confidence Left: " + confidenceLeft);
         System.out.println("Distance Left: "  + distanceLeft);
         System.out.println("Distance Right: " + distanceRight);*/

        // theta = 0 degrees is straight ahead
        // thetaLeft = 45 degrees is turned sharply toward left wall
        // thetaLeft = -45 degrees is turned sharply away from left wall
        // thetaRight = 45 degrees is turned sharply toward right wall
        // thetaRight = -45 degrees is turned sharply away from right wall
        //setLEDAngleLeft(thetaLeft, confidenceLeft);
        //setLEDAngleRight(-thetaRight, confidenceRight);


        //st = robot.getVal(1);
        if (error) {
            step1 = SERVO1_LOW;
            step2 = SERVO2_LOW;
            //velocityBlinker.setColor(LEDColor.BLUE);
            //progBlinker.setColor(LEDColor.BLUE);
            error = false;
        }
        //System.out.println("Checking distance...");
        //checkDistance();
        //System.out.println("Done checking distance.");
            /*if (direction == 1) {
         System.out.println("Going forward...");
         System.out.println();
         forward();
         } else if (direction == -1) {
         System.out.println("Going backward...");
         backward();
         } else {
         stop();
         }*/
        forward();
        if (rl > 40) {
            System.out.println("Turning right...");
            //right();
        } else if (rl < -40) {
            System.out.println("Turning left...");
            //left();
        } else {
            goStraight();
        }
        /*} else {
         velocityBlinker.setColor(LEDColor.RED);
         progBlinker.setColor(LEDColor.RED);
         error = true;
         }*/
        Utils.sleep(50);
        if (STOP == 1) {
            servo2.setValue(1500);
        }
    }
}
private void setServoForwardValue() {
        servo1Left = current1 + step1;
        servo1Right = current1 - step1;
        servo2Forward = current2 + step2;
        servo2Back = current2 - step2;
        if (step2 == SERVO2_HIGH) {
            velocityBlinker.setColor(LEDColor.GREEN);
        } else {
            velocityBlinker.setColor(LEDColor.BLUE);
        }
    }

    private void offsetLeft() {
        System.out.println("offsetting left...");
        for (int i = 0; i < getTurnLength(); i++) {
            forward();
            left();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
        int fixTurn = (int) (getTurnLength() / 2);
        for (int i = 0; i < fixTurn; i++) {
            forward();
            right();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
        goStraight();

    }

    private int getTurnLength() {
        int speed_diff = 1500 - SET_SPEED; // max speed = 500; min speed = 100;
        int speed_diff_importance = speed_diff / 500; // max speed = 1; min speed = 1/5;
        int turn_length_adjust = (int) (speed_diff_importance * (default_turn_cycles / 2));
        return default_turn_cycles - turn_length_adjust;
    }

    private void offsetRight() {
        System.out.println("Offsetting right...");

        for (int i = 0; i < getTurnLength(); i++) {
            forward();
            right();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
        int fixTurn = (int) (getTurnLength() / 2);
        for (int i = 0; i < fixTurn; i++) {
            forward();
            left();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
        goStraight();

    }

    private void left() {
        System.out.println("left");
        current1 = servo1.getValue();
        if (current1 + step1 < SERVO1_MAX_VALUE) {
            servo1.setValue(current1 + step1);
            Utils.sleep(10);
        } else {
            servo1.setValue(SERVO1_MAX_VALUE);
            Utils.sleep(10);
        }
    }

    private void right() {
        System.out.println("right");
        current1 = servo1.getValue();
        if (current1 - step1 > SERVO1_MIN_VALUE) {
            servo1.setValue(current1 - step1);
            Utils.sleep(10);
        } else {
            servo1.setValue(SERVO1_MIN_VALUE);
            Utils.sleep(10);
        }
        //servo2.setValue(0);
    }

    private void stop() {
        //System.out.println("stop");
        //servo1.setValue(0);
        servo2.setValue(SERVO_CENTER_VALUE);
    }

    private void goStraight() {
        //System.out.println("stop");
        //servo1.setValue(0);
        servo1.setValue(SERVO_CENTER_VALUE);
    }

    private void backward() {
        //servo2.setValue(SERVO_CENTER_VALUE + step2);
        servo2.setValue(1600);
    }

    private void forward() {
        //servo2.setValue(SERVO_CENTER_VALUE - step2);
        servo2.setValue(SET_SPEED);
    }

    private int leftOrRight() {
        int rl_val = 0;
        double LF = getDistance(irLeftFront);
        double LR = getDistance(irLeftRear);
        double RF = getDistance(irRightFront);
        double RR = getDistance(irRightRear);
        double averageLeft = (LF + LR) / 2;
        double averageRight = (RF + RR) / 2;

        /*if (averageLeft < averageRight) {
         if (averageLeft < 20) {
         rl_val = 50;
         System.out.println("Setting RL high to turn right");
         } else {
         rl_val = 0;
         }
         } else if (averageRight < averageLeft) {
         if (averageRight < 20) {
         rl_val = -50;
         System.out.println("Setting RL low to turn left");
         } else {
         rl_val = 0;
         }
         } else {
         rl_val = 0;
         }*/

        if (averageLeft < averageRight) { // closer to left wall, so use left sensors
            double left_diff = LF - LR;
            double left_diff_importance = Math.abs(left_diff) / averageLeft;
            if (averageLeft > 30) { // If the left wall is closer, but farther than half the hall away, turn toward it. (Alcove on right!)
                rl_val = 0;
            } else {
                //System.out.println("Left diff importance" + left_diff_importance);
                if (left_diff_importance > .05) { // significant difference between front and back sensors
                    if (left_diff > 0) { // LR is closer to wall than LF
                        if (averageLeft < 15) {
                            rl_val = 0; // go forward -- you might be too close to correct angle!
                        } else {
                            rl_val = -50; // adjust the steering left to straighten out.
                        }
                    } else if (left_diff < 0) // LF is closer to wall than LR
                    {
                        rl_val = 50; // Set RL high to turn right
                    }
                } else if (averageLeft < 15) {
                    offsetRight(); // Continue going straight (for now!) Should replace with an offset-position method!
                } else {
                    rl_val = 0;
                }
            }

        } else if (averageRight < averageLeft) {
            double right_diff = RF - RR;
            double right_diff_importance = Math.abs(right_diff) / averageRight;
            if (averageRight > 30) { // If the right wall is closer, but farther than half the hall away, don't turn. (Alcove on left!)
                rl_val = 0;
            } else {
                //System.out.println("right difference" + right_diff_importance);
                if (right_diff_importance > .05) { // significant difference between front and back sensors
                    if (right_diff > 0) { // RR is closer to wall than RF
                        if (averageRight < 15) {
                            rl_val = 0; // go forward -- you might be too close to correct angle!
                        } else {
                            rl_val = 50; // turn right to straighten out
                        }
                    } else if (right_diff < 0) // LF is closer to wall than LR
                    {
                        rl_val = -50; // Set RL low to turn left
                    }
                } else if (averageRight < 15) {
                    offsetLeft();
                } else {
                    rl_val = 0; // Continue going straight (for now!) Should replace with an offset-position method!
                }
            }
        }
        return rl_val;
    }

    private void checkDistance() {
        if (getDistance(irLeftFront) < 20) {
            myLEDs.getLED(7).setColor(LEDColor.RED);
            System.out.println("Turning right.");
            right();
        } else if (getDistance(irRightFront) < 20) {
            myLEDs.getLED(0).setColor(LEDColor.RED);
            System.out.println("Turning left.");
            left();
        } else {
            goStraight();
            System.out.println("Going straight.");
        }

        /*if(getDistance(irFront) > 10 && getDistance(irFront) < 200){
         myLEDs.getLED(3).setColor(LEDColor.RED);
         step2 = SERVO2_LOW;
         forward();
         }else if(getDistance(irFront) <= 10){
         myLEDs.getLED(3).setColor(LEDColor.RED);
         stop();
         }else {
         step2 = SERVO2_HIGH;
         forward();
         }
         if(getDistance(irRear) > 10 && getDistance(irRear) < 200){
         myLEDs.getLED(4).setColor(LEDColor.RED);
         step2 = SERVO2_LOW;
         backward();
         }else if(getDistance(irFront) <= 10){
         myLEDs.getLED(3).setColor(LEDColor.RED);
         stop();
         }else{
         step2 = SERVO2_HIGH;
         backward();
         }*/
    }

    public double getDistance(IAnalogInput analog) {
        double volts = 0;
        try {
            volts = analog.getVoltage();
        } catch (IOException e) {
            System.err.println(e);
        }
        return 18.67 / (volts + 0.167);
    }

    /*public void switchPressed(SwitchEvent sw) {
     step1 = (step1 == SERVO1_HIGH) ? SERVO1_LOW : SERVO1_HIGH;
     step2 = (step2 == SERVO2_HIGH) ? SERVO2_LOW : SERVO2_HIGH;
     setServoForwardValue();
     }
    
     public void switchReleased(SwitchEvent sw) {
     // do nothing
     }*/
    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system. I.e. if startApp throws
     * any exception other than MIDletStateChangeException, if the isolate
     * running the MIDlet is killed with Isolate.exit(), or if VM.stopVM() is
     * called.
     *
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true when this method is called, the MIDlet must
     * cleanup and release all resources. If false the MIDlet may throw
     * MIDletStateChangeException to indicate it does not want to be destroyed
     * at this time.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.getLED(i).setOff();
        }
    }
}
