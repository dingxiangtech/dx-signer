/**
 * dx-signer
 *
 * Copyright 2022 北京顶象技术有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dx.channel;

import com.android.apksig.ApkSigner;
import dx.zip.AxmlFastZipOut;
import dx.zip.FastZipEntry;
import dx.zip.FastZipIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

public class ApkSigns {
    private static final Logger log = LoggerFactory.getLogger(ApkSigns.class);
    private static final Pattern stripPattern = Pattern.compile("^META-INF/(.*)[.](SF|RSA|DSA|EC)$");

    public static void zipAlign(Path inputApk, Path outApk, boolean deleteSignature) throws IOException {
        try (FastZipIn in = new FastZipIn(inputApk.toFile())) {
            Path parent = outApk.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ByteBuffer manifest = null;
            for (FastZipEntry entry : in.entries()) {
                if (entry.utf8Name().equals("AndroidManifest.xml")) {
                    manifest = in.getUncompressed(entry);
                    break;
                }
            }

            try (AxmlFastZipOut out = new AxmlFastZipOut(outApk.toFile())) {
                if (manifest != null) {
                    out.initByAndroidManifestContent(manifest);
                }
                List<FastZipEntry> entries = in.entries();
                List<FastZipEntry> zipEntryList = cleanup(entries, deleteSignature);
                out.copy(in, zipEntryList);
            }
        } catch (DataFormatException e) {
            throw new IOException(e);
        }
    }

    public static List<FastZipEntry> cleanup(List<FastZipEntry> entries, boolean deleteSignature) {
        List<FastZipEntry> zipEntryList = new ArrayList<>();
        for (FastZipEntry e : entries) {
            String name = e.utf8Name();
            if (name.endsWith("/")) {
                // skip dir
                continue;
            }
            if (deleteSignature) {
                if (name.equals(JarFile.MANIFEST_NAME) || stripPattern.matcher(name).matches()) {
                    continue;
                }
            }
            zipEntryList.add(e);
        }
        return zipEntryList;
    }

    public static KeyStore.PrivateKeyEntry loadKey(Path ks, String ksPass, String keyAlias, String keyPass) throws IOException {
        KeyStore.PrivateKeyEntry privateKeyEntry = null;

        Set<String> passwordList = new TreeSet<>();
        if (ksPass != null) {
            passwordList.add(ksPass);
        }
        if (keyPass != null) {
            passwordList.add(keyPass);
        }
        passwordList.add(ksPass);
        passwordList.add("android");
        KeyStore keyStore = loadKeyStore(ks, passwordList);

        List<String> aliasesList = new ArrayList<>();
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    aliasesList.add(alias);
                }
            }
            if (keyAlias != null) {
                aliasesList.remove(keyAlias);
                aliasesList.add(0, keyAlias);
            }
        } catch (
                KeyStoreException e) {
            throw new RuntimeException(e);
        }


        for (String alias : aliasesList) {
            for (String pass : passwordList) {
                try {
                    KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(pass.toCharArray());
                    privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, param);
                    break;
                } catch (Exception ignore) {
                }
            }
        }
        if (privateKeyEntry == null) {
            throw new IOException("fail load key from keystore");
        }
        X509Certificate certificate = (X509Certificate) privateKeyEntry.getCertificate();

        log.info("loaded certificate {}", certificate.getSubjectDN());

        try {
            byte[] encoded = certificate.getEncoded();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            String md5hex = toHexString(md5.digest(encoded));

            log.info("cert md5: {}", md5hex);

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String sha256hex = toHexString(sha256.digest(encoded));

            log.info("cert sha256: {}", sha256hex);
        } catch (Exception ignore) {

        }

        return privateKeyEntry;
    }

    private static String toHexString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    public static KeyStore loadKeyStore(Path ks, Set<String> passwordList) throws IOException {
        return loadKeyStore(Files.readAllBytes(ks), passwordList);
    }

    public static KeyStore loadKeyStore(byte[] ksContent, Set<String> passwordList) throws IOException {
        List<String> storeTypes = Arrays.asList("PKCS12", "JKS", KeyStore.getDefaultType());
        for (String type : storeTypes) {
            for (String password : passwordList) {
                try {
                    KeyStore keyStore = KeyStore.getInstance(type);
                    keyStore.load(new ByteArrayInputStream(ksContent), password.toCharArray());
                    log.info("loaded keystore with type {}", type);
                    return keyStore;
                } catch (Exception ignore) {
                }
            }
        }

        log.warn("fail load keystore with type {}, bad type or bad password", storeTypes);
        throw new IOException("fail to open keystore");
    }


    public static void sign(Path in, Path out,
                            Path ks, String ksPass, String keyAlias, String keyPass,
                            boolean isAAB
    ) throws IOException {
        Path apkUnsigned = Files.createTempFile(out.getParent(), "unsigned", ".apk");
        try {
            zipAlign(in, apkUnsigned, true);
            KeyStore.PrivateKeyEntry privateKeyEntry1 = loadKey(ks, ksPass, keyAlias, keyPass);
            sign(apkUnsigned, out, privateKeyEntry1, isAAB);
        } finally {
            Files.deleteIfExists(apkUnsigned);
        }
    }

    public static void sign(Path in, Path out, KeyStore.PrivateKeyEntry key, boolean isAAB) throws IOException {
        List<X509Certificate> x509Certificates = new ArrayList<>();
        for (Certificate c : key.getCertificateChain()) {
            x509Certificates.add((X509Certificate) c);
        }
        ApkSigner.SignerConfig signerConfig =
                new ApkSigner.SignerConfig.Builder(
                        "cert", key.getPrivateKey(), x509Certificates)
                        .build();
        ApkSigner.Builder apkSignerBuilder =
                new ApkSigner.Builder(Collections.singletonList(signerConfig))
                        .setOtherSignersSignaturesPreserved(false)
                        .setV3SigningEnabled(false)
                        .setInputApk(in.toFile())
                        .setOutputApk(out.toFile());

        apkSignerBuilder.setV1SigningEnabled(true);
        apkSignerBuilder.setV2SigningEnabled(true);
        int minSdkVersion = isAAB ? 26 : 0;
        if (minSdkVersion > 0) {
            apkSignerBuilder.setMinSdkVersion(minSdkVersion);
        }
        ApkSigner signer = apkSignerBuilder.build();
        try {
            signer.sign();
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
