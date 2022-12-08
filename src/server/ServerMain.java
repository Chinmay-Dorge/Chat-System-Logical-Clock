/* Sameer Valiah Pusunuru
 * 1001552473
 * DS Lab1
 */

package server;
//nedded for server gui
import javax.swing.*;

public class ServerMain {
	public static void main (String[] args){
		//intiating a server client gui instancece
		ServerGUI s = new ServerGUI();
		//enabling the function to close the window and exit on the gui
		s.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//method in which server will be created and handled
		s.startRunning();  
	}
}
