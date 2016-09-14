package org.openhab.binding.pca9685control.internal;

import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.bulldog.beagleboneblack.BBBNames;
import org.bulldog.core.gpio.Pwm;
import org.bulldog.core.io.bus.i2c.I2cBus;
import org.bulldog.core.platform.Board;
import org.bulldog.core.platform.Platform;
import org.bulldog.devices.servo.AdafruitServoDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCA9685PwmControl {
    private I2cBus bus;
    private AdafruitServoDriver servoDriver;
    public static final float MIN_ANGLE_DEFAULT = 0.012f;
	public static final float MAX_ANGLE_DEFAULT = 0.119f;
	public static final int DEGREE_MILLISECONDS = 6;
	
    private TreeMap<Integer, Servo> servos = new TreeMap<>();
    private ScheduledExecutorService disablePinWorker;
    
    private static final Logger logger = 
    		LoggerFactory.getLogger(pca9685controlBinding.class);	

    public PCA9685PwmControl(int I2CAddress){
    	logger.debug("PCA9685PwmControl: Constructor!");
        Board board = Platform.createBoard();
		bus = board.getI2cBus(BBBNames.I2C_1); //Select I2CBus
		servoDriver = new AdafruitServoDriver(bus, I2CAddress);
		disablePinWorker = Executors.newSingleThreadScheduledExecutor();
    }
    
    private Servo getServo(int pin, int initialAngle, float minAngle, float maxAngle, int degreeMilliseconds) {
    	if(!servos.containsKey(pin)){
    		Pwm pwm = servoDriver.getChannel(pin);
			Servo servo = new Servo(pwm,
									initialAngle,
									MIN_ANGLE_DEFAULT,
									MAX_ANGLE_DEFAULT,
									DEGREE_MILLISECONDS,
									disablePinWorker);
			servos.put(pin, servo);
						
			return servo;
		} else {
			return servos.get(pin);
		}
    }

    public void setOn(int pin, float minAngle, float maxAngle, int degreeMilliseconds){
    	moveTo(pin, 100, minAngle, maxAngle, degreeMilliseconds);
    }

    public void setOff(int pin, float minAngle, float maxAngle, int degreeMilliseconds){
    	moveTo(pin, 0, minAngle, maxAngle, degreeMilliseconds);
    }

    public void moveTo(int pin, int value, float minAngle, float maxAngle, int degreeMilliseconds) {
    	logger.debug("PCA9685PwmControl: moveTo pin: {} value: {}", pin, value);
    	Servo servo = getServo(pin, value, minAngle, maxAngle, degreeMilliseconds);

    	if (!servo.isEnabled())
    		servo.enable();

    	servo.moveTo(value);
    	servo.disableDelayed(5);
    }
    
    public void dispose() {
    	if (disablePinWorker != null)
    		disablePinWorker.shutdownNow();
    }
}
