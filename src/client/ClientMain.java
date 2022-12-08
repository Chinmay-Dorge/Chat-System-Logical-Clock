package client;

import javax.swing.*;

public class ClientMain {

	public static void main(String[] args) throws Exception{
		//calling instance of the client interface
		ClientGUI client = new ClientGUI();
		client.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		client.startClient();
		
	}
}
