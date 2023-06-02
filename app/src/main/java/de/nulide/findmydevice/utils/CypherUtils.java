package de.nulide.findmydevice.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class CypherUtils {

    private static final int IV_SIZE = 128;
    private static final int IV_LENGTH = IV_SIZE / 4;
    private static final int keySize = 256;
    private static final int iterationCount = 1867;
    private static final int saltLength = keySize / 4;

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt(12));
    }

    public static boolean checkPassword(String hash, String password) {
        if(!hash.isEmpty() && !password.isEmpty()) {
            return BCrypt.checkpw(password, hash);
        }
        return false;
    }

    public static String hashWithPKBDF2(String password){
        try {
            String salt = toHex(generateSecureRandom(keySize / 8));
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), fromHex(salt), iterationCount*2, keySize);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return salt+"///SPLIT///"+toHex(factory.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String hashWithPKBDF2WithGivenSalt(String password, String salt){
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), fromHex(salt), iterationCount*2, keySize);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return toHex(factory.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static KeyPair genRsaKeyPair(){
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(1024, random);
            KeyPair pair = keyGen.generateKeyPair();
            return pair;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encryptWithKey(PublicKey pub, String msg){
        final Cipher rsa;
        try {
            rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.ENCRYPT_MODE, pub);
            return rsa.doFinal(msg.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String decryptWithKey(PrivateKey priv, byte[] encryptedMsg){
        final Cipher rsa;
        try {
            rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.DECRYPT_MODE, priv);
            return new String(rsa.doFinal(encryptedMsg), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encryptKeyWithPassword(PrivateKey priv, String password) {
        String pem = pemEncodeRsaKey(priv);
        return encryptWithAES(pem.getBytes(), password);
    }

    public static PrivateKey decryptKeyWithPassword(String encryptedPrivKey, String password) {
        byte[] decryptedKey = decryptWithAES(encryptedPrivKey, password);
        String pem = new String(decryptedKey);
        return pemDecodeRsaKey(pem);
    }

    public static String pemEncodeRsaKey(PrivateKey priv) {
        StringWriter sw = new StringWriter();
        PemWriter writer = new PemWriter(sw);
        PemObject po = new PemObject("RSA PRIVATE KEY", priv.getEncoded());
        try {
            writer.writeObject(po);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sw.getBuffer().toString();
    }

    public static PrivateKey pemDecodeRsaKey(String pem){
        try {
            pem = pem.replace("-----END RSA PRIVATE KEY-----\n", "");
            pem = pem.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
            pem = pem.replace("\n", "");
            byte[] key = decodeBase64(pem);

            EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(privKeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        return null;
    }

    public static String encryptWithAES(byte[] msg, String password){
        try {
            String salt = toHex(generateSecureRandom(keySize / 8));
            String iv = toHex(generateSecureRandom(IV_SIZE / 8));
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), fromHex(salt), iterationCount, keySize);
            SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(fromHex(iv)));
            byte[] encrypted = cipher.doFinal(msg);
            String encryptedBase64 = encodeBase64(encrypted);
            return salt + iv + encryptedBase64;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static byte[] decryptWithAES(String encryptedMsg, String password){
        try {
            String salt = encryptedMsg.substring(0, saltLength);
            String iv = encryptedMsg.substring(saltLength, saltLength + IV_LENGTH);
            String ct = encryptedMsg.substring(saltLength + IV_LENGTH);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), fromHex(salt), iterationCount, keySize);
            SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            byte[] encrypted = decodeBase64(ct);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(fromHex(iv)));
            byte[] decrypted = cipher.doFinal(encrypted);
            return decrypted;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encodeBase64(byte[] toEncode){
        return DatatypeConverter.printBase64Binary(toEncode);
    }

    public static byte[] decodeBase64(String toDecode){
        return DatatypeConverter.parseBase64Binary(toDecode);
    }

    public static byte[] fromHex(String str) {
        return DatatypeConverter.parseHexBinary(str);
    }

    public static String toHex(byte[] ba) {
        return DatatypeConverter.printHexBinary(ba);
    }

    public static byte[] generateSecureRandom(int length) {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[length];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

}
