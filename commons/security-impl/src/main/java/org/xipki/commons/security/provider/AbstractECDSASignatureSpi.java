/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2013 - 2016 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License (version 3
 * or later at your option) as published by the Free Software Foundation
 * with the addition of the following permission added to Section 15 as
 * permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.commons.security.provider;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.spec.AlgorithmParameterSpec;

import org.bouncycastle.crypto.Digest;
import org.xipki.commons.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */
// CHECKSTYLE:SKIP
abstract class AbstractECDSASignatureSpi extends SignatureSpi {

    private Digest digest;

    private P11PrivateKey signingKey;

    private boolean x962;

    AbstractECDSASignatureSpi(
            final Digest digest,
            final boolean x962) {
        this.digest = ParamUtil.requireNonNull("digest", digest);
        this.x962 = x962;
    }

    @Override
    protected void engineInitVerify(
            final PublicKey publicKey)
    throws InvalidKeyException {
        throw new UnsupportedOperationException("engineInitVerify unsupported");
    }

    @Override
    protected void engineInitSign(
            final PrivateKey privateKey)
    throws InvalidKeyException {
        if (!(privateKey instanceof P11PrivateKey)) {
            throw new InvalidKeyException("privateKey is not instanceof "
                    + P11PrivateKey.class.getName());
        }

        String algo = privateKey.getAlgorithm();
        if (!("EC".equals(algo) || "ECDSA".equals(algo))) {
            throw new InvalidKeyException("privateKey is not a EC private key: " + algo);
        }

        digest.reset();
        this.signingKey = (P11PrivateKey) privateKey;
    }

    @Override
    protected void engineUpdate(
            final byte input)
    throws SignatureException {
        digest.update(input);
    }

    @Override
    protected void engineUpdate(
            final byte[] input,
            final int off,
            final int len)
    throws SignatureException {
        digest.update(input, off, len);
    }

    @Override
    protected byte[] engineSign()
    throws SignatureException {
        byte[] hash = new byte[digest.getDigestSize()];

        digest.doFinal(hash, 0);

        try {
            if (x962) {
                return signingKey.CKM_ECDSA_X962(hash);
            } else {
                return signingKey.CKM_ECDSA_Plain(hash);
            }
        } catch (SignatureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SignatureException(ex.getMessage(), ex);
        }
    }

    @Override
    protected void engineSetParameter(
            final AlgorithmParameterSpec params) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    @Override
    protected void engineSetParameter(
            final String param,
            final Object value) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    @Override
    protected Object engineGetParameter(
            final String param) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    @Override
    protected boolean engineVerify(
            final byte[] sigBytes)
    throws SignatureException {
        throw new UnsupportedOperationException("engineVerify unsupported");
    }

}