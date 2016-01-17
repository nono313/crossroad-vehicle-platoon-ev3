package main;

import java.io.IOException;

import lejos.hardware.lcd.LCD;
import robot.GenericRobot;
import robot.LeaderRobot;
import robot.MiddleRobot;
import robot.QueueRobot;
import utils.RobotMenu;

/**
 * Main function
 * Defines the role of the robot
 * @author Nathan Olff and Felix Lahemade
 */
public class MainTrain {

	public static void main(String[] args) {
			
		int behaviour;
		/* Show menu for choosing robot type */
		behaviour = RobotMenu.drawMenu();
		
		GenericRobot I = null;
		
		/* Create corresponding robot  */
		if(behaviour==1){
			I = new LeaderRobot();
			I.setBehaviour("Leader");
		}else if(behaviour==2){
			I = new MiddleRobot();
			I.setBehaviour("Middle");
		}else if(behaviour == 3){
			I = new QueueRobot();
			I.setBehaviour("Queue");
		}else{
			System.out.println("Error lol.");
			return;
		}
		
		/* Display behavior */
		LCD.clear();
		LCD.drawString(I.getBehaviour(), 1, 1);
		
		try {
			/* Starts the robot */
			I.live();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
