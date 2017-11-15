package com.lakj.comspace.simpletextclient;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * This is a simple Android mobile client
 * This application read any string massage typed on the text field and
 * send it to the server when the Send button is pressed
 * Author by Lak J Comspace
 */
public class SlimpleTextClientActivity extends Activity {

    private static PublicKey pubk;
    private static PrivateKey prvk;
    private Socket client;
    private EditText textField;
    private Button button;
    private String messsage;
    private Cipher pkCipher;
    private Cipher aesCipher;
    private byte[] aesKey;
    private SecretKeySpec aeskeySpec;


    private void createKeyPair() {
        KeyPairGenerator kpg;
        try {
            //... write the needed code the creates an RSA pair keys of 1024 bits
            kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.genKeyPair();
            pubk = kp.getPublic();
            prvk = kp.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
        
        
            pkCipher = Cipher.getInstance("RSA");
            aesCipher = Cipher.getInstance("AES");
            
            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slimple_text_client);

        textField = (EditText) findViewById(R.id.editText1); // reference to the text field
        button = (Button) findViewById(R.id.button1); // reference to the send button

        // Button press event listener
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                messsage = textField.getText().toString(); // get the text message on the text field
                textField.setText(""); // Reset the text field to blank
                SendMessage sendMessageTask = new SendMessage();
                sendMessageTask.execute();
            }
        });
        Setup setupTask = new Setup();
        setupTask.execute();
    }

    private static PublicKey createPubK(byte[] b) {
        try {
            KeyFactory rsa = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(b);
            return rsa.generatePublic(x509EncodedKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] code(Cipher c, Key k, int mode, byte[] b) {
        try {
            c.init(mode,k);
            return c.doFinal(b);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private class Setup extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... params) {
            try {
                client = new Socket("10.0.0.54", 4444);
                ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(client.getInputStream());

                // Step 1 Create Key Pair;
                
                //... write 1 line code
                createKeyPair();

                // Step 2  Send public key pubk.getEncoded() writing writeObject() to the object oos.
                
                //... write 1 line code
                oos.writeObject(pubk.getEncoded());
                oos.flush();

                // Step 3
                byte[] buffer = new byte[162];
                ois.read(buffer);
                PublicKey serverPublicKey = createPubK(buffer);

                byte[] buffer2 = new byte[128];
                ois.read(buffer2);

                // Step 4
                byte[] temp = code(pkCipher, prvk, Cipher.PRIVATE_KEY, buffer2);
          
                aesKey = new byte[16];
                System.arraycopy(temp, 127-16, aesKey, 0, 16);
                aeskeySpec = new SecretKeySpec(aesKey, "AES");


                code(aesCipher, aeskeySpec, Cipher.ENCRYPT_MODE, serverPublicKey.getEncoded());

                client.close();
            } catch (Exception e) {
                Log.println(Log.ERROR, "err", e.toString());
            }
            return null;
        }
    }

    private class SendMessage extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                client = new Socket("127.0.0.1", 4444); // connect to the server
                ObjectOutputStream coos = new ObjectOutputStream(client.getOutputStream());
                coos.writeObject(new String(Base64.encode(code(aesCipher, aeskeySpec, Cipher.ENCRYPT_MODE, messsage.getBytes()), Base64.NO_WRAP)));
                coos.flush();
                coos.close();
                client.close(); // closing the connection
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.slimple_text_client, menu);
        return true;
    }

}
