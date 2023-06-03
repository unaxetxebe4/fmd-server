package de.nulide.findmydevice.utils;

import androidx.annotation.VisibleForTesting;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.ByteArrayOutputStream;
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
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;


public class CypherUtils {

    private static final int AES_GCM_IV_SIZE_BYTES = 12; // byte = 96 bit
    @VisibleForTesting
    protected static final int AES_GCM_KEY_SIZE_BYTES = 32; // byte = 256 bit
    private static final int AES_GCM_TAG_SIZE_BITS = 128; // bit = 16 byte

    // Argon2: see the PROTOCOL.md
    private static final int ARGON2_T = 1;
    private static final int ARGON2_P = 4;
    private static final int ARGON2_M = 131072;
    private static final int ARGON2_HASH_LENGTH = 32; // byte = 256 bit
    private static final int ARGON2_SALT_LENGTH = 16; // byte = 128 bit

    // Contextualise all usages of Argon2 to provide some hacky key separation
    private static final String CONTEXT_STRING_ASYM_KEY_WRAP = "context:asymmetricKeyWrap";
    private static final String CONTEXT_STRING_FMD_PIN = "context:fmdPin";
    private static final String CONTEXT_STRING_LOGIN = "context:loginAuthentication";
    private static final String CONTEXT_PREFIX = "context:";

    // ------ Section: Password and hashing ------

    public static String hashPasswordForFmdPin(String password) {
        password = CONTEXT_STRING_FMD_PIN + password;
        byte[] salt = generateSecureRandom(ARGON2_SALT_LENGTH);
        Argon2Result result = hashPasswordArgon2(password, salt);
        return Argon2EncodingUtils.encode(result.hash, result.params);
    }

    public static String hashPasswordForLogin(String password) {
        byte[] salt = generateSecureRandom(ARGON2_SALT_LENGTH);
        return hashPasswordForLogin(password, salt);
    }

    public static String hashPasswordForLogin(String password, String saltBase64) {
        return hashPasswordForLogin(password, decodeBase64(saltBase64));
    }

    public static String hashPasswordForLogin(String password, byte[] saltBytes) {
        password = CONTEXT_STRING_LOGIN + password;
        Argon2Result result = hashPasswordArgon2(password, saltBytes);
        return Argon2EncodingUtils.encode(result.hash, result.params);
    }


    private static Argon2Result hashPasswordArgon2(String password, byte[] salt) {
        // Inspired by https://github.com/spring-projects/spring-security/blob/6.1.0/crypto/src/main/java/org/springframework/security/crypto/argon2/Argon2PasswordEncoder.java
        // and https://www.baeldung.com/java-argon2-hashing#2-implement-argon2-hashing-with-bouncy-castle
        if (!password.startsWith(CONTEXT_PREFIX)) {
            // This is a bug that should not happen
            // Be defensive to ensure all Argon2 usages are context-separated.
            throw new RuntimeException("Missing context string");
        }
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[ARGON2_HASH_LENGTH];

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

        return new Argon2Result(out, params);
    }

    public static boolean checkPasswordForFmdPin(String expectedHash, String password) {
        return checkPassword(expectedHash, CONTEXT_STRING_FMD_PIN + password);
    }

    public static boolean checkPasswordForLogin(String expectedHash, String password) {
        return checkPassword(expectedHash, CONTEXT_STRING_LOGIN + password);
    }

    private static boolean checkPassword(String expectedHash, String password) {
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

    // ------ Section: asymmetric key ------

    public static KeyPair genRsaKeyPair() {
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

    public static byte[] encryptWithKey(PublicKey pub, String msg) {
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

    public static String decryptWithKey(PrivateKey priv, byte[] encryptedMsg) {
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

    // ------ Section: asymmetric key (key backup) ------

    public static String encryptPrivateKeyWithPassword(PrivateKey priv, String password) {
        // Derive key with Argon2 as KDF and a context
        String contextualisedPw = CONTEXT_STRING_ASYM_KEY_WRAP + password;
        byte[] argonSalt = generateSecureRandom(ARGON2_SALT_LENGTH);

        Argon2Result result = hashPasswordArgon2(contextualisedPw, argonSalt);
        byte[] aesKey = result.hash;

        // PEM-encode the key and symmetrically encrypt
        String pem = pemEncodeRsaKey(priv);
        byte[] aesPlaintextBytes = pem.getBytes(StandardCharsets.UTF_8);
        byte[] aesCiphertextBytes = encryptWithAes(aesPlaintextBytes, aesKey);

        byte[] concat = concatByteArrays(argonSalt, aesCiphertextBytes);
        return encodeBase64(concat);
    }

    public static PrivateKey decryptPrivateKeyWithPassword(String encryptedPrivKey, String password) {
        byte[] concatBytes = decodeBase64(encryptedPrivKey);
        byte[] argonSalt = Arrays.copyOfRange(concatBytes, 0, ARGON2_SALT_LENGTH);
        byte[] ciphertextBytes = Arrays.copyOfRange(concatBytes, ARGON2_SALT_LENGTH, concatBytes.length);

        String contextualisedPw = CONTEXT_STRING_ASYM_KEY_WRAP + password;
        Argon2Result result = hashPasswordArgon2(contextualisedPw, argonSalt);
        byte[] aesKey = result.hash;

        byte[] aesPlaintextBytes = decryptWithAes(ciphertextBytes, aesKey);
        String pem = new String(aesPlaintextBytes, StandardCharsets.UTF_8);
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

    public static PrivateKey pemDecodeRsaKey(String pem) {
        try {
            pem = pem.replace("-----END RSA PRIVATE KEY-----\n", "");
            pem = pem.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
            pem = pem.replace("\n", "");
            byte[] key = decodeBase64(pem);

            EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(privKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------ Section: symmetric key ------

    public static byte[] encryptWithAes(byte[] msgBytes, byte[] aesKey) {
        if (aesKey.length != AES_GCM_KEY_SIZE_BYTES) {
            // This is a bug
            throw new RuntimeException("Bad AES key size:" + aesKey.length);
        }
        try {
            byte[] ivBytes = generateSecureRandom(AES_GCM_IV_SIZE_BYTES);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(AES_GCM_TAG_SIZE_BITS, ivBytes);
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);
            byte[] ctBytes = cipher.doFinal(msgBytes);

            return concatByteArrays(ivBytes, ctBytes);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | IllegalBlockSizeException | BadPaddingException |
                 InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static byte[] decryptWithAes(byte[] msgBytes, byte[] aesKey) {
        try {
            byte[] ivBytes = Arrays.copyOfRange(msgBytes, 0, AES_GCM_IV_SIZE_BYTES);
            byte[] ctBytes = Arrays.copyOfRange(msgBytes, AES_GCM_IV_SIZE_BYTES, msgBytes.length);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(AES_GCM_TAG_SIZE_BITS, ivBytes);
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);
            return cipher.doFinal(ctBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |
                 BadPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------ Section: utils ------

    public static String encodeBase64(byte[] toEncode) {
        return DatatypeConverter.printBase64Binary(toEncode);
    }

    public static byte[] decodeBase64(String toDecode) {
        return DatatypeConverter.parseBase64Binary(toDecode);
    }

    public static byte[] fromHex(String str) {
        return DatatypeConverter.parseHexBinary(str);
    }

    public static String toHex(byte[] ba) {
        return DatatypeConverter.printHexBinary(ba);
    }

    public static byte[] generateSecureRandom(int lengthInBytes) {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[lengthInBytes];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    public static byte[] concatByteArrays(byte[]... arrays) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            try {
                out.write(array);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out.toByteArray();
    }

    private static class Argon2Result {
        public final byte[] hash;
        public final Argon2Parameters params;


        public Argon2Result(byte[] hash, Argon2Parameters params) {
            this.hash = hash;
            this.params = params;
        }
    }

}
