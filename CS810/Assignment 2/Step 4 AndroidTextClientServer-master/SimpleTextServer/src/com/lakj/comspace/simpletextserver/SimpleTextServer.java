package com.lakj.comspace.simpletextserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * This is a simple server application. This server receive a string message
 * from the Android mobile phone and show it on the console.
 * Author by Lak J Comspace
 */
public class SimpleTextServer {

	private static ServerSocket serverSocket;
	private static Socket clientSocket;
	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;

	private static Cipher pkCipher;
	private static Cipher aesCipher;

	private static byte[] aesKey;
	private static SecretKeySpec aeskeySpec;
	private static PublicKey pubk;
    private static PrivateKey prvk;

	public static void runServer() {
		try {
			serverSocket = new ServerSocket(4444); // Server socket
		} catch (IOException e) {
			System.out.println("Could not listen on port: 4444");
		}
		System.out.println("Server started. Listening to the port 4444");
		// Step 1
		createKeyPair();
        System.out.println("KeyPair created");
		// Step 3
		createSymmetricKey();
        System.out.println("SymmetricKey created");
        int i = 0;
		while (true) {
			try {
                //System.out.println("Waiting Incoming Connection...");
				clientSocket = serverSocket.accept(); // accept the client connection
                System.out.println("accepted");
				ois = new ObjectInputStream(clientSocket.getInputStream());
				oos = new ObjectOutputStream(clientSocket.getOutputStream());
				Object o = ois.readObject();
                if (o instanceof String) {
					// Read encrypted message
					System.out.println(new String(code(aesCipher, aeskeySpec, Cipher.DECRYPT_MODE, Base64.getDecoder().decode(((String) o).getBytes()))));
                } else if (o instanceof byte[]) {
					// Step 2
					PublicKey clientpublickey = createPubK((byte[]) o);
					// Step 3
					byte[] send = new byte[128 + 162];
					byte[] encoded = pubk.getEncoded();
					byte[] code = code(pkCipher, clientpublickey, Cipher.PUBLIC_KEY, aesKey);
					System.arraycopy(encoded, 0, send, 0, encoded.length);
					System.arraycopy(code, 0, send, encoded.length, code.length);
					oos.write(send);
					oos.flush();
				}
				clientSocket.close();
			} catch (Exception ex) {
				//System.out.println("Problem in message reading");
                //i++;
			}
		}
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

	private static void createKeyPair(){
		KeyPairGenerator kpg;
		try {
					//Create an RSA key of 1024  bit
			kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(1024);
			KeyPair kp = kpg.genKeyPair();
            pubk = kp.getPublic();
            prvk = kp.getPrivate();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private static void createSymmetricKey() { // creates an aes key
		KeyGenerator keygen;
		try {
			//Create an AES key 128 bit
			keygen = KeyGenerator.getInstance("AES");
			keygen.init(128);
			SecretKey secret = keygen.generateKey();
			aesKey = secret.getEncoded();
            aeskeySpec = new SecretKeySpec(secret.getEncoded(), "AES");

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			pkCipher = Cipher.getInstance("RSA");
			aesCipher = Cipher.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
		runServer();
	}

}
