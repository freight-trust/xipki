/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.security.pkcs12;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.HashAlgoType;
import org.xipki.security.bc.XiContentSigner;
import org.xipki.security.exception.XiSecurityException;
import org.xipki.security.util.AlgorithmUtil;

/**
 * @author Lijun Liao
 * @since 2.2.0
 */

public class HmacContentSigner implements XiContentSigner {

    private class HmacOutputStream extends OutputStream {

        @Override
        public void write(int bb) throws IOException {
            hmac.update((byte) bb);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            hmac.update(bytes, 0, bytes.length);
        }

        @Override
        public void write(byte[] bytes, int off, int len) throws IOException {
            hmac.update(bytes, off, len);
        }

    }

    private final AlgorithmIdentifier algorithmIdentifier;

    private final byte[] encodedAlgorithmIdentifier;

    private final HmacOutputStream outputStream;

    private final HMac hmac;

    private final int outLen;

    public HmacContentSigner(AlgorithmIdentifier algorithmIdentifier,
            SecretKey signingKey) throws XiSecurityException {
        this(null, algorithmIdentifier, signingKey);
    }

    public HmacContentSigner(HashAlgoType hashAlgo, AlgorithmIdentifier algorithmIdentifier,
            SecretKey signingKey) throws XiSecurityException {
        this.algorithmIdentifier = ParamUtil.requireNonNull("algorithmIdentifier",
                algorithmIdentifier);
        try {
            this.encodedAlgorithmIdentifier = algorithmIdentifier.getEncoded();
        } catch (IOException ex) {
            throw new XiSecurityException("could not encode AlgorithmIdentifier", ex);
        }
        ParamUtil.requireNonNull("signingKey", signingKey);
        if (hashAlgo == null) {
            hashAlgo = AlgorithmUtil.extractHashAlgoFromMacAlg(algorithmIdentifier);
        }

        this.hmac = new HMac(hashAlgo.createDigest());
        byte[] keyBytes = signingKey.getEncoded();
        this.hmac.init(new KeyParameter(keyBytes, 0, keyBytes.length));
        this.outLen = hmac.getMacSize();
        this.outputStream = new HmacOutputStream();
    }

    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return algorithmIdentifier;
    }

    @Override
    public byte[] getEncodedAlgorithmIdentifier() {
        return Arrays.copyOf(encodedAlgorithmIdentifier, encodedAlgorithmIdentifier.length);
    }

    @Override
    public OutputStream getOutputStream() {
        hmac.reset();
        return outputStream;
    }

    @Override
    public byte[] getSignature() {
        byte[] signature = new byte[outLen];
        hmac.doFinal(signature, 0);
        return signature;
    }

}
