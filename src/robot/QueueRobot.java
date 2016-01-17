package robot;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import utils.Constants;
import utils.Message;

/**
 * QueueRobot class
 * 
 * <p>Same as MiddleRobot with the addition of the end crossing detection</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 */
public class QueueRobot extends MiddleRobot {

	private boolean inCrossingMyself = false;
	
	/**
	 * Default constructor
	 */
	public QueueRobot() {
		super();
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
			
			if (this.speedPolynom != null) {
				/* 
				 * If a speedPolynom is defined, computer the value of the function for the current time
				 */
				Button.LEDPattern(1);
				this.newSpeed = this.speedPolynom.getValue(lastOrangeTimestamp)/100*leftMotor.getMaxSpeed() *Math.PI/180.*(Constants.WHEEL_SIZE/2.);
				if(leftMotor.getTachoCount()-speedPolynomTachoCount >= 1500) {
					speedPolynom = null;
				}
			}
			else {
				Button.LEDPattern(5);
				selectSpeedPercentage();				
			}
			
			/* When we have left the crossing, send a message to the train */
			if(inCrossing && inCrossingMyself && spin > 6 && spin < 7) {
				inCrossing = false;
				inCrossingMyself = false;
				sendToItsTrain(new Message("trainOutOfCrossing",null));
			}
			//*/
			
			/*
			LCD.clear();
			LCD.drawString("color : "+color, 1, 1);
			LCD.drawString("color[0]:"+Float.toString(colorValue[0]),1,2);
			LCD.drawString("color[1]:"+Float.toString(colorValue[1]),1,3);
			LCD.drawString("color[2]:"+Float.toString(colorValue[2]),1,4);
			//*/
			LCD.drawString("position:"+Float.toString(position),1,1);
			LCD.drawString(" tour : "+Float.toString(spin),1,2);
			LCD.drawString(" orange = " + Integer.toString(numberOfOrange),1,3);
			
			LCD.drawString("current "+currentCount, 1,4);
			LCD.drawString("crossing ? "+ inCrossing, 1, 5);
			
			forward();	// Apply each motor's speed
		}
	}
	
	/**
	 * Manage its own inCrossing variable for sending trainOutOfCrossing messages.
	 * 
	 * <p>This is necessary to avoid sending the same messages twice</p>
	 */
	public void orangeMark() {
		inCrossingMyself = true;
	}
}
