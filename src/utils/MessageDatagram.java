package utils;

import java.io.Serializable;

/**
 * MessageDatagram class
 * 
 * <p>Contains the message to send and medata about the sender's id, receiver's id
 * and sender's timestamp (used for redundancy)</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 *
 */
public class MessageDatagram implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5459906412344962193L;
	private int to;
	private int from;
	private long timestamp;
	private Message content;
	
	public MessageDatagram(int from, int to, Message content) {
		this.from = from;
		this.timestamp = System.currentTimeMillis();
		this.to = to;
		this.content = content;
	}

	public int getTo() {
		return to;
	}

	public void setTo(int to) {
		this.to = to;
	}

	public Message getContent() {
		return content;
	}

	public void setContent(Message content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "Message [to=" + to + ", content=" + content + "]";
	}

	public long getTimestamp() {
		return timestamp;
	}
	

	public int getFrom() {
		return from;
	}

}
