package app.server.models;

import org.jenkinsci.plugins.stw.Commands;
import org.jenkinsci.plugins.stw.Status;




public class Message {
	
	public Status status;
	public Commands command;
	public Object data;	
	public String description;

	public Message(Status status, Commands command, Object data) {
		this.status = status;
		this.command = command;
		this.data = data;
	}
	
	public Message() {}
}