package utils;

import java.io.Serializable;

/**
 * Message class
 * 
 * <p>This class contains value to send from a robot to another.</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 *
 */
public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5445331802632796714L;
	
	/* 
	 * The key allows the robot to know which type of data is contained in value.
	 * 
	 * Sometimes the key itself is the message.
	 */
	private String key;
	
	private Serializable value;
	
	public Message() {
		this.key = "";
		this.value = null;
	}
	
	public Message(String key, Serializable value) {
		super();
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Serializable getValue() {
		return value;
	}
	public void setValue(Serializable value) {
		this.value = value;
	}
	
	
}
