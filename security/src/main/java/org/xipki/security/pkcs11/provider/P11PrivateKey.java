/*
 *
 * Copyright (c) 2013 - 2017 Lijun Liao
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

package org.xipki.security.pkcs11.provider;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

import org.xipki.common.util.ParamUtil;
import org.xipki.security.exception.P11TokenException;
import org.xipki.security.exception.XiSecurityException;
import org.xipki.security.pkcs11.P11CryptService;
import org.xipki.security.pkcs11.P11EntityIdentifier;
import org.xipki.security.pkcs11.P11Params;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class P11PrivateKey implements PrivateKey {

    private static final long serialVersionUID = 1L;

    private final P11CryptService p11CryptService;

    private final P11EntityIdentifier identityId;

    private final String algorithm;

    private final int keysize;

    public P11PrivateKey(final P11CryptService p11CryptService,
            final P11EntityIdentifier identityId) throws P11TokenException {
        this.p11CryptService = ParamUtil.requireNonNull("identityId", p11CryptService);
        this.identityId = ParamUtil.requireNonNull("entityId", identityId);

        PublicKey publicKey = p11CryptService.getIdentity(identityId).publicKey();

        if (publicKey instanceof RSAPublicKey) {
            algorithm = "RSA";
            keysize = ((RSAPublicKey) publicKey).getModulus().bitLength();
        } else if (publicKey instanceof DSAPublicKey) {
            algorithm = "DSA";
            keysize = ((DSAPublicKey) publicKey).getParams().getP().bitLength();
        } else if (publicKey instanceof ECPublicKey) {
            algorithm = "EC";
            keysize = ((ECPublicKey) publicKey).getParams().getCurve().getField().getFieldSize();
        } else {
            throw new P11TokenException("unknown public key: " + publicKey);
        }
    }

    boolean supportsMechanism(final long mechanism) {
        try {
            return p11CryptService.getSlot(identityId.slotId()).supportsMechanism(mechanism);
        } catch (P11TokenException ex) {
            return false;
        }
    }

    @Override
    public String getFormat() {
        return null;
    }

    @Override
    public byte[] getEncoded() {
        return null;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    public int keysize() {
        return keysize;
    }

    /**
     *
     * @param parameters
     *          Parameters. Could be {@code null}.
     * @throws XiSecurityException
     * @throws P11TokenException
     */
    public byte[] sign(final long mechanism, final P11Params parameters,
            final byte[] content) throws XiSecurityException, P11TokenException {
        return p11CryptService.getIdentity(identityId).sign(mechanism, parameters, content);
    }

    P11CryptService p11CryptService() {
        return p11CryptService;
    }

    P11EntityIdentifier identityId() {
        return identityId;
    }

}
