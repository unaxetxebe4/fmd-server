package de.nulide.findmydevice.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

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

    // Argon2: see the PROTOCOL.md
    private static final int ARGON2_T = 1;
    private static final int ARGON2_P = 4;
    private static final int ARGON2_M = 131072;
    private static final int ARGON2_HASH_LENGTH = 32; // byte = 256 bit
    private static final int ARGON2_SALT_LENGTH = 16; // byte = 128 bit


    public static String hashPassword(String password) {
        // Inspired by https://github.com/spring-projects/spring-security/blob/6.1.0/crypto/src/main/java/org/springframework/security/crypto/argon2/Argon2PasswordEncoder.java
        // and https://www.baeldung.com/java-argon2-hashing#2-implement-argon2-hashing-with-bouncy-castle
        byte[] salt = generateSecureRandom(ARGON2_SALT_LENGTH);
        byte[] out = new byte[ARGON2_HASH_LENGTH];
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ARGON2_T)
                .withParallelism(ARGON2_P)
                .withMemoryAsKB(ARGON2_M)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        generator.generateBytes(passwordBytes, out);

        return Argon2EncodingUtils.encode(out, params);
    }

    public static boolean checkPassword(String expectedHash, String password) {
        if (expectedHash.isEmpty() || password.isEmpty()) {
            return false;
        }

        Argon2EncodingUtils.Argon2Hash decodedExpected;
        try {
            decodedExpected = Argon2EncodingUtils.decode(expectedHash);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }

        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = new byte[decodedExpected.getHash().length];

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(decodedExpected.getParameters());
        generator.generateBytes(passwordBytes, actualBytes);

        return Arrays.constantTimeAreEqual(decodedExpected.getHash(), actualBytes);
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
