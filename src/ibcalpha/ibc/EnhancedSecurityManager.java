package ibcalpha.ibc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class EnhancedSecurityManager {
    // 修复 1: 修正 Logger 宿主类为本类名称
    private static final Logger logger = LoggerFactory.getLogger(EnhancedSecurityManager.class);
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;

    private static final Charset charset = StandardCharsets.UTF_8;
    private KeyStore keyStore;
    private char[] derivedPassword;
    private final String propertiesPath;
    private final String jvmArgsStr;
    private boolean isDebug = false;

    record CryptoPair(String key, SecretKey secretKey) {
        void setEntry(KeyStore keyStore, KeyStore.ProtectionParameter protectionParam) throws KeyStoreException {
            keyStore.setEntry(key, new KeyStore.SecretKeyEntry(secretKey), protectionParam);
        }
    }

    public EnhancedSecurityManager(String propertiesPath) {
        this.propertiesPath = propertiesPath;
        List<String> rawArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        StringBuilder sb = new StringBuilder();
        for (String arg : rawArgs) {
            if (arg.contains("jdwp=")) {
                this.isDebug = true;
            }
            // 过滤掉该参数,这是为了冷启动而存在的
            if (!arg.startsWith("-Dibcsessionid=")) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(arg);
            }
        }
        this.jvmArgsStr = sb.toString();
    }

    public Properties initialize(String... keys) {
        File propFile = new File(propertiesPath);
        Properties props = new Properties();
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                props.load(fis);
                this.keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            } catch (IOException e) {
                logger.error("Failed to read properties file at: {}", propertiesPath, e);
            } catch (KeyStoreException e) {
                logger.error("Unsupported KeyStore type: {}", KEYSTORE_TYPE, e);
            }
        } else {
            logger.warn("Properties file not found at path: {}", propertiesPath);
        }
        String keystorePath = propertiesPath;
        int dotIndex = propertiesPath.lastIndexOf(".");
        if (dotIndex > -1) {
            keystorePath = propertiesPath.substring(0, dotIndex);
        }
        keystorePath = keystorePath + ".jks";
        File ksFile = new File(keystorePath);
        // 判断是“首次启动”还是“后续启动”
        if (!ksFile.exists()) {
            logger.info("Keystore file does not exist. Triggering initial credential migration and timestamp watermarking...");
            List<CryptoPair> list = new ArrayList<>();
            for (String key : keys) {
                if (props.containsKey(key)) {
                    byte[] secret = ((String) props.remove(key)).getBytes(charset);
                    if (secret.length > 0) {
                        SecretKey secretKey = new SecretKeySpec(secret, "AES");
                        list.add(new CryptoPair(key, secretKey));
                    }
                }
            }
            if (!isDebug) {
                try (FileOutputStream fos = new FileOutputStream(propFile)) {
                    props.store(fos, "Keystore initialized.");
                } catch (IOException e) {
                    logger.error("Failed to persist modified properties file to disk.", e);
                }
            }
            this.derivedPassword = generateDerivedPassword(propFile);
            if (this.derivedPassword == null) {
                throw new IllegalStateException("Failed to derive keystore password from environment parameters.");
            }
            try {
                this.keyStore.load(null, this.derivedPassword);
                KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(this.derivedPassword);
                for (CryptoPair pair : list) {
                    pair.setEntry(this.keyStore, protectionParam);
                }
            } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
                logger.error("Failed to store encrypted entries inside the KeyStore object.", e);
            }
            // 持久化保存到磁盘
            if (!isDebug) {
                try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                    this.keyStore.store(fos, this.derivedPassword);
                    logger.info("Successfully initialized encrypted Keystore file at: {}", keystorePath);
                } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
                    logger.error("Failed to store encrypted Keystore file to disk.", e);
                }
            }
        } else {
            logger.info("Keystore file detected. Running subsequent startup environment and timestamp verification...");
            this.derivedPassword = generateDerivedPassword(propFile);
            if (this.derivedPassword == null) {
                throw new IllegalStateException("Failed to regenerate derived password during subsequent startup.");
            }
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                this.keyStore.load(fis, this.derivedPassword);
                logger.info("Keystore successfully verified and loaded from disk.");
            } catch (FileNotFoundException e) {
                logger.error("KeyStore file not found at: {}", keystorePath, e);
            } catch (IOException e) {
                logger.error("Failed to load or verify KeyStore at: {}. Check password or file integrity.", keystorePath, e);
            } catch (CertificateException | NoSuchAlgorithmException e) {
                logger.error("Environment tampering or incorrect JVM arguments detected. Failed to unlock Keystore.", e);
            }
        }
        return props;
    }

    /**
     * 结合 JVM 参数与修改后的 Properties 文件内容生成 PBKDF2 密码
     */
    private char[] generateDerivedPassword(File propFile) {
        char[] pbePassword = jvmArgsStr.toCharArray();
        SecretKey tmp = null;
        try {
            byte[] salt = Files.readAllBytes(Paths.get(propFile.toURI()));
            if (salt.length == 0) {
                salt = "FallbackSaltIfFileIsEmpty".getBytes(charset);
            }

            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec spec = new PBEKeySpec(pbePassword, salt, ITERATION_COUNT, KEY_LENGTH);
            tmp = factory.generateSecret(spec);
        } catch (IOException e) {
            logger.error("Failed to read properties file byte stream for password derivation.", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Required cryptographic algorithm [{}] is unavailable in this JVM environment.", PBKDF2_ALGORITHM, e);
        } catch (InvalidKeySpecException e) {
            logger.error("Invalid key specification generated for PBKDF2 process.", e);
        } finally {
            Arrays.fill(pbePassword, ' ');
        }
        if (tmp == null) return null;
        byte[] encoded = tmp.getEncoded();
        char[] hexChars = new char[encoded.length * 2];
        char[] hexArray = "0123456789abcdef".toCharArray();
        for (int j = 0; j < encoded.length; j++) {
            int v = encoded[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        logger.info("generateDerivedPassword={}", hexChars);
        return hexChars;
    }

    /**
     * 获取明文解密能力（供程序后续调用）
     */
    public String getCredential(String alias) {
        if (keyStore == null) {
            throw new IllegalStateException("Security manager is not initialized properly!");
        }
        KeyStore.SecretKeyEntry secretKeyEntry = null;
        KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(this.derivedPassword);
        try {
            secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, protectionParam);
        } catch (Exception e) {
            logger.error("Failed to retrieve or decrypt secret entry with alias: [{}] from Keystore.", alias, e);
        }
        if (secretKeyEntry == null) {
            return null;
        }
        SecretKey secretKey = secretKeyEntry.getSecretKey();
        return new String(secretKey.getEncoded(), charset);
    }

    public String getPropertiesPath() {
        return propertiesPath;
    }

}
