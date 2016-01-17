package utils;

/**
 * Constant class
 * 
 * <p>Contains a series of constants used throughout the project.</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 *
 */
public abstract class Constants {
	
	public final static int SOCKET_NUMBER = 5000;
	
	public final static int PACKET_SIZE = 2000;
	
	public final static int DISTANCE_MARK_TO_CROSSING = 1500;
	
	public final static double TRAIN_NORMAL_SPEED = 40;
	public final static double TRAIN_CROSSING_SPEED = 40;
	public final static double TRAIN_NORMAL_DISTANCE = 0.30;
	public final static double TRAIN_CROSSING_DISTANCE = 0.30;
	
	/* Constants used for distance computation */
	public final static double WHEEL_SIZE = 0.056;
	public final static double CIRCUIT_SIZE = 5000;
	public final static double MARK_CROSSING = 2000;
	
	public final static double WHEEL_PERIMETER = 2*Math.PI*(WHEEL_SIZE/2.);
}