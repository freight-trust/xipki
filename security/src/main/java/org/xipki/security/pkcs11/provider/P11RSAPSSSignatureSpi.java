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

package org.xipki.security.pkcs11.provider;

import java.io.ByteArrayOutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.xipki.security.pkcs11.P11PlainRSASigner;
import org.xipki.security.pkcs11.P11RSAKeyParameter;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */
// CHECKSTYLE:SKIP
public class P11RSAPSSSignatureSpi extends SignatureSpi {

    // CHECKSTYLE:SKIP
    public static class NonePSS extends P11RSAPSSSignatureSpi {

        public NonePSS() {
            super(null, true);
        }

    } // class nonePSS

    // CHECKSTYLE:SKIP
    public static class PSSwithRSA extends P11RSAPSSSignatureSpi {

        public PSSwithRSA() {
            super(null);
        }

    } // class PSSwithRSA

    // CHECKSTYLE:SKIP
    public static class SHA1withRSA extends P11RSAPSSSignatureSpi {

        public SHA1withRSA() {
            super(PSSParameterSpec.DEFAULT);
        }

    } // class SHA1withRSA

    // CHECKSTYLE:SKIP
    public static class SHA224withRSA extends P11RSAPSSSignatureSpi {

        public SHA224withRSA() {
            super(new PSSParameterSpec("SHA-224", "MGF1",
                    new MGF1ParameterSpec("SHA-224"), 28, 1));
        }

    } // class SHA224withRSA

    // CHECKSTYLE:SKIP
    public static class SHA256withRSA extends P11RSAPSSSignatureSpi {

        public SHA256withRSA() {
            super(new PSSParameterSpec("SHA-256", "MGF1",
                    new MGF1ParameterSpec("SHA-256"), 32, 1));
        }

    } // class SHA256withRSA

    // CHECKSTYLE:SKIP
    public static class SHA384withRSA extends P11RSAPSSSignatureSpi {

        public SHA384withRSA() {
            super(new PSSParameterSpec("SHA-384", "MGF1",
                    new MGF1ParameterSpec("SHA-384"), 48, 1));
        }

    } // class SHA384withRSA

    // CHECKSTYLE:SKIP
    public static class SHA512withRSA extends P11RSAPSSSignatureSpi {

        public SHA512withRSA() {
            super(new PSSParameterSpec("SHA-512", "MGF1",
                    new MGF1ParameterSpec("SHA-512"), 64, 1));
        }

    } // class SHA512withRSA

    // CHECKSTYLE:SKIP
    public static class SHA3_224withRSA extends P11RSAPSSSignatureSpi {

        public SHA3_224withRSA() {
            super(new PSSParameterSpec("SHA3-224", "MGF1",
                    new MGF1ParameterSpec("SHA3-224"), 28, 1));
        }

    } // class SHA224withRSA

    // CHECKSTYLE:SKIP
    public static class SHA3_256withRSA extends P11RSAPSSSignatureSpi {

        public SHA3_256withRSA() {
            super(new PSSParameterSpec("SHA3-256", "MGF1",
                    new MGF1ParameterSpec("SHA3-256"), 32, 1));
        }

    } // class SHA256withRSA

    // CHECKSTYLE:SKIP
    public static class SHA3_384withRSA extends P11RSAPSSSignatureSpi {

        public SHA3_384withRSA() {
            super(new PSSParameterSpec("SHA3-384", "MGF1",
                    new MGF1ParameterSpec("SHA3-384"), 48, 1));
        }

    } // class SHA384withRSA

    // CHECKSTYLE:SKIP
    public static class SHA3_512withRSA extends P11RSAPSSSignatureSpi {

        public SHA3_512withRSA() {
            super(new PSSParameterSpec("SHA3-512", "MGF1",
                    new MGF1ParameterSpec("SHA3-512"), 64, 1));
        }

    } // class SHA512withRSA

    private static class NullPssDigest implements Digest {

        private ByteArrayOutputStream baOut = new ByteArrayOutputStream();

        private Digest baseDigest;

        private boolean oddTime = true;

        NullPssDigest(final Digest mgfDigest) {
            this.baseDigest = mgfDigest;
        }

        @Override
        public String getAlgorithmName() {
            return "NULL";
        }

        @Override
        public int getDigestSize() {
            return baseDigest.getDigestSize();
        }

        @Override
        public void update(final byte in) {
            baOut.write(in);
        }

        @Override
        public void update(final byte[] in, final int inOff, final int len) {
            baOut.write(in, inOff, len);
        }

        @Override
        public int doFinal(final byte[] out, final int outOff) {
            byte[] res = baOut.toByteArray();

            if (oddTime) {
                System.arraycopy(res, 0, out, outOff, res.length);
            } else {
                baseDigest.update(res, 0, res.length);
                baseDigest.doFinal(out, outOff);
            }

            reset();
            oddTime = !oddTime;
            return res.length;
        }

        @Override
        public void reset() {
            baOut.reset();
            baseDigest.reset();
        }

    } // class NullPssDigest

    private AlgorithmParameters engineParams;

    private PSSParameterSpec paramSpec;

    private PSSParameterSpec originalSpec;

    private P11PlainRSASigner signer = new P11PlainRSASigner();

    private Digest contentDigest;

    private Digest mgfDigest;

    private int saltLength;

    private byte trailer;

    private boolean isRaw;

    private P11PrivateKey signingKey;

    private org.bouncycastle.crypto.signers.PSSSigner pss;

    protected P11RSAPSSSignatureSpi(final PSSParameterSpec paramSpecArg) {
        this(paramSpecArg, false);
    }

    protected P11RSAPSSSignatureSpi(final PSSParameterSpec baseParamSpec, final boolean isRaw) {
        this.originalSpec = baseParamSpec;
        this.paramSpec = (baseParamSpec == null) ? PSSParameterSpec.DEFAULT : baseParamSpec;
        this.mgfDigest = DigestFactory.getDigest(paramSpec.getDigestAlgorithm());
        this.saltLength = paramSpec.getSaltLength();
        this.trailer = getTrailer(paramSpec.getTrailerField());
        this.isRaw = isRaw;

        setupContentDigest();
    }

    protected void engineInitVerify(final PublicKey publicKey) throws InvalidKeyException {
        throw new UnsupportedOperationException("engineInitVerify unsupported");
    }

    protected void engineInitSign(final PrivateKey privateKey, final SecureRandom random)
            throws InvalidKeyException {
        if (!(privateKey instanceof P11PrivateKey)) {
            throw new InvalidKeyException("privateKey is not instanceof "
                    + P11PrivateKey.class.getName());
        }

        String algo = privateKey.getAlgorithm();
        if (!"RSA".equals(algo)) {
            throw new InvalidKeyException("privateKey is not an RSA private key: " + algo);
        }

        this.signingKey = (P11PrivateKey) privateKey;

        pss = new org.bouncycastle.crypto.signers.PSSSigner(signer, contentDigest, mgfDigest,
                saltLength, trailer);

        P11RSAKeyParameter p11KeyParam = P11RSAKeyParameter.getInstance(
                signingKey.p11CryptService(), signingKey.identityId());
        if (random == null) {
            pss.init(true, p11KeyParam);
        } else {
            pss.init(true, new ParametersWithRandom(p11KeyParam, random));
        }
    }

    @Override
    protected void engineInitSign(final PrivateKey privateKey) throws InvalidKeyException {
        engineInitSign(privateKey, null);
    }

    @Override
    protected void engineUpdate(final byte input) throws SignatureException {
        pss.update(input);
    }

    @Override
    protected void engineUpdate(final byte[] input, final int off, final int len)
            throws SignatureException {
        pss.update(input, off, len);
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        try {
            return pss.generateSignature();
        } catch (CryptoException ex) {
            throw new SignatureException(ex.getMessage(), ex);
        }
    }

    @Override
    protected boolean engineVerify(final byte[] sigBytes) throws SignatureException {
        throw new UnsupportedOperationException("engineVerify unsupported");
    }

    @Override
    protected void engineSetParameter(final AlgorithmParameterSpec params)
            throws InvalidParameterException {
        if (params instanceof PSSParameterSpec) {
            PSSParameterSpec newParamSpec = (PSSParameterSpec) params;

            if (originalSpec != null) {
                if (!DigestFactory.isSameDigest(originalSpec.getDigestAlgorithm(),
                        newParamSpec.getDigestAlgorithm())) {
                    throw new InvalidParameterException("parameter must be using "
                            + originalSpec.getDigestAlgorithm());
                }
            }
            if (!newParamSpec.getMGFAlgorithm().equalsIgnoreCase("MGF1")
                    && !newParamSpec.getMGFAlgorithm().equals(
                            PKCSObjectIdentifiers.id_mgf1.getId())) {
                throw new InvalidParameterException("unknown mask generation function specified");
            }

            if (!(newParamSpec.getMGFParameters() instanceof MGF1ParameterSpec)) {
                throw new InvalidParameterException("unkown MGF parameters");
            }

            MGF1ParameterSpec mgfParams = (MGF1ParameterSpec) newParamSpec.getMGFParameters();

            if (!DigestFactory.isSameDigest(mgfParams.getDigestAlgorithm(),
                    newParamSpec.getDigestAlgorithm())) {
                throw new InvalidParameterException(
                        "digest algorithm for MGF should be the same as for PSS parameters.");
            }

            Digest newDigest = DigestFactory.getDigest(mgfParams.getDigestAlgorithm());

            if (newDigest == null) {
                throw new InvalidParameterException(
                        "no match on MGF digest algorithm: " + mgfParams.getDigestAlgorithm());
            }

            this.engineParams = null;
            this.paramSpec = newParamSpec;
            this.mgfDigest = newDigest;
            this.saltLength = paramSpec.getSaltLength();
            this.trailer = getTrailer(paramSpec.getTrailerField());

            setupContentDigest();
        } else {
            throw new InvalidParameterException("only PSSParameterSpec supported");
        }
    } // method engineSetParameter

    @Override
    protected void engineSetParameter(final String param, final Object value) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        if (engineParams == null) {
            if (paramSpec != null) {
                try {
                    engineParams = AlgorithmParameters.getInstance("PSS", "BC");
                    engineParams.init(paramSpec);
                } catch (Exception ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }
        }

        return engineParams;
    }

    @Override
    protected Object engineGetParameter(final String param) {
        throw new UnsupportedOperationException("engineGetParameter unsupported");
    }

    private byte getTrailer(final int trailerField) {
        if (trailerField == 1) {
            return org.bouncycastle.crypto.signers.PSSSigner.TRAILER_IMPLICIT;
        }

        throw new IllegalArgumentException("unknown trailer field");
    }

    private void setupContentDigest() {
        this.contentDigest = isRaw ? new NullPssDigest(mgfDigest) : mgfDigest;
    }

}
