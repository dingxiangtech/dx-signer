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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.*;

/**
 * JDK-8184341 : Release Note: New defaults for DSA keys in jarsigner and keytool
 * <a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8184341">https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8184341</a>
 * <p>
 * 高版本java(8u151+)中DSA限制了最小的长度，而Android低版本兼容要求使用sha1算法，
 * apksigner为了兼容，选择使用Sha1WithDSA签名算法, 进而产生崩溃
 *
 * <pre>
 * Caused by: java.security.InvalidKeyException: The security strength of SHA-1 digest algorithm is not sufficient for this key size
 * at java.base/sun.security.provider.DSA.checkKey(DSA.java:124)
 * at java.base/sun.security.provider.DSA.engineInitSign(DSA.java:156)
 * ...
 * at java.base/java.security.Signature.initSign(Signature.java:636)
 * at com.android.apksig.internal.apk.v1.V1SchemeSigner.generateSignatureBlock(V1SchemeSigner.java:511)
 * </pre>
 */
public class FixSha1WithDsaProvider extends Provider {
    private static final Logger log = LoggerFactory.getLogger(FixSha1WithDsaProvider.class);
    public FixSha1WithDsaProvider() {
        super("fix-sha1-with-dsa-provider", 1.0, "null");
        // 使用BC的实现
        // put("Signature.SHA1WITHDSA", org.bouncycastle.jcajce.provider.asymmetric.dsa.DSASigner.stdDSA.class.getName());

        // 使用RawDSA实现
        put("Signature.SHA1WITHDSA", MySignatureSpi.class.getName());
    }

    public static class MySignatureSpi extends SignatureSpi {

        private static boolean logged = false;
        public MySignatureSpi() throws NoSuchAlgorithmException {
            this.sha1 = MessageDigest.getInstance("SHA-1");
            this.rawDSA = Signature.getInstance("RawDSA");
            if (!logged) {
                logged = true;
                log.info("Sha1WithDSA patch enabled");
            }
        }

        private final MessageDigest sha1;
        private final Signature rawDSA;

        @Override
        protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
            rawDSA.initVerify(publicKey);
            sha1.reset();
        }

        @Override
        protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
            rawDSA.initSign(privateKey);
            sha1.reset();
        }

        protected void engineUpdate(byte b) {
            sha1.update(b);
        }

        protected void engineUpdate(byte[] data, int off, int len) {
            sha1.update(data, off, len);
        }

        protected void engineUpdate(ByteBuffer b) {
            sha1.update(b);
        }

        @Override
        protected byte[] engineSign() throws SignatureException {
            byte[] data = sha1.digest();
            rawDSA.update(data);
            return rawDSA.sign();
        }

        @Override
        protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
            byte[] data = sha1.digest();
            rawDSA.update(data);
            return rawDSA.verify(sigBytes);
        }

        @Override
        protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
            throw new InvalidParameterException("No parameter accepted");
        }

        @Override
        protected Object engineGetParameter(String param) throws InvalidParameterException {
            return null;
        }
    }
}
