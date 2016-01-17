package utils;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;


/**
 * RobotMenu class
 * 
 * <p>Used for displaying a menu for the user to choose which role the robot should have</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 *
 */
public abstract class RobotMenu {
	
	/**
	 * Draw the menu asking the user for a role
	 * @return the role selected by the user
	 */
	public static int drawMenu(){
		
		//Create menu
		LCD.drawString("Top-Leader",1,1);
		LCD.drawString("Enter-Generic", 1, 2);
		LCD.drawString("Bottom-Queue", 1, 3);
		
		int behaviour = 0;
		
		// Choose behavior
		
		int pressed = Button.waitForAnyPress();
		switch(pressed) {
		case Button.ID_UP:
			behaviour = 1;
			break;
			
		case Button.ID_ENTER:
			behaviour = 2;
			break;
			
		case Button.ID_DOWN:
			behaviour = 3;
			break;
		
		default:
			break;
		}
		
		return behaviour;
		
	}
}
