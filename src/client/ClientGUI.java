package client;

import javax.swing.*;

import server.ServerGUI.ClientThread;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ClientGUI  extends JFrame {
	private JFrame frame;
	private JTextArea chatWindow;
	private JTextField logicalClock;
	private JTextField userText;
	private JButton broadCast;
	private JButton personal;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private String message ="";
	private String personaly;

	static Random rand = new Random();
	private static String usrname="Client_"+ rand.nextInt(300);
	private String msg1="";
	private static int count =0;
	private static final String serverIP ="localhost";
	private Socket clientConnection;

	ClientTime ct = new ClientTime();
	public ClientGUI(){
		super(usrname);
		ct.start();
		
		JLabel label = new JLabel();
		add(label,BorderLayout.NORTH);
		label.setText("Distributed Systems Project - Client");

		//field for showing logical clock show
		logicalClock=new JTextField();
		logicalClock.setEditable(false);
		add(logicalClock, BorderLayout.PAGE_START);

		//text field for input
		userText=new JTextField();
		userText.setEditable(false);
		add(userText, BorderLayout.SOUTH);

		//Text area where incoming and outgoing msgs are displayed
		chatWindow = new JTextArea();
		chatWindow.setEditable(false);
		add(new JScrollPane(chatWindow), BorderLayout.CENTER);

		//button for sending one to one connection
		personal = new JButton("One to One");
		add(personal,BorderLayout.WEST);

		//button for broadcasting message 
		broadCast = new JButton("BroadCast");
		add(broadCast,BorderLayout.EAST);

		setSize(500,400);
		setLocation(0,0);
		setVisible(true);

		userText.addActionListener
		(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				if(count==0){
					count++;
					userText.setText("");
					showMessage("*Your input : "+ e.getActionCommand() +"*");
					sendMessage(e.getActionCommand());
				}
				else {
					if(!e.getActionCommand().isEmpty()){
						sendMessage(e.getActionCommand());
						showMessage("*Your input : "+ e.getActionCommand()+"*");
						userText.setText("");
					}
					else {
						showMessage("*Your input : "+ e.getActionCommand()+"*");
						sendMessage(" ");
						userText.setText("");
					}
				}

			}
		}
				);

		personal.addActionListener
		(new ActionListener(){	
			public void actionPerformed(ActionEvent e) {
				personaly = userText.getText();
				sendMessage("one to one");

			}
		}
				);

		broadCast.addActionListener
		(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!userText.getText().isEmpty()){
					sendMessage(userText.getText());
					userText.setText("");
				}
				else{
					sendMessage(" ");
					userText.setText("");
				}
			}
		}
				);


	}

	private String decodeHttp(String msg) throws Exception  {
		URLDecoder.decode(msg,"UTF-8");
		String msg1="";
		String[] brak= msg.split("/");
		String[] ms= brak[1].split(" ");
		msg1="Status "+ ms[1]+ " " +ms[2];
		return msg1;
	}

	// http encoding of the message into http
	String encodeHttp(String msg) throws UnsupportedEncodingException{
		int len = msg.length();
		Date d = new Date();
		String type ="";
		if(msg.equalsIgnoreCase("one to one")){
			type = "GET";
		}
		else{
			type ="POST";

		}
	
		SimpleDateFormat sf = new SimpleDateFormat("MM/dd/yy'at' hh:mm");
		msg1 = type+" / HTTP/1.0 \r\n" +
				"Host: localhost:7689 \r\n"  +
				"Accept-Language: en-us\r\n" +
				"Date:"+sf.format(d)+"\r\n" +
				"Content-type: text/html\r\n"+
				"Content-Length: "+ len +"\r\n"+
				"Body:"+msg;

		URLEncoder.encode(msg1,"UTF-8");
		return msg1;
	}
	
	//for sending random messages with logical clock value
	public void choiceRandom(String[] list) throws IOException 
	{ 
		Random ren = new Random();
		//getting logical clock value
		int cn = ct.getCount();
		ArrayList<String> kem = new ArrayList<String>();
		for(int i =1;i<list.length;i++){
			if(!list[i].equalsIgnoreCase(usrname)){
				kem.add(list[i]);
			}
		}
		int g = kem.size();
		if(g>0){
			String lmsg = encodeHttp("unicast-"+kem.get(ren.nextInt(g))+"-"+usrname +"- Logical Time "+Integer.toString(cn));
			direct(lmsg);
		}
	}

	// for letting user choose other users in network while sending one to one.
	public void  ChoiceExample(String[] list){  
		Frame f = new Frame();  
		Choice c= new Choice(); 
		
		broadCast.setEnabled(false);
		personal.setEnabled(false);
		userText.setEnabled(false);
		
		c.setBounds(50,50, 100,100);
		c.add("Select from network Users list below"); 
		
		for(int m=1;m<list.length;m++){
			if(!list[m].equalsIgnoreCase(usrname)){
				c.add(list[m]);
			}

		}

		f.add(c);  

		f.setSize(200,100);  
		f.setLayout(null);  
		f.setVisible(true);
		
		//Function which will note the selection for the user in one to one scenario and send to server after encoding it
		c.addItemListener(  (ItemListener) new ItemListener(){  
			public void itemStateChanged(ItemEvent e) {
				String option  =(String) e.getItem();
				String lmsg;
				try{
					lmsg = encodeHttp("one2one-"+option+"-"+usrname +"-"+personaly);
					direct(lmsg);
				}
				catch (IOException e1){
					e1.printStackTrace();
				} 
				f.dispose();

				broadCast.setEnabled(true);
				personal.setEnabled(true);
				userText.setEnabled(true);
				userText.setText("");	
			}  
		});

	}

	//Initiating the client , this fuction is being called by clientMain class
	public void startClient() throws Exception{
		try{
			connectToServer();
			setUpStreams();
			whileChatting();
		}
		catch (EOFException e){
			showMessage("\n Client Terminated");
		}
		catch(IOException ei){
			ei.printStackTrace();
		}
		finally {
			close();
		}
	}
	
	// LAMPORT LOGICAL CLOCK SYNCRONIZATION
	//function to handle the messages sent by server with logical clock and does the needed changes to the logical clock
	private String Process(String[] m){
		String l= m[1]+m[2];
		showMessage(l);
		String[] g = m[2].split(" ");
		int k = Integer.parseInt(g[4]);
		int u = ct.getCount();
		if(k>u){
			ct.setCount(k++);
			String me = "* Adjustment was needed in the logical clock! \n Time changed from "+u +" to " + k +" *";
			showMessage(me);
		}
		else {
			showMessage("* Adjustment was not nedded in the logical clock! *");
		}
		
		return l;
	}

	private String decoderHttp(String msg) throws Exception  {
		URLDecoder.decode(msg,"UTF-8");
		String msg1 = null;
		String[] brak= msg.split("\n");
		String[] ms= brak[6].split(":");
		try{
			String[] mg = ms[1].split("-");
			if(mg[0].equalsIgnoreCase("un")){
				Process(mg);
				msg1="";
			}
			else {
				msg1=ms[1];
			}
		}
		catch(Exception e)
		{
			return ("");
		}
		return msg1;
	}

	//Function which creates connection for client to server
	private void connectToServer() throws IOException{
		showMessage(" Trying to connect at port 7689 \n");
		clientConnection = new Socket("localhost",7689);
	}

	private void setUpStreams() throws IOException{
		output = new ObjectOutputStream(clientConnection.getOutputStream());
		
		output.flush();
		sendMessage(usrname);
		showMessage(usrname+" connected to Server \n");

		input = new ObjectInputStream(clientConnection.getInputStream());
	}

	//Function handling the communication between client and server
	private void whileChatting() throws  Exception{
		String msg="";
		ableToType(true);
		do {
			try{
				message=(String) input.readObject();
				String[] http = message.split("/");
				String[] verify = message.split("-");
				
				if(verify[0].equalsIgnoreCase("killo")){
					ChoiceExample(verify);
				}
				else if(verify[0].equalsIgnoreCase("uni")){
					choiceRandom(verify);
				}
				else if(http[0].contains("HTTP")) {
					msg = decodeHttp(message);
					
				}
				else if(http[0].contains("POST")){	
					msg = decoderHttp(message);
					showMessage("\n"+msg);
				}
				else {
					showMessage(" "+message);
				}
			}
			catch(ClassNotFoundException e)
			{
				showMessage("\n please send the message again");
			}
		}while(!message.equalsIgnoreCase("END"));
	}

	//function to properly close all the client related objects like input,output stream and socket of the client
	@SuppressWarnings("deprecation")
	private void close() 
	{
		showMessage("\n Connection Closed");
		sendMessage("***"+usrname+" Connection closed ***");

		ableToType(false);
		try {
			input.close();
			output.close();
			clientConnection.close();
			ct.stop();
		}
		catch(Exception e1){
			e1.printStackTrace();
		}
	}

	private void sendMessage(String message1){
		int county = ct.getCount();
		String time = Integer.toString(county);
		try {
			String ms = message1;
			ms = encodeHttp(message1);
			output.writeObject(ms);
			output.flush();

		}
		catch (IOException ex){
			chatWindow.append("\n error in messages");
		}
	}
	//displaying the message on the GUI(chatWindow-text area) the messages are decoded
	private void showMessage(final String s){
		SwingUtilities.invokeLater(
				new Runnable() {
					@Override
					public void run() {
						chatWindow.append("\n "+s);
					}
				}
				);
	}

	//send the msg to directly to server
	private void direct(String g) throws IOException{
		output.writeObject(g);
		output.flush();
	}
	
	//Function which handles text field inability so that its restricted when the communication is lost
	private void ableToType(final boolean b)
	{
		SwingUtilities.invokeLater(
				new Runnable() {
					@Override
					public void run() {
						userText.setEditable(b);
					}
				}
				);
	}
	
	// used to send msg to clients randomly 
	public void sendMessageUnicast(int c) throws IOException
	{
		String ms = "";
		ms = encodeHttp("uni");
		direct(ms);
	}
	
	// BUILDING THE LOGICAL CLOCK FOR EACH CLIENT
	public class ClientTime extends Thread {
		//random function to initiate the clock with random number
		Random rand1 = new Random();
		//taking the random number into integer variable
		int count = rand1.nextInt(50);

		public void run() {

			//infinite loop to keep on increasing the time
			for(;;) 
			{ 
				try { 
					//making the clock increase by 1 second
					Thread.sleep(1000); 
					count ++; 
					//displaying the clock
					logicalClock.setText("Logical Time : "+ Integer.toString(count) + " ");
					//sending the message after every 8 seconds
					if(count%8==0){
						//calling the function to send unicast message
						sendMessageUnicast(count);
					}
				} catch (InterruptedException | IOException e) { 
					e.printStackTrace(); 
				} 
			}
		}

		//function to get the clock value
		public int getCount(){
			return count;
		}

		//function to set the clock value
		public void setCount(int count){
			this.count = count;
		}

	}
}
