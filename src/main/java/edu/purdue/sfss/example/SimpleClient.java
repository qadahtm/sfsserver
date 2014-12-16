/*
   Copyright 2014 - Thamir Qadah

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package edu.purdue.sfss.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class SimpleClient {

	public static void main(String[] args) {
		if (args.length == 2){
			Socket s = null;
			BufferedReader is = null;
			try {
				String hostname = args[0];
				int port = Integer.parseInt(args[1]);
				
				// Connecting to network socket
				s = new Socket(hostname, port);
				is = new BufferedReader( new InputStreamReader(s.getInputStream()));
				String line;
				while ((line = is.readLine()) != null){
					System.out.println(line);
				}						
			} catch (NumberFormatException nfe) {
				System.out.println("Error: Incorrect format for port, must be a number");
				nfe.printStackTrace();
				System.exit(-1);
			} catch (UnknownHostException e) {
				System.out.println("Unknown hostname");
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {				
				e.printStackTrace();
				System.exit(-1);
			}
			finally {
				try {
					s.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else{
			System.out.println("Error: Incorrect number of arguments. Usage: SimpleClient hostname port");
			System.exit(-1);
		}
	}

}
