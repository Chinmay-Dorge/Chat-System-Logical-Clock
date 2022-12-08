package server;

import javax.swing.*;

public class ServerMain {
	public static void main (String[] args){
		//intiating a server client gui instancece
		ServerGUI s = new ServerGUI();
		s.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		s.startRunning();  
	}
}
