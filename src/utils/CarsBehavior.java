package utils;

import java.io.Serializable;

/**
 * CarsBehavior class
 * 
 * <p>Stores all main attributes of a robot. This is used to transmit 
 * informations to the other train in case of conflict.</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 *
 */
public class CarsBehavior implements Serializable, Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6650875482123635456L;
	
	/* Main robot's attributes */
	private int id;
	private double speed;
	private double distance;
	private double position;
	private double spin;
	private int orangeNumber;
	
	/**
	 *  Constructor 
	 */
	public CarsBehavior(int id, double speed, double distance, double position,double spin, int orangeNumber) {
		this.id = id;
		this.speed = speed;
		this.distance = distance;
		this.position = position;
		this.spin = spin;
		this.orangeNumber = orangeNumber;
	}
	
	/* Setters and getters */
	
	public double getPosition() {
		return position;
	}


	public void setPosition(double position) {
		this.position = position;
	}


	public double getSpin() {
		return spin;
	}


	public void setSpin(double spin) {
		this.spin = spin;
	}


	public int getOrangeNumber() {
		return orangeNumber;
	}


	public void setOrangeNumber(int orangeNumber) {
		this.orangeNumber = orangeNumber;
	}


	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	

	@Override
	public Object clone() throws CloneNotSupportedException {
		CarsBehavior clone=(CarsBehavior)super.clone();
		clone.setId(this.id);
		clone.setDistance(this.distance);
		clone.setSpeed(this.speed);
		clone.setOrangeNumber(this.orangeNumber);
		clone.setPosition(this.position);
		clone.setSpin(this.spin);
		return clone;
	}

	
}