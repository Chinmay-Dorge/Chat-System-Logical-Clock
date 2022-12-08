package server;

import java.awt.BorderLayout;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.*;

public class ServerGUI extends JFrame {
	//arraylist to store the thereads
	ArrayList<ClientThread> al;

	private JTextArea chatWindow;
	private ServerSocket serverSocket;
	private Socket clientConnection;

	public ServerGUI(){
		super("Server");
		JLabel label = new JLabel();
		add(label,BorderLayout.NORTH);
		label.setText("Distributed Systems Project - Server");

		chatWindow = new JTextArea();
		add(new JScrollPane(chatWindow));
		chatWindow.setEditable(false);
		setSize(600, 400); 
		setVisible(true);

		setLocationRelativeTo(null);
		al = new ArrayList<ClientThread>();
	}
	
//main method which is being called by serveMain
	public void startRunning(){
		try{
			serverSocket = new ServerSocket(7689); 
			showMessage(" Waiting for someone to connect at 7689... \n");
			while(true){
				try{
					waitForConnection();
				}catch(EOFException eofException){
					showMessage("\n Server ended the connection! ");
				} 
			}
		} 
		catch (IOException ioException){
			ioException.printStackTrace();
		}
	}

	//wait for connection, then display connection information
	void waitForConnection() throws IOException{
		clientConnection = serverSocket.accept();
		ClientThread ct = new ClientThread(clientConnection);
		al.add(ct);

		ct.start();
	}

	//displaying msgs on the server gui textarea
	void showMessage(final String text){
		SwingUtilities.invokeLater(
				new Runnable(){
					public void run(){
						chatWindow.append("\n"+text);
					}
				}
				);
	}

//encoding the msgs (status) and sending it to the client
	String encodeHttp(String s) throws UnsupportedEncodingException {
		String status;
		if(s.equalsIgnoreCase("blank")){
			status="206 PartialContent";
			
		}
		else
			status ="200 OK";
		
	//date and time to send to client as recipet
		Date d = new Date();
		String msg1="";
		SimpleDateFormat sf = new SimpleDateFormat("yy/MM/dd 'at' hh:mm");
		msg1 = "HTTP/1.0 "+status+" \r\n" +
				"Server : localmachine \r \n"  +
				"Accept-Language: en-us\r\n" +
				"Date:"+sf.format(d)+"\r\n" +
				"Content-type: text/html\r\n";
					
		URLEncoder.encode(msg1,"UTF-8");
		return msg1;
	}

	String encoderHttp(String mes) throws UnsupportedEncodingException{
		int len = mes.length();
		Date d = new Date();
		String msg1 ="";
		
		SimpleDateFormat sf = new SimpleDateFormat("yy/MM/dd 'at' hh:mm");
		msg1 = "POST/ HTTP/1.0 \r\n" +
				"Server: localhost \r\n"  +
				"Accept-Language: en-us\r\n" +
				"Date:"+sf.format(d)+"\r\n" +
				"Content-type: text/html\r\n"+
				"Content-Length: "+ len +"\r\n"+
				"Body:"+mes+" (broadcast)";
		URLEncoder.encode(msg1,"UTF-8");
		return msg1;
	}

	//sending to all the clients on the network
	synchronized void toAll(String message) throws IOException{
		for( int j =al.size();--j>=0;){
			ClientThread k = al.get(j);
			String msg1 = encoderHttp(message);
			k.output.writeObject(msg1);
			k.output.flush();	
		}
	}

//getting number of user present in the network at the particular instance
	synchronized String getUsers() throws IOException{
		String list="Killo";
		for( int l =al.size();--l>=0;){
			ClientThread p = al.get(l);
			list = list + "-"+p.username ;
		}
		return list;
	}
	
	//to get the user list 
	synchronized String getUser() throws IOException{
		String list="uni";
		for( int l =al.size();--l>=0;){
			ClientThread p = al.get(l);
			list = list + "-"+p.username ;
		}
		return list;
	}

//send personal msg or one to one msg by taking the username as input to whom msg should  be sent
	boolean personal(String[] s) throws IOException{
		for( int j =al.size();--j>=0;){
			ClientThread k = al.get(j);
			if(k.username.equalsIgnoreCase(s[1])){
				if(s.length>=4){
					String msg1 = encoderHttp( "un-"+s[2]+"- "+s[3] +" (Personal Msg) \n");
					k.output.writeObject(msg1);
					k.output.flush();
				}
				else{
					showMessage("Blank message recieved , requested sender to send again");
					return false;
				}
			}
		}
		return true;
	}

// class where client thread are handled 
	public class ClientThread extends Thread{
		Socket clientConnection;
		ObjectOutputStream output;
		ObjectInputStream input;
		String username;

		int count = 0;
		public ClientThread(Socket clientConnection) {
			this.clientConnection = clientConnection;
		}

		public void run(){
			try {
				setupStreams();
				whileChatting();
			}
			catch (Exception e){
				e.printStackTrace();
			}
			finally{
				closeCon();
			}
		}

//fucntion to initalize the communction strema objects
		private void setupStreams() throws IOException{
			output = new ObjectOutputStream(clientConnection.getOutputStream());
			output.flush();
			input = new ObjectInputStream(clientConnection.getInputStream());
		}

//decoding the http msg recieved by the client and sending the body of the message as return
		private String decodeHttp(String msg) throws Exception  {
			URLDecoder.decode(msg,"UTF-8");
			String msg1="";
			String[] brak= msg.split("\n");
			String[] ms= brak[6].split(":");
			msg1=ms[1];
			return msg1;
		}


		//during the chat conversation
		private void whileChatting() throws Exception{
			String message = " You are now connected! \n";
			sendMessage(message);
			do{
				try{
					message = (String) input.readObject();
					String unDecoded= message;
					message= decodeHttp(message);

					String[] killer = message.split("-");
					if(message.equalsIgnoreCase(" ")){
						sendMessage(" * Server Recieved blank message*");
						sendMessage(encodeHttp("blank"));
					}

					else if(message.equals("one to one")){
						showMessage("\n"+unDecoded);
					}
					else {
						sendMessage(encodeHttp("normal"));
					}

					if(count==0){
						username = message;
						count++;
						showMessage("\n"+ username +" joined the network");
					toAll("**"+username +" joined the network***");
					}
					else{
						String finMsg=("\n"+ username +"- "+ message);
						String finMsg1=(username +"- "+ message);

						if(message.equalsIgnoreCase("END")){
							finMsg= (" ***"+ username +" left the network ***");
							toAll(finMsg);
						}
						else if(message.equalsIgnoreCase("one to one")) { 
							finMsg=getUsers();
							output.writeObject(finMsg);
							output.flush();
						}
						else if(message.equalsIgnoreCase("uni")){ 
							finMsg=getUser();
							output.writeObject(finMsg);
							output.flush();
						}
						else if(killer[0].equalsIgnoreCase("one2one")){
							if(personal(killer)){
								output.writeObject("Recievd sucessfully by "+killer[1]+"\n");
								output.flush();
								showMessage("\n"+unDecoded +"(Personal Msg) \n");
							}
							else {
								output.writeObject("Blank message sent , please send again");
								output.flush();
							}	
						}
						else if(killer[0].equalsIgnoreCase("unicast")){
							if(personal(killer)){
								output.writeObject("Recievd sucessfully by "+killer[1]+ "\n");
								output.flush();
								showMessage("\n"+unDecoded +"(Personal Msg) \n");
							}
								
						}
						else {
							showMessage("\n"+username+" - "+unDecoded);
							toAll(finMsg1);
							
						}
					}
				}
				catch(ClassNotFoundException classNotFoundException)
				{
					showMessage("The user has sent an unknown object!");
				}

			}while(!message.equalsIgnoreCase("END")); 
		}

		//connection closed then closing the inputs and output stream and socket
		public void closeCon(){
			showMessage("\n "+username +" connection closed... \n");
			try{
				for( int j =al.size();--j>=0;){
					ClientThread k = al.get(j);
					if(k.username==username){
						al.remove(j);
					}
				}
				output.close(); 
				input.close(); 
				clientConnection.close(); 
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}


//sending msgs to the client 
		void sendMessage(String message){
			try{
				output.writeObject("\n"+ message);
				output.flush();	
			}
			catch(IOException ioException)
			{
				chatWindow.append("\n ERROR: CANNOT SEND MESSAGE, PLEASE RETRY");
			}
		}
	}

}