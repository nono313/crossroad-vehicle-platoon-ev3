package robot;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.TreeMap;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import utils.CarsBehavior;
import utils.Constants;
import utils.Message;
import utils.SpeedPolynom;

/**
 * LeaderRobot class
 * 
 * <p>First car of the train. The train itself is managed here</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 *
 */
public class LeaderRobot extends GenericRobot {
	/* Train's attributes */
	private int otherTrainId;	
	protected boolean otherTrainInCrossing;
	/**
	 * Map containing the coordinates (and speed) of the train's components 
	 */
	protected TreeMap<Integer, CarsBehavior> carsMap;	
	
	/**
	 * Default constructor
	 */
	public LeaderRobot() {
		super();
		/* Initialize attributes */
		otherTrainInCrossing = false;
		dist=0;
		newSpeed = 0;
		carsMap = new TreeMap<Integer, CarsBehavior>();
		speedToReach = Constants.TRAIN_NORMAL_SPEED;
	}
	
	/*
	 * Move function specific to the Leader's behavior
	 * 
	 * (non-Javadoc)
	 * @see src.robot.GenericRobot#move()
	 */
	@Override
	public void move() {
		position = 0;

		leftMotor.resetTachoCount();
		rightMotor.resetTachoCount();
		
		/* Set other train's id */
		if(carId == 11) {
			otherTrainId = 21;
		}
		else if(carId == 21) {
			otherTrainId = 11;
		}
	
		/* Main loop */
		while(true) {
			getNewMessages();
			followLine();
			
			if (this.speedPolynom != null) {
				/* 
				 * If a speedPolynom is defined, computer the value of the function for the current time
				 */
				Button.LEDPattern(1);
				this.newSpeed = this.speedPolynom.getValue(lastOrangeTimestamp)/100*leftMotor.getMaxSpeed() *Math.PI/180.*(Constants.WHEEL_SIZE/2.);;
				/* Only consider the speed polynom for about 1500 deg of the wheel */
				if(leftMotor.getTachoCount()-speedPolynomTachoCount >= Constants.DISTANCE_MARK_TO_CROSSING) {
					speedPolynom = null;
				}
			} else {
				Button.LEDPattern(3);
				this.newSpeed = this.speedToReach;
			}
			
			this.speedPercentage = newSpeed;
			
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
			
			forward();	// Apply change in motor's speeds
		}
		
	}
	
	/**
	 * Broadcast a message to all the vehicules within the train
	 * @param message to send
	 */
	public void broadcastMessageToOwnVehicules(Message message) {
		sendMessage(carId/10*10, message);
	}
	
	/**
	 * Send a message to the other train
	 * @param message to send
	 */
	public void sendToOtherTrain(Message message) {
		sendMessage(otherTrainId, message);
	}

	/*
	 * Action to do when an orange mark is detected
	 * (non-Javadoc)
	 * @see src.robot.GenericRobot#orangeMark()
	 */
	@Override
	public void orangeMark() {
		/* Send message to the train */
		sendToItsTrain(new Message("trainInCrossing", null));
		inCrossing = true;
	}
	
	/**
	 * Conflict crossing management
	 * @param otherCars : coordinates of all cars of the other train
	 */
	public void prepareCrossing(TreeMap<Integer, CarsBehavior> otherCars) {
		/* Write logs to a text file */
		try {
			file = new FileWriter("prepareCrossing.txt");
			writer = new PrintWriter(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Button.LEDPattern(1);
		
		/* Define variables used for conflict management */
		Double minTime = null;
		Double maxTime = null;
		Double optimalTime = null;
		Double adjustSpeed = null;
		double nbOrangeOtherLeader = -1;
		double firstOtherCarTimeToCrossing = -1;
		CarsBehavior firstOtherCarBehavior = null;
		TreeMap<Integer, Double> distanceFromCrossing = new TreeMap<Integer, Double>();
		TreeMap<Integer, Double> timeLeft = new TreeMap<Integer, Double>();
		double otherTrainAverageSpeed = 0;
		
		int j = 0;
		double myDistanceToCrossing = (Constants.MARK_CROSSING-position)*Constants.WHEEL_PERIMETER/360;
		double myRealSpeed = this.speedPercentage/100*leftMotor.getMaxSpeed() *Math.PI/180.*(Constants.WHEEL_SIZE/2.);
		double myTimeToCrossing = myDistanceToCrossing/ myRealSpeed;
		
		writer.println("size of map : " + otherCars.size());
		writer.println("myDistance :"+myDistanceToCrossing);
		writer.println("myRealSpeed :"+myRealSpeed);
		writer.println("myTimeToCrossing :"+myTimeToCrossing);
		
		/* For each element in the map... */
		for(Integer id : otherCars.keySet()) {
			CarsBehavior behavior = otherCars.get(id);
			double speed = behavior.getSpeed()/100*leftMotor.getMaxSpeed() *Math.PI/180.*(Constants.WHEEL_SIZE/2.);	//v = omega x r (omega in rad/s) -> m/s
			/* We try to get the first car of the other train that has not yet passed the crossing */
			if(firstOtherCarBehavior == null) {
				/* Distance between the robot and the crossing */
				double distance = (Constants.MARK_CROSSING-behavior.getPosition())*Constants.WHEEL_PERIMETER/360;
				if(nbOrangeOtherLeader == -1) {
					/* 
					 * Orange number is used to know if the car is close to the crossing or 
					 * if we need to add something to the position to get its distance from the crossing
					 */
					nbOrangeOtherLeader = behavior.getOrangeNumber();
				}
				/* Negative values mean that the car already passed the crossing */
				if(distance > 0) {
					try {
						firstOtherCarBehavior = (CarsBehavior) behavior.clone();
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
					if(nbOrangeOtherLeader == behavior.getOrangeNumber()) { // Both in the crossing after the orange mark
						distanceFromCrossing.put(id, (Constants.MARK_CROSSING-behavior.getPosition())*Constants.WHEEL_PERIMETER/360);
						timeLeft.put(id, distanceFromCrossing.get(id)/speed);
					}
					else if(behavior.getOrangeNumber() == nbOrangeOtherLeader-1){	// Second car before the mark
						distanceFromCrossing.put(id, (Constants.MARK_CROSSING + Constants.CIRCUIT_SIZE - behavior.getPosition())*Constants.WHEEL_PERIMETER/360);
						timeLeft.put(id, distanceFromCrossing.get(id)/speed);
					}
					firstOtherCarTimeToCrossing = distanceFromCrossing.get(id)/speed;
					otherTrainAverageSpeed += speed;
					j++;
				}
			}
			else {
				if(nbOrangeOtherLeader == behavior.getOrangeNumber()) { // Both in the crossing after the orange mark
					distanceFromCrossing.put(id, (Constants.MARK_CROSSING-behavior.getPosition())*Constants.WHEEL_PERIMETER/360);
					timeLeft.put(id, distanceFromCrossing.get(id)/speed);
				}
				else if(behavior.getOrangeNumber() == nbOrangeOtherLeader-1){	// Second car before the mark
					distanceFromCrossing.put(id, (Constants.MARK_CROSSING + Constants.CIRCUIT_SIZE - behavior.getPosition())*Constants.WHEEL_PERIMETER/360);
					timeLeft.put(id, distanceFromCrossing.get(id)/speed);
				}
				otherTrainAverageSpeed += speed;
				j++;
			}
			otherTrainAverageSpeed /= j;
			/*
			LCD.drawString("speed("+id+") :"+speed, 1, 1);
			LCD.drawString("distance("+id+")  :"+distanceFromCrossing.get(id), 1, 2);
			LCD.drawString("time("+id+") :"+timeLeft.get(id), 1, 3);
			LCD.drawString("position("+id+") :"+behavior.getPosition(), 1, 4);
			LCD.drawString("position("+id+") :"+behavior.getOrangeNumber(), 1, 5);
*/
			/* Log */
			writer.println("speed("+id+") :"+speed);
			writer.println("distance("+id+")  :"+distanceFromCrossing.get(id));
			writer.println("time("+id+") :"+timeLeft.get(id));
			writer.println("position("+id+") :"+behavior.getPosition());
			writer.println("orangeNumber("+id+") :"+behavior.getOrangeNumber());
			
		}
		if(firstOtherCarTimeToCrossing != -1) {	// If all cars have passed the crossing, we don't prepare anything
			for (Double timer : timeLeft.values()) {
					// if closest car to be in front of our train
					if(timer < myTimeToCrossing && (minTime == null ||timer > minTime)) {
						minTime = timer;
					// if closest car to go behind our train's first
					} 
					else if (timer >= myTimeToCrossing && (maxTime == null || maxTime > timer)) {
						maxTime = timer;
					}
	
			}
			if(maxTime == null) {
				maxTime = minTime + myTimeToCrossing;
				writer.println("Fixing null max");
			}
			if(minTime == null) {
				minTime = maxTime -0.15/otherTrainAverageSpeed;
				writer.println("Fixing null min");
			} 
				
			//minTime += 0.015;
			writer.println("max original : "+maxTime);
			minTime += 1;
			maxTime -= 2*0.015/otherTrainAverageSpeed;
			
			optimalTime = (minTime + maxTime)/2.;
			
			adjustSpeed = myDistanceToCrossing/optimalTime;
			
			writer.println("min :"+ minTime);
			writer.println("max :"+ maxTime);
			writer.println("optimal :"+ optimalTime);
			writer.println("adjSpeed :"+ adjustSpeed);
			
			/* Speed polynom computation */
			SpeedPolynom speedPol;
			double otherTrainSpeed = firstOtherCarBehavior.getSpeed()/100*leftMotor.getMaxSpeed() *Math.PI/180.*(Constants.WHEEL_SIZE/2.);
			if(Math.abs(adjustSpeed-otherTrainSpeed) < 5) {
				/* 
				 * If adjustSpeed and otherTrainSpeed are close, we just use the 
				 * average speed (adjustSpeed) and then change directly at the crossing.
				 */
				speedPol = new SpeedPolynom();
				speedPol.setCoeff(0, adjustSpeed);
			}
			else {
				speedPol = new SpeedPolynom();
				speedPol.interpolation(myRealSpeed, otherTrainSpeed, myDistanceToCrossing, optimalTime);
			}
			/* Convert speed to m/s */
			@SuppressWarnings("unused")
			double speedOrder = adjustSpeed/(leftMotor.getMaxSpeed() *Math.PI/180.*(Constants.WHEEL_SIZE/2.))*100;
			broadcastMessageToOwnVehicules(new Message("speedPolynom", speedPolynom));
			broadcastMessageToOwnVehicules(new Message("speed", firstOtherCarBehavior.getSpeed()));
		}
		else {
			writer.println("No first car !");
		}
		writer.close();
		try {
		    file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Button.LEDPattern(0);
	}
	
}
