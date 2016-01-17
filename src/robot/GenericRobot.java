package robot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.Color;
import lejos.robotics.SampleProvider;
import utils.CarsBehavior;
import utils.Constants;
import utils.FollowingPolitic;
import utils.Message;
import utils.MessageDatagram;
import utils.SpeedPolynom;

/**
 * GenericRobot class
 * 
 * <p>All robots inherit from this class.
 * It contains the necessary attributes and function for 
 * sending basic messags, moving and using the sensors.</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 *
 */
public abstract class GenericRobot {

	private DatagramSocket listenSocket;
	private InetAddress broadcastAddr;
	/* All messages can be sent multiple times in order to avoid lost packages */
	private int redundancy;
	
	/* Mail box of received messages */
	protected Map<Integer, Long> timestampLastMessageFrom;
	protected ConcurrentLinkedQueue<Message> mailbox;

	/* Identity of the robot */
	private String behaviour;
	protected int trainNumber;
	/**
	 * Each car as a unique ID.
	 * carId = 10*trainNumber + role 
	 * role = 1 for Leader, 2 for Middle, 3 for Queue
	 */
	protected int carId;	
	protected int color;
	
	/* Speed attributes */
	protected SpeedPolynom speedPolynom;
	protected long roundSinceSetPolynom = 0;
	protected double speedToReach;	// Objective of speed to reach
	protected double newSpeed;
	protected long speedPolynomTachoCount = 0;
	/*
	 * The speed of the robot is proportional to the maximum speed. This
	 * attribute stores a percentage between 0 and 100.
	 */
	protected double speedPercentage;

	/* Distance detected by the sensor at the beginning of the current iteration */
	protected float dist;

	/*
	 * prevDistance stores the value found by the distance sensor at the last
	 * iteration
	 */
	protected float prevDistance;
	
	/*
	 * Variables used to log data into a text file
	 */
	protected FileWriter file;
	protected PrintWriter writer;

	/*
	 * Position related variables
	 */

	protected float position = 0;	// Tacho count since last orange mark
	protected float spin; 	// Spins of the wheels
	protected boolean orange = false;
	protected int previousTachoL = 0, previousTachoR = 0;
	protected int currentCount;
	protected int numberOfOrange = 0;
	protected long lastOrangeTimestamp = 0;
	protected boolean inCrossing = false;

	/* When was the last coordinate sent to the train */
	protected long lastCoordinatesSent = 0;


	/* Instantiate the 2 motors */
	protected EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(
			MotorPort.B);
	protected EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(
			MotorPort.C);

	
	protected float colorValue[];

	private EV3ColorSensor c = new EV3ColorSensor(SensorPort.S2);
	private SampleProvider colorSampleProvider = c.getRGBMode();
	private EV3UltrasonicSensor ultraSonor = new EV3UltrasonicSensor(
			SensorPort.S3);
	private SampleProvider distanceSampleProvider = ultraSonor
			.getDistanceMode();
	protected FollowingPolitic currentPolitic = FollowingPolitic.TO_A_POINT;

	/**
	 * Generic constructor
	 */
	public GenericRobot() {
		Sound.setVolume(20);	// Set Sound volumne
		setSpeedPercentage(Constants.TRAIN_NORMAL_SPEED);
	}

	/**
	 * Filter incoming message to only get messages assigned to the robot
	 * @param m : Incoming message packet
	 * @return boolean : truc if the message was added to the mailbox
	 */
	protected boolean filterMessage(MessageDatagram m) {
		/* If the message is for me or for all cars within my train */
		if (m.getTo() == carId
				|| (m.getTo() % 10 == 0 && m.getTo() == carId / 10 * 10)
		) {
			mailbox.add(m.getContent());
			return true;
		}
		return false;
	}

	/**
	 * Live function
	 * 
	 * When the robot starts its lifecycle
	 * @throws IOException
	 */
	public void live() throws IOException {
		init();
		LCD.clear();
		LCD.drawString(behaviour, 1, 1);
		LCD.drawString("ID :  " + Integer.toString(carId), 1, 2);

		/* Open a datagram socket */
		try {
			/* Broadcast all messages */
			broadcastAddr = InetAddress.getByName("255.255.255.255");
			/* Create Socket */
			listenSocket = new DatagramSocket(Constants.SOCKET_NUMBER);
			listenSocket.setSoTimeout(100);
			listenSocket.setBroadcast(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		redundancy = 2;
		
		timestampLastMessageFrom = new HashMap<>();
		
		/* Initialize mailbox */
		mailbox = new ConcurrentLinkedQueue<>();

		/* Setup listening thread */
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				MessageDatagram m = null;
				while (true) {
					try {
						m = receiveMessage();
						if (m != null) {
							filterMessage(m);
						}
					} catch (ClassNotFoundException e) {
						// e.printStackTrace();
					}
				}
			}

		});
		t.start();
		
		move();
	}

	/**
	 * Hello function
	 * 
	 * Ask user for the train number of the robot
	 */
	public void hello() {
		// Create menu
		LCD.clear();
		LCD.drawString("Train number :", 1, 1);
		LCD.drawString("Top-1", 1, 2);
		LCD.drawString("Enter-2", 1, 3);

		int pressed = Button.waitForAnyPress();
		switch (pressed) {
		case Button.ID_UP:
			this.trainNumber = 1;
			break;
		case Button.ID_ENTER:
			this.trainNumber = 2;
			break;
		default:
			break;
		}
		
		/* Define carId from its role and train number */
		carId = this.trainNumber * 10;
		if (this instanceof LeaderRobot) {
			carId += 1;
		} else if (this instanceof QueueRobot) {
			carId += 3;
		} else {
			carId += 2;
		}
	}
	
	/**
	 * Init function
	 * 
	 * Wait for the user to press enter to start the robot
	 */
	private void init() {
		LCD.drawString("-----", 1, 2);
		LCD.drawString("Enter to init", 1, 3);
		int pressed = Button.waitForAnyPress();
		if (pressed == Button.ID_ENTER) {
			hello();
		}
	}

	/**
	 * Move function
	 * 
	 * Function containing general behavior
	 */
	public abstract void move();

	/**
	 * Forward function
	 * 
	 * Starts both motors and execute the speed attributes
	 */
	protected void forward() {
		leftMotor.forward();
		rightMotor.forward();
	}

	/**
	 * Set the speed to both motors
	 * @param speedPercentage : percentage of the max speed of the motors (between 0 and 100)
	 */
	protected void setSpeedPercentage(double speedPercentage) {
		float speed = (float) (leftMotor.getMaxSpeed() * speedPercentage / 100.);
		this.speedPercentage = speedPercentage;
		leftMotor.setSpeed(speed);
		rightMotor.setSpeed(speed);
	}

	/**
	 * Get color sensor values
	 * @return RGB value from the sensor
	 */
	protected float[] getColor() {
		float[] value = new float[3];
		colorSampleProvider.fetchSample(value, 0);
		return value;
	}

	/**
	 * Get distance from ultrasonis sensor
	 * @return distance to the robot (or anything) in front of me
	 */
	public float distance() {
		/* Fetch sample for ultrasonic gives an array of one value */
		float[] value = new float[1];
		distanceSampleProvider.fetchSample(value, 0);
		
		/* We only return a float value, not the array */
		return value[0];
	}

	/**
	 * Stop both motors
	 */
	public void stop() {
		leftMotor.stop(true);
		rightMotor.stop();
	}

	/**
	 * Follow line using color sensor and adjusting each motor's speed
	 */
	public void followLine() {
		int politic = 0;
		colorValue = getColor();
		/* 
		 * Detect color from sensor value
		 * Set politic for blue, white or black behavior
		 * Set orange flag 
		 */
		if (colorValue[0] > 0.1 && colorValue[1] > 0.1 && colorValue[2] > 0.1) {
			politic = 0;
			orange = false;
			color = Color.WHITE;
		} else if (colorValue[0] < 0.1 && colorValue[1] < 0.1
				&& colorValue[2] < 0.1) {
			politic = -1;
			orange = false;
			color = Color.BLACK;
		} else if (colorValue[0] >= 0.2 && colorValue[1] <= 0.10
				&& colorValue[1] > 0.05 && colorValue[2] <= 0.05) {
			politic = -1;
			orange = true;
			color = Color.YELLOW;
		} else if (colorValue[0] >= 0.14 && colorValue[1] <= 0.10
				&& colorValue[1] > 0.05 && colorValue[2] <= 0.05) {
			politic = 1;
			orange = true;
			color = Color.ORANGE;
		} else if (colorValue[0] <= 0.1 && colorValue[1] >= 0.1
				&& colorValue[2] < 0.1) {
			politic = 1;
			orange = false;
			color = Color.BLUE;
		} else {
			politic = 0;
			color = Color.NONE;
			orange = false;
		}
		/* Adjust each motor's speed using politic defined above */
		switch (politic) {
		case -1:
			rightMotor.setSpeed((float) (rightMotor.getMaxSpeed()
					* speedPercentage / 100));
			leftMotor.setSpeed((float) (leftMotor.getMaxSpeed()
					* speedPercentage / 2 / 100));
			break;
		case 0:
			rightMotor.setSpeed((float) (rightMotor.getMaxSpeed()
					* speedPercentage / 2 / 100));
			leftMotor.setSpeed((float) (leftMotor.getMaxSpeed()
					* speedPercentage / 100));
			break;
		case 1:
			rightMotor.setSpeed((float) (rightMotor.getMaxSpeed()
					* speedPercentage / 100));
			leftMotor.setSpeed((float) (leftMotor.getMaxSpeed()
					* speedPercentage / 100));
			break;
		default:
			break;
		}

		/* Get position on the map */
		int tachoR = rightMotor.getTachoCount();
		currentCount = tachoR - previousTachoR + previousTachoL;
		position += currentCount;
		previousTachoR = tachoR;
		spin = position / 360;
		
		long currentTime = System.currentTimeMillis();
		/*
		 * Reach to orange mark and avoid having multiple calls for one mark 
		 */
		if (orange && currentTime - lastOrangeTimestamp > 2000) {
			rightMotor.resetTachoCount();
			lastOrangeTimestamp = currentTime;
			numberOfOrange++;
			orangeMark();
		}

		/* Send the robot's main attributes to the train every 500ms */
		if (currentTime - lastCoordinatesSent > 500) {
			CarsBehavior behavior = new CarsBehavior(carId, speedPercentage,
					dist, position, spin, numberOfOrange);
			sendToItsTrain(new Message("coordinates", behavior));
			lastCoordinatesSent = currentTime;
		}

	}

	/**
	 * Function called when an orange mark is detected
	 * 
	 * Empty by default
	 */
	public void orangeMark() { }

	/**
	 * Send a message via UDP to a robot or a group of robot
	 * @param to : destination of the message
	 * @param message to send
	 */
	protected void sendMessage(int to, Message message) {
		MessageDatagram mD = new MessageDatagram(this.carId, to, message);

		/* Loop for redundancy */
		for (int i = 0; i < redundancy; i++) {

			/* Create buffer */
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(
					Constants.PACKET_SIZE);
			ObjectOutputStream os = null;
			try {
				os = new ObjectOutputStream(
						new BufferedOutputStream(byteStream));
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				os.flush();
				/* Fill buffer with serializable object */
				os.writeObject(mD);
				os.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			/* Convert buffer to an array of bytes */
			byte[] sendBuf = byteStream.toByteArray();

			/* Create UDP packet containing the byte array */
			DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length,
					broadcastAddr, Constants.SOCKET_NUMBER);

			try {
				/* Send the packet */
				listenSocket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Receive a message on the listenSocket
	 * @return a MessageDatagram
	 * @throws ClassNotFoundException
	 */
	public MessageDatagram receiveMessage() throws ClassNotFoundException {
		/* Create buffer for receiving content of the packet */
		byte[] buf = new byte[Constants.PACKET_SIZE];

		/* Create packet */
		DatagramPacket packet = new DatagramPacket(buf, Constants.PACKET_SIZE);

		MessageDatagram m = null;
		try {
			/* Receive packet */
			this.listenSocket.receive(packet);

			/* If the packet is not empty */
			if (packet.getLength() > 0) {
				/* Convert byte array to byte buffer */
				ByteArrayInputStream byteStream = new ByteArrayInputStream(buf);
				ObjectInputStream is = null;
				try {
					is = new ObjectInputStream(new BufferedInputStream(
							byteStream));
				} catch (IOException e) {
					e.printStackTrace();
				}

				Object o = null;
				try {
					/* Read object from packet content */
					o = is.readObject();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (ClassNotFoundException e2) {
					e2.printStackTrace();
				}

				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (o != null) {
					m = (MessageDatagram) o;

					/* 
					 * Retrieve timestamp from the sending robot and compare it
					 * with the content of the timestampLastMessageFrom map.
					 * 
					 * This allow us to check for redundancy and avoid processing
					 * the same message twice.
					 */
					long time = m.getTimestamp();
					Long mapTimestamp = this.timestampLastMessageFrom.get(m
							.getFrom());

					if (mapTimestamp == null
							|| (mapTimestamp != null && time > mapTimestamp)) {
						timestampLastMessageFrom.put(m.getFrom(), time);
					} else {
						m = null;
					}
				}
			}
		} catch (IOException e) {
			
		}
		return m;
	}

	/**
	 * Send a message to the robot's train
	 * @param message to send
	 */
	protected void sendToItsTrain(Message message) {
		sendMessage(carId / 10 * 10 + 1, message);
	}

	/**
	 * Get all new messages from the mailbox
	 */
	@SuppressWarnings("unchecked")
	public void getNewMessages() {
		Message m = null;

		/* Loop as long as the mailbox contains something */
		while (!mailbox.isEmpty()) {
			m = mailbox.poll();

			Object o = m.getValue();
			
			/* 
			 * Check key of the message
			 * 
			 * Knowing the key, we can cast the content to a certain type
			 */
			if (m.getKey().equals("trainInCrossing")) {
				/* 
				 * The train receives a message from its leader indicating
				 * that it entered the crossing. 
				 */
				
				LeaderRobot IamALeader = (LeaderRobot) this;
				this.inCrossing = true;
				IamALeader.broadcastMessageToOwnVehicules(new Message(
						"inCrossing", null));
				IamALeader
						.sendToOtherTrain(new Message("warningCrossing", null));
				IamALeader.broadcastMessageToOwnVehicules(new Message("speed",
						Constants.TRAIN_CROSSING_SPEED));
				IamALeader.broadcastMessageToOwnVehicules(new Message(
						"safeDistance", Constants.TRAIN_CROSSING_DISTANCE));

			} else if (m.getKey().equals("warningCrossing")) {
				/*
				 * The train receives a warning from the other train
				 * indicating that the other train entered the crossing
				 */
				LeaderRobot IamALeader = (LeaderRobot) this;
				IamALeader.otherTrainInCrossing = true;
				if (inCrossing) {
					IamALeader.sendToOtherTrain(new Message("carsMap",
							IamALeader.carsMap));
					IamALeader.sendToItsTrain(new Message("speed",Constants.TRAIN_NORMAL_SPEED/2));
				}
				Button.LEDPattern(2);
			} else if (m.getKey().equals("inCrossing")) {
				/* 
				 * The robot receives a message from its train indicating
				 * that it entered the crossing. 
				 */
				this.inCrossing = true;
				Button.LEDPattern(1);
				Sound.playTone(1500, 100);
				Sound.playTone(750, 100);
			} else if (m.getKey().equals("trainOutOfCrossing")) {
				/*
				 * The train receives a message from its queue indicating that
				 * it left the crossing
				 */
				LeaderRobot IamALeader = (LeaderRobot) this;
				IamALeader.broadcastMessageToOwnVehicules(new Message(
						"outCrossing", null));
				IamALeader.broadcastMessageToOwnVehicules(new Message("speed",
						Constants.TRAIN_NORMAL_SPEED));
				IamALeader.broadcastMessageToOwnVehicules(new Message(
						"safeDistance", Constants.TRAIN_NORMAL_DISTANCE));
				if (IamALeader.inCrossing && IamALeader.otherTrainInCrossing) {
					IamALeader.sendToOtherTrain(new Message(
							"warningExitCrossing", null));
				}
				Button.LEDPattern(0);
				Sound.playTone(1500, 100);
				Sound.playTone(750, 100);
			} else if (m.getKey().equals("warningExitCrossing")) {
				/*
				 * The train receives a message from the other train indicating
				 * that the he is now alone in the crossing
				 */
				LeaderRobot IamALeader = (LeaderRobot) this;
				IamALeader.otherTrainInCrossing = false;
				IamALeader.sendToItsTrain(new Message("speed",Constants.TRAIN_NORMAL_SPEED));
			} else if (m.getKey().equals("outCrossing")) {
				/*
				 * The robot receives a message from its train indicating
				 * that it left the crossing.
				 */
				this.inCrossing = false;
			} else if (m.getKey().equals("coordinates")) {
				/*
				 * The train receives the coordinate and speed of one of its
				 * vehicule.
				 */
				LeaderRobot IamALeader = (LeaderRobot) this;
				CarsBehavior behavior = (CarsBehavior) o;
				IamALeader.carsMap.put(behavior.getId(), behavior);
			} else if (m.getKey().equals("carsMap")) {
				/*
				 * The train receives the list of coordinates from all cars of the other train.
				 * We use that information to prepare to pass the crossing and avoiding a conflict.
				 */
				LeaderRobot IamALeader = (LeaderRobot) this;

				IamALeader.prepareCrossing((TreeMap<Integer, CarsBehavior>) o);
			} else if (m.getKey().equals("speed")) {
				/*
				 * The robot receives a speed order from its train
				 */
				double speed = (double) o;
				speedToReach = speed;
			} else if (m.getKey().equals("safeDistance")) {
				/*
				 * The robot receives a safeDistance order from its train
				 */
				if(!(this instanceof LeaderRobot)){
					double safeD = (double) o;
					MiddleRobot IamAMiddleRobot = (MiddleRobot)this;
					IamAMiddleRobot.D = safeD;
				}
			}
			else if (m.getKey().equals("speedPolynom")) {
				/* 
				 * A speed polynom has been calculated by the train and sent to
				 * its components
				 */
				this.speedPolynom = (SpeedPolynom) o;
				this.speedPolynomTachoCount  = leftMotor.getTachoCount();
			} else if (m.getKey().equals("debug")) {
				/*
				 * Debug message used to stop the robot and check transmission
				 */
				LCD.clear();
				LCD.drawString("DEBUG", 1, 2);
				stop();
				while (true) {
				}
			}

		}
	}

	/**
	 * Get the robot's behavior
	 * @return the robot's behavior as a String
	 */
	public String getBehaviour() {
		return behaviour;
	}

	/**
	 * Set the behavior of the robot
	 * @param behaviour string
	 */
	public void setBehaviour(String behaviour) {
		this.behaviour = behaviour;
	}

}
