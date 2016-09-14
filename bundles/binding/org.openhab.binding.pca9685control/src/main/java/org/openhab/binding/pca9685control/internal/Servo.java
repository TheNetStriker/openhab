package org.openhab.binding.pca9685control.internal;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bulldog.core.gpio.Pwm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Servo extends org.bulldog.devices.servo.Servo {

	private ScheduledExecutorService disablePinWorker;
	private ScheduledFuture<?> disablePinFuture;
	
	private static final Logger logger = 
    		LoggerFactory.getLogger(Servo.class);
	private Pwm pwm;
	
	public Servo(Pwm pwm, ScheduledExecutorService disablePinWorker) {
		super(pwm);
		logger.debug("Servo: Constructor! pwm", pwm);
		this.pwm = pwm;
		this.disablePinWorker = disablePinWorker;
	}
	
	public Servo(Pwm pwm, float initialAngle, ScheduledExecutorService disablePinWorker) {
		super(pwm, initialAngle);
		this.pwm = pwm;
		this.disablePinWorker = disablePinWorker;
	}
	
	public Servo(Pwm pwm, double initialAngle, double minAngleDuty, double maxAngleDuty, int degreeMilliseconds, ScheduledExecutorService disablePinWorker) {
		super(pwm, initialAngle, minAngleDuty, maxAngleDuty, degreeMilliseconds);
		this.pwm = pwm;
		this.disablePinWorker = disablePinWorker;
	}

	public boolean isEnabled() {
		return this.pwm.isEnabled();
	}
	
	public void enable() {
		this.pwm.enable();
	}
	
	public void disable() {
		this.pwm.disable();
	}
	
	public void disableDelayed(int delayInSeconds) {
		Runnable task = new Runnable() {
		    public void run() {
		    	disable();
				logger.debug("Servo: disableDelayed pin", pwm.getPin());
		    }
		};
		
		if (disablePinFuture != null && !disablePinFuture.isDone())
			disablePinFuture.cancel(true);
		
		disablePinFuture = disablePinWorker.schedule(task, 5, TimeUnit.SECONDS);
	}
}
