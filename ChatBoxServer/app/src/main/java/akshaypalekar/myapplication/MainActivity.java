package akshaypalekar.myapplication;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

    //The server will start at the this port
    static final int ServerPort = 6000;

    //The socket which will be used to communicate with the client
    ServerSocket serverSocket;

    //Variable for the UI elements
    TextView messageWindow;
    EditText messageEdit;
    Button messageSend;

    String serverMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Binding UI elements to the variables
        messageWindow = (TextView)findViewById(R.id.tv_MessageWindow);
        messageEdit = (EditText)findViewById(R.id.et_MessageEdit);
        messageSend = (Button)findViewById(R.id.bt_messageSend);

        //ChatBoxServerThread chatboxServerThread = new ChatBoxServerThread();
        new ChatBoxServerThread().execute();
    }

    public void sendMessage(){
        if(messageEdit.getText().toString().equals("")){
            Toast.makeText(getApplicationContext(),"Kindly type you message",Toast.LENGTH_SHORT).show();
            return;
        }
        if(serverSocket==null){
            return;
        }
        serverMessage = messageEdit.getText().toString()+"\n";
    }


    private class ChatBoxServerThread extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Socket listener = null;
            try {
                serverSocket = new ServerSocket(ServerPort);
                Log.d(TAG,"Server socket created at port"+serverSocket.toString());
                while(true){
                    listener = serverSocket.accept();
                    Log.d(TAG,"Server listening for client connections");
                    ClientConnectThread clientConnectThread = new ClientConnectThread(listener);
                    clientConnectThread.start();
                    //new ClientConnectThread().execute(listener);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(listener!=null){
                    try {
                        listener.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            messageWindow.setText(R.string.client_wait);
        }
    }

    private class ClientConnectThread extends Thread {
        Socket socket;

        public ClientConnectThread(Socket listener) {
            this.socket = listener;
        }

        @Override
        public void run() {
            DataInputStream inputStream = null;
            DataOutputStream outputStream = null;
            try {

                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("Welcome");
                outputStream.flush();

                //This thread prints 'Connected to Client' in the Message view
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messageWindow.setText(R.string.client_connect);
                    }
                });

                while(true){
                    if(inputStream.available()>0){
                        final String newMessage = inputStream.readUTF();
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        messageWindow.setText(newMessage);
                                    }
                                });
                    }
                    if(!serverMessage.equals("")){
                        outputStream.writeUTF(serverMessage);
                        outputStream.flush();
                        serverMessage = "";
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(inputStream!=null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }if(outputStream!=null){
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
