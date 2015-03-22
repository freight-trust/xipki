/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2014 - 2015 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

package org.xipki.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSASSAPSSparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDefaultDigestProvider;
import org.bouncycastle.operator.bc.BcDigestProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;
import org.xipki.common.util.SecurityUtil;
import org.xipki.security.api.SignerException;

/**
 * utility class for converting java.security RSA objects into their
 * org.bouncycastle.crypto counterparts.
 *
 * @author Lijun Liao
 */

public class SignerUtil
{
    static public RSAKeyParameters generateRSAPublicKeyParameter(
        RSAPublicKey key)
    {
        return new RSAKeyParameters(false, key.getModulus(), key.getPublicExponent());

    }

    static public RSAKeyParameters generateRSAPrivateKeyParameter(
        RSAPrivateKey key)
    {
        if (key instanceof RSAPrivateCrtKey)
        {
            RSAPrivateCrtKey k = (RSAPrivateCrtKey)key;

            return new RSAPrivateCrtKeyParameters(k.getModulus(), k.getPublicExponent(), k.getPrivateExponent(),
                k.getPrimeP(), k.getPrimeQ(), k.getPrimeExponentP(), k.getPrimeExponentQ(), k.getCrtCoefficient());
        }
        else
        {
            RSAPrivateKey k = key;

            return new RSAKeyParameters(true, k.getModulus(), k.getPrivateExponent());
        }
    }

    static public PSSSigner createPSSRSASigner(AlgorithmIdentifier sigAlgId)
    throws OperatorCreationException
    {
        return createPSSRSASigner(sigAlgId, null);
    }

    static public PSSSigner createPSSRSASigner(AlgorithmIdentifier sigAlgId, AsymmetricBlockCipher cipher)
    throws OperatorCreationException
    {
        if(PKCSObjectIdentifiers.id_RSASSA_PSS.equals(sigAlgId.getAlgorithm()) == false)
        {
            throw new OperatorCreationException("signature algorithm " + sigAlgId.getAlgorithm() + " is not allowed");
        }

        BcDigestProvider digestProvider = BcDefaultDigestProvider.INSTANCE;
        AlgorithmIdentifier digAlgId;
        try
        {
            digAlgId = SecurityUtil.extractDigesetAlgorithmIdentifier(sigAlgId);
        } catch (NoSuchAlgorithmException e)
        {
            throw new OperatorCreationException(e.getMessage(), e);
        }
        Digest dig = digestProvider.get(digAlgId);
        if(cipher == null)
        {
            cipher = new RSABlindedEngine();
        }

        RSASSAPSSparams param = RSASSAPSSparams.getInstance(sigAlgId.getParameters());

        AlgorithmIdentifier mfgDigAlgId = AlgorithmIdentifier.getInstance(
                param.getMaskGenAlgorithm().getParameters());
        Digest mfgDig = digestProvider.get(mfgDigAlgId);

        int saltSize = param.getSaltLength().intValue();
        int trailerField = param.getTrailerField().intValue();

        return new PSSSigner(cipher, dig, mfgDig, saltSize, getTrailer(trailerField));
    }

    static private byte getTrailer(
            int trailerField)
    {
        if (trailerField == 1)
        {
            return org.bouncycastle.crypto.signers.PSSSigner.TRAILER_IMPLICIT;
        }

        throw new IllegalArgumentException("unknown trailer field");
    }

    static public RSASSAPSSparams createPSSRSAParams(ASN1ObjectIdentifier digestAlgOID)
    throws NoSuchAlgorithmException
    {
        int saltSize;
        if(X509ObjectIdentifiers.id_SHA1.equals(digestAlgOID))
        {
            saltSize = 20;
        }
        else if(NISTObjectIdentifiers.id_sha224.equals(digestAlgOID))
        {
            saltSize = 28;
        }
        else if(NISTObjectIdentifiers.id_sha256.equals(digestAlgOID))
        {
            saltSize = 32;
        }
        else if(NISTObjectIdentifiers.id_sha384.equals(digestAlgOID))
        {
            saltSize = 48;
        }
        else if(NISTObjectIdentifiers.id_sha512.equals(digestAlgOID))
        {
            saltSize = 64;
        }
        else
        {
            throw new NoSuchAlgorithmException(
                    "unknown digest algorithm " + digestAlgOID);
        }

        AlgorithmIdentifier digAlgId = new AlgorithmIdentifier(digestAlgOID, DERNull.INSTANCE);
        return new RSASSAPSSparams(
            digAlgId,
            new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, digAlgId),
            new ASN1Integer(saltSize),
            RSASSAPSSparams.DEFAULT_TRAILER_FIELD);
    }

    static public AlgorithmIdentifier buildRSAPSSAlgorithmIdentifier(
            ASN1ObjectIdentifier digAlgOid)
    throws NoSuchAlgorithmException
    {
        RSASSAPSSparams params = createPSSRSAParams(digAlgOid);
        return new AlgorithmIdentifier(PKCSObjectIdentifiers.id_RSASSA_PSS, params);
    }

    static public AlgorithmIdentifier buildDSASigAlgorithmIdentifier(AlgorithmIdentifier digAlgId)
    throws NoSuchAlgorithmException
    {
        ASN1ObjectIdentifier digAlgOid = digAlgId.getAlgorithm();
        ASN1ObjectIdentifier sid;
        if(X509ObjectIdentifiers.id_SHA1.equals(digAlgOid))
        {
            sid = X9ObjectIdentifiers.id_dsa_with_sha1;
        }
        else if(NISTObjectIdentifiers.id_sha224.equals(digAlgOid))
        {
            sid = NISTObjectIdentifiers.dsa_with_sha224;
        }
        else if(NISTObjectIdentifiers.id_sha256.equals(digAlgOid))
        {
            sid = NISTObjectIdentifiers.dsa_with_sha256;
        }
        else if(NISTObjectIdentifiers.id_sha384.equals(digAlgOid))
        {
            sid = NISTObjectIdentifiers.dsa_with_sha384;
        }
        else if(NISTObjectIdentifiers.id_sha512.equals(digAlgOid))
        {
            sid = NISTObjectIdentifiers.dsa_with_sha512;
        }
        else
        {
            throw new NoSuchAlgorithmException("no signature algorithm for DSA with digest algorithm " + digAlgOid.getId());
        }
        return new AlgorithmIdentifier(sid);
    }

    public static  boolean verifyPOP(CertificationRequest p10Request)
    {
        PKCS10CertificationRequest p10Req = new PKCS10CertificationRequest(p10Request);
        return verifyPOP(p10Req);
    }

    public static  boolean verifyPOP(PKCS10CertificationRequest p10Request)
    {
        try
        {
            SubjectPublicKeyInfo pkInfo = p10Request.getSubjectPublicKeyInfo();
            PublicKey pk = KeyUtil.generatePublicKey(pkInfo);

            ContentVerifierProvider cvp = KeyUtil.getContentVerifierProvider(pk);
            return p10Request.isSignatureValid(cvp);
        } catch (OperatorCreationException | InvalidKeyException | PKCSException |
                NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            return false;
        }
    }

    public static byte[] pkcs1padding(byte[] in, int blockSize)
    throws SignerException
    {
        int inLen = in.length;

        if (inLen+3 > blockSize)
        {
            throw new SignerException("data too long (maximal " + (blockSize - 3) + " allowed): " + inLen);
        }

        byte[]  block = new byte[blockSize];

        block[0] = 0x00;
        block[1] = 0x01;                        // type code 1

        for (int i = 2; i != block.length - inLen - 1; i++)
        {
            block[i] = (byte)0xFF;
        }

        block[block.length - inLen - 1] = 0x00;       // mark the end of the padding
        System.arraycopy(in, 0, block, block.length - inLen, inLen);
        return block;
    }

}

