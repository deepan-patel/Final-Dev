package sample;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.security.auth.login.LoginContext;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class client1GUI extends Application {
    public  TextField tf;
    public  TextArea ta;
    public Stage mainStage;
    Button sendButton;

    public String username;
    public  Socket clientSocket; //Client Socket
    DataOutputStream writer; //writes to server from Client Socket
    DataInputStream listener; //reads to server from Client Socket

    //Helps stop various threads to avoid Socket connection errors
    boolean listenerAlive;
    boolean writerAlive;

    LogIn login;

    //new var
    String messageDatabase = "src/messageDatabase.txt";
    Scanner scan;
    public Menu onlineUserMenu;

    //constructor of Client GUI
    public client1GUI(String username, LogIn logIn) throws IOException {
        this.username=username;
        clientSocket = new Socket("localhost", 4001); //connect Client socket to host server
        this.login=logIn;
        writer = new DataOutputStream(clientSocket.getOutputStream());
        writerAlive=true;
        listener = new DataInputStream(clientSocket.getInputStream());
        listenerAlive=true;
    }

    public void start(Stage primaryStage) throws IOException, InterruptedException {
       /* //setting up GUI
        mainStage = new Stage();
        pane = new VBox();
        ta = new TextArea();
        tf = new TextField();

        pane.getChildren().addAll(ta,tf);
        Scene scene = new Scene(pane);
        mainStage.setScene(scene);
        mainStage.show();


        sendUsername();

        ListenFromServer listening=new ListenFromServer();
        WriteToServer writing=new WriteToServer(); //Thread that takes messages inputted from GUI and sends it to Server
        InvalidExitCheck invalidExit=new InvalidExitCheck(); //Thread that makes Client go offline if GUI window is closed
        //All threads ran concurrently
        listening.start();
        writing.start();
        invalidExit.start();*/
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        int offset = 50;

        mainStage = new Stage();
        //Needed by the user to receive messages
        ta = new TextArea();

        //MENU FOR ONLINE AND QUIT//
        BorderPane borderLayout = new BorderPane();
        Menu userActivity = new Menu("Online");
        MenuItem quit = new MenuItem("Quit");
        userActivity.getItems().addAll(quit);
        MenuBar userDropDown = new MenuBar();

        //This menu notifies the user of who's online
        onlineUserMenu = new Menu("View Online Users");

        userDropDown.getMenus().addAll(userActivity,onlineUserMenu);
        borderLayout.setTop(userDropDown);
        borderLayout.setCenter(ta);
        borderLayout.getChildren().addAll();

        //TEXTFIELD FOR USER TO TYPE//
        GridPane messageLayout = new GridPane();
        messageLayout.setHgap(10);
        messageLayout.setVgap(10);

        tf = new TextField();
        tf.setPromptText("Enter Message Here. (Type quit to exist the program)");
        tf.setPrefWidth(primScreenBounds.getWidth()/2 - offset*2);
        tf.setMinHeight(offset/5);
        GridPane.setConstraints(tf, 1, 1);

        //create space at bottom of screen
        Rectangle blankSpace = new Rectangle();
        blankSpace.setWidth(10);
        blankSpace.setHeight(10);
        blankSpace.setFill(Color.TRANSPARENT);
        blankSpace.setStroke(Color.TRANSPARENT);
        GridPane.setConstraints(blankSpace, 0, 0); //ontop of textField
        GridPane.setConstraints(blankSpace, 0, 2); //below textField

        //SEND BUTTON//
        sendButton = new Button("Send");
        GridPane.setConstraints(sendButton, 2, 1);


        messageLayout.getChildren().addAll(sendButton, tf, blankSpace);
        borderLayout.setBottom(messageLayout);

        //BUTTON ACTION - QUIT//
        quit.setOnAction(e->{ mainStage.close();});


        //                                  display  to screen                                          //
        //sets size to the size of the user's screen
        Scene scene = new Scene(borderLayout, primScreenBounds.getWidth()/2, primScreenBounds.getHeight()/2);
        mainStage.setScene(scene);
        mainStage.setResizable(false);
        mainStage.setTitle(username);
        mainStage.show();

        //centers screen
        mainStage.setX((primScreenBounds.getWidth() - primaryStage.getWidth()) / 2);
        mainStage.setY((primScreenBounds.getHeight() - primaryStage.getHeight()) / 4);

        //Styling the GUI
        userDropDown.setStyle("-fx-background-color:INDIANRED");
        sendButton.setStyle("-fx-background-color:INDIANRED");
        tf.setStyle("-fx-prompt-text-fill:INDIANRED");
        messageLayout.setStyle("-fx-background-color:BLANCHEDALMOND");

        sendUsername();

        //once the GUI is setup all old messages are loaded into the textArea
        openMessages();
        //made to ensure user does not enter anything into the data by accident
        ta.setEditable(false);

        ListenFromServer listening=new ListenFromServer();
        WriteToServer writing=new WriteToServer();
        InvalidExitCheck invalidExit=new InvalidExitCheck();
        updateOnline whosOnline = new updateOnline();

        //All threads ran concurrently
        listening.start();
        writing.start();
        invalidExit.start();
        whosOnline.start();

    }


    //load messages from the database
    public void openMessages() throws FileNotFoundException {
        scan = new Scanner(new File(messageDatabase));

        while(scan.hasNextLine()){
            String line =  scan.nextLine();
            if(line.equals("")){
                continue;
            }
            else{
                ta.appendText(line+"\n");
            }
        }
    }


    public class updateOnline extends Thread{//thread that update the online menu with online users
        public void run(){
            while(listenerAlive){
                try{
                    Platform.runLater(()->{
                        updateUsers();
                    });
                    //thread sleep for sometime then update the menu
                    Thread.sleep(1500);
                }catch(Exception e){
                    e.printStackTrace();

                }
            }
        }

    }


    public void updateUsers(){

        onlineUserMenu.getItems().clear();
        int numUser = login.currentlyLoggedIn.size();
        for(int i=0;i<numUser;i++){
            //add all the users online and skips the current user so their name is not added
            String newUsername = login.currentlyLoggedIn.get(i);
            if(newUsername.equals(username)){
                continue;
            }
            else{
                MenuItem item = new MenuItem(newUsername);
                onlineUserMenu.getItems().addAll(item);
            }
        }

    }



    public class WriteToServer extends Thread { //Thread that calls method that sends messages to Server

        public void run() {
            try {
                write();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally { //This thread is the last one to end so close any streams that are still alive
                try {
                    writer.close();
                    writerAlive=false;
                    listener.close();
                    listenerAlive=false;
                    clientSocket.close(); //close Socket connection from Client side
                    login.currentlyLoggedIn.remove(username); //remove user form currently Logged in list so they can log in again

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write() throws IOException, InterruptedException { //This method writes messages to Server
            while (listenerAlive) { //runs infinitely until the Client is still listening from Server(Client is online)
                tf.setOnKeyPressed(e->{
                    if(e.getCode().equals(KeyCode.ENTER) && tf.getText().length()>0){ //whenever Client writes message in textfield and presses enter...
                        try {
                            writer.writeUTF(tf.getText()); //retrieve the GUI input and write it to Server
                            tf.clear();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                sendButton.setOnAction(e->{
                    try {
                        writer.writeUTF(tf.getText());
                        tf.clear();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            writer.close(); //close output stream to Server
            writerAlive=false;
            Platform.runLater(()->{ //close Client GUI window
                mainStage.close();
            });
        }
    }

    public class ListenFromServer extends Thread { //Thread that calls method that listens for messages from Server
        public void run() {
            try {
                listen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void listen() throws IOException { //This method listens for messages from Server
            while(listenerAlive){
                String incommingData;
                while ((incommingData = listener.readUTF()) != null) { //Keeps waiting for input from server and reads it infinitely
                    if (incommingData.equals("quit")) { //Stops listening when Server sends quit message (meant to tell Client to stop processes)
                        break;
                    }
                    ta.appendText(incommingData + "\n"); //Other messages are all put onto Client GUI
                }
                listener.close(); //Close input stream from Server
                listenerAlive=false;
            }
        }

    }

    public class InvalidExitCheck extends Thread{ //Thread that keeps searching for whether the user has closed the Client GUI window
        public void run(){
            while(listenerAlive){ //repeat till user is still online...
                if (!mainStage.isShowing()){ //if the window is closed...
                    try {
                        writer.writeUTF("quit"); //send quit to Server to notify Client's desire to end connection
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void sendUsername() throws IOException { //This method sends the username of the current clientSocket to the server
        writer.writeUTF(username);
    }
}



