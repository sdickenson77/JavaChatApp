Example simple chat application built in java and manage by maven:
-  Demonstates Java's networking capabilities to send and receive messages between clients and a server. 
- to build jar locally use:  mvn clean package

includes Dockefile and docker-compose.yaml to be able to spin up a chat server locally
-  use docker-compose build to create a new image
-  use docker-compose up to start the chat server
-  once server is running use the following to launch the GUI: java -cp <also list path to jar if located in a different directory> JavaChatApp-1.0-SNAPSHOT.jar com.chatapp.ChatClientGUI
