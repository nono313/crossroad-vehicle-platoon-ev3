package robot;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import utils.Constants;

/**
 * MiddleRobot class
 * 
 * <p>Base class for following robots (not leader)</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 */
public class MiddleRobot extends GenericRobot {
	
	/*
	 * Threshold used for the all or nothing politic.
	 */
	protected float threshold;
	
	/*
	 * Parameters used in the speed computations for to a point and to two
	 * points politics
	 */
	protected double a, D;
	
	public MiddleRobot() {
		super();
		
		prevDistance = 0;
		
		/*
		 *  After a few tries, we found out that if we wanted to have a distance
		 * 	of around 15cm, we had to pass a bigger value to the threshold parameter
		 */
		threshold = (float) 0.20;
		
		/*
		 * Initialize the attributes used for the speed computation 
		 */
		a = 100.;
		D = 0.30;	/* Inter-distance between two cars */
	}
	
	/*
	 * Move function specific to the Leader's behavior
	 * 
	 * (non-Javadoc)
	 * @see src.robot.GenericRobot#move()
	 */
	@Override
	public void move() {
		/* 
		 * Number of iteration needs to be changed according to the cycle value 
		 * 	in order to have the same sample size.
		 */

		leftMotor.resetTachoCount();
		rightMotor.resetTachoCount();
		while(true) {
			getNewMessages();
			
			followLine();
			/*
			if(inCrossing) {
				
				} else {
					Button.LEDPattern(3);
					this.newSpeed = this.speedToReach;
				}
				this.speedPercentage = newSpeed;
			}
		//*/
			if (this.speedPolynom != null) {
				/* 
				 * If a speedPolynom is defined, computer the value of the function for the current time
				 */
				Button.LEDPattern(1);
				this.newSpeed = this.speedPolynom.getValue(lastOrangeTimestamp)/100*leftMotor.getMaxSpeed() *Math.PI/180.*(Constants.WHEEL_SIZE/2.);;
				if(leftMotor.getTachoCount()-speedPolynomTachoCount >= 1500) {
					speedPolynom = null;
				}
			}
			else {
				Button.LEDPattern(5);
				selectSpeedPercentage();				
			}
			
			
			LCD.drawString("position:"+Float.toString(position),1,1);
			LCD.drawString(" tour : "+Float.toString(spin),1,2);
			LCD.drawString(" orange = " + Integer.toString(numberOfOrange),1,3);
			
			LCD.drawString("current "+currentCount, 1,4);
			LCD.drawString("crossing ? "+ inCrossing, 1, 5);

			forward();	// Apply motor's speeds
		}
	}

	/**
	 * Select speed percentage for each motor's using the distance sensor
	 */
	protected void selectSpeedPercentage() {
		dist = distance();
		switch(currentPolitic) {
		case ALL_OR_NOTHING:
			/* 
			 * We only change the state of the motors when the threshold is reached.
			 */
			if(dist >= threshold && prevDistance < threshold) {
				forward();	// ALL
			}
			else if(dist < threshold && prevDistance >= threshold) {	
				stop();	// NOTHING
			}
			
			break;
		case TO_A_POINT:
			/* 
			 * Set the speed percentage and then call forward to apply it
			 */
			speedPercentage = Math.max(Math.min(60, a*(dist-D)), 0);
			//forward();
			break;
		case TO_TWO_POINTS:
			/* 
			 * Set the speed percentage and then call forward to apply it
			 */
			speedPercentage = Math.min(Math.max(2.5*(dist-20), Math.min(Math.max(a*(dist-D),0),speedPercentage)),50);
			//forward();
			break;
		case WIRELESS_COMMUNICATION:
			/* Not implemented */
			break;
		default:
			break;
		}
		prevDistance = dist;
	}
}
