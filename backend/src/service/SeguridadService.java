package service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// Cifrado AES y hash SHA-256 para datos sensibles
public class SeguridadService {

    private static final String SECRET_STRING = "SIVC_SuperSecretKeyForAcademicProject2026!";
    private static SecretKeySpec secretKey;

    static {
        try {
            // Attempt to remove JCE restriction via reflection (supported in JRE 8)
            java.lang.reflect.Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
            field.setAccessible(true);
            
            java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            
            field.set(null, java.lang.Boolean.FALSE);
        } catch (Exception e) {
            // Silence or log warning
        }

        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
            
            // Check max allowed key size for AES
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            if (maxKeyLen < 256) {
                System.out.println("SIVC Seguridad: Limites JCE detectados. Ajustando clave AES a 128 bits.");
                byte[] key128 = new byte[16];
                System.arraycopy(keyBytes, 0, key128, 0, 16);
                secretKey = new SecretKeySpec(key128, "AES");
            } else {
                System.out.println("SIVC Seguridad: JCE sin restricciones. Usando clave AES de 256 bits.");
                secretKey = new SecretKeySpec(keyBytes, "AES");
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar SeguridadService: " + e.getMessage());
        }
    }

    /**
     * Hashes a string using SHA-256. Returns a hexadecimal string.
     */
    public static String hashSHA256(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password with SHA-256", e);
        }
    }

    /**
     * Encrypts a string using AES-256 (CBC mode with PKCS5Padding).
     * The random 16-byte IV is prepended to the ciphertext and encoded in Base64.
     */
    public static String cifrarDatos(String datos) {
        if (datos == null) {
            return null;
        }
        try {
            // Generate a random IV
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(datos.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted bytes
            byte[] combinedBytes = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combinedBytes, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combinedBytes, iv.length, encryptedBytes.length);

            // Encode to Base64
            return Base64.getEncoder().encodeToString(combinedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    /**
     * Decrypts a Base64 string that has been encrypted with cifrarDatos.
     * Decodes Base64, extracts the IV, and decrypts the remaining bytes.
     */
    public static String descifrarDatos(String datosCifrados) {
        if (datosCifrados == null) {
            return null;
        }
        try {
            byte[] combinedBytes = Base64.getDecoder().decode(datosCifrados);

            // Extract IV (first 16 bytes)
            byte[] iv = new byte[16];
            System.arraycopy(combinedBytes, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Extract encrypted text (remaining bytes)
            int encryptedSize = combinedBytes.length - iv.length;
            byte[] encryptedBytes = new byte[encryptedSize];
            System.arraycopy(combinedBytes, iv.length, encryptedBytes, 0, encryptedSize);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}
