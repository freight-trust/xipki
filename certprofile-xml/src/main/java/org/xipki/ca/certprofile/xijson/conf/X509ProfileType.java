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

package org.xipki.ca.certprofile.xijson.conf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.x509.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.api.profile.CertLevel;
import org.xipki.ca.api.profile.CertprofileException;
import org.xipki.ca.api.profile.ExtensionControl;
import org.xipki.ca.api.profile.ExtensionValue;
import org.xipki.ca.api.profile.KeyParametersOption;
import org.xipki.ca.api.profile.X509CertVersion;
import org.xipki.ca.certprofile.xijson.conf.Describable.DescribableOid;
import org.xipki.security.ObjectIdentifiers;
import org.xipki.util.Args;
import org.xipki.util.CollectionUtil;
import org.xipki.util.conf.InvalidConfException;
import org.xipki.util.conf.ValidatableConf;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class X509ProfileType extends ValidatableConf {

  private static final Logger LOG = LoggerFactory.getLogger(X509ProfileType.class);

  @JSONField(ordinal = 1)
  private Map<String, String> metadata;

  @JSONField(ordinal = 2)
  private X509CertVersion version;

  @JSONField(ordinal = 3)
  private CertLevel certLevel;

  @JSONField(ordinal = 4)
  private Boolean raOnly;

  @JSONField(ordinal = 5)
  private Integer maxSize;

  /**
   * The validity of the certificate to be generated, namely notAfter - notBefore.
   * Examples are:
   *  - 5y: 5 years
   *  - 365d: 365 days
   *  - 120h: 120 hours
   *  - 100m: 100 minutes
   */
  @JSONField(ordinal = 6)
  private String validity;

  /**
   * Value of the notBefore field:
   *  - 'current': current time
   *  - 'midnight'[:timezone]: the next mid night time for the given timezone.
   *               Valid timezones are: GMT+0, GMT+1, ..., GMT+12,
   *                                          GMT-1, ..., GMT-12
   *  - '+'offset: offset after current time,
   *  - '-'offset: before after current time, In the current implementation,
   *               offset of maximal 10 minutes is allowed.
   *
   *               The offset must have following suffixes:
   *               - 'd' for day, e.g. '2d' for 2 days,
   *               - 'h' for day, e.g. '2h' for 2 hours,
   *               - 'm' for day, e.g. '2m' for 2 minutes,
   *               - 's' for day, e.g. '2s' for 2 seconds.
   */
  @JSONField(ordinal = 7)
  private String notBeforeTime;

  @JSONField(ordinal = 8)
  private boolean serialNumberInReq;

  /**
   * Control how CA will generate the keypair for the certificate.
   * Defaults to forbidden.
   */
  @JSONField(ordinal = 9)
  private KeypairGenerationType keypairGeneration;

  /**
   * Signature algorithm name. Algorithms supported by the CA are
   * SHA*withECDSA, SHA*withDSA, SHA*withRSA, SHA*withRSAandMGF1, SHA*withPlainECDSA
   * where * is for 1, 224, 256, 384 and 512,
   * and SHA3-*withECDSA, SHA3-*withDSA, SHA3-*withRSA, SHA3-*withRSAandMGF1
   * where * is for 224, 256, 384 and 512.
   */
  @JSONField(ordinal = 10)
  private List<String> signatureAlgorithms;

  @JSONField(ordinal = 11)
  private List<AlgorithmType> keyAlgorithms;

  @JSONField(ordinal = 12)
  private Subject subject;

  @JSONField(ordinal = 13)
  private List<SubjectToSubjectAltNameType> subjectToSubjectAltNames;

  @JSONField(ordinal = 14)
  private List<ExtensionType> extensions;

  public static X509ProfileType parse(InputStream confStream) throws CertprofileException {
    Args.notNull(confStream, "confStream");
    try {
      X509ProfileType root = JSON.parseObject(confStream, X509ProfileType.class);
      root.validate();
      return root;
    } catch (IOException | InvalidConfException | RuntimeException ex) {
      throw new CertprofileException("parse profile failed, message: " + ex.getMessage(), ex);
    } finally {
      try {
        confStream.close();
      } catch (IOException ex) {
        LOG.warn("could not close confStream: {}", ex.getMessage());
      }
    }
  } // method parse

  public X509CertVersion getVersion() {
    return version;
  }

  public void setVersion(X509CertVersion version) {
    this.version = version;
  }

  public List<String> getSignatureAlgorithms() {
    if (signatureAlgorithms == null) {
      signatureAlgorithms = new LinkedList<>();
    }
    return signatureAlgorithms;
  }

  public void setSignatureAlgorithms(List<String> signatureAlgorithms) {
    this.signatureAlgorithms = signatureAlgorithms;
  }

  public CertLevel getCertLevel() {
    return certLevel;
  }

  public void setCertLevel(CertLevel certLevel) {
    this.certLevel = certLevel;
  }

  public Map<String, String> getMetadata() {
    if (metadata == null) {
      metadata = new HashMap<>();
    }
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public String getValidity() {
    return validity;
  }

  public void setValidity(String validity) {
    this.validity = validity;
  }

  public String getNotBeforeTime() {
    return notBeforeTime;
  }

  public void setNotBeforeTime(String notBeforeTime) {
    this.notBeforeTime = notBeforeTime;
  }

  public boolean isSerialNumberInReq() {
    return serialNumberInReq;
  }

  public void setSerialNumberInReq(boolean serialNumberInReq) {
    this.serialNumberInReq = serialNumberInReq;
  }

  public KeypairGenerationType getKeypairGeneration() {
    return keypairGeneration;
  }

  public void setKeypairGeneration(KeypairGenerationType keypairGeneration) {
    this.keypairGeneration = keypairGeneration;
  }

  public List<AlgorithmType> getKeyAlgorithms() {
    if (keyAlgorithms == null) {
      keyAlgorithms = new LinkedList<>();
    }
    return keyAlgorithms;
  }

  public void setKeyAlgorithms(List<AlgorithmType> keyAlgorithms) {
    this.keyAlgorithms = keyAlgorithms;
  }

  public Subject getSubject() {
    return subject;
  }

  public void setSubject(Subject subject) {
    this.subject = subject;
  }

  public List<SubjectToSubjectAltNameType> getSubjectToSubjectAltNames() {
    if (subjectToSubjectAltNames == null) {
      subjectToSubjectAltNames = new LinkedList<>();
    }
    return subjectToSubjectAltNames;
  }

  public void setSubjectToSubjectAltNames(
      List<SubjectToSubjectAltNameType> subjectToSubjectAltNames) {
    this.subjectToSubjectAltNames = subjectToSubjectAltNames;
  }

  public List<ExtensionType> getExtensions() {
    if (extensions == null) {
      extensions = new LinkedList<>();
    }
    return extensions;
  }

  public void setExtensions(List<ExtensionType> extensions) {
    this.extensions = extensions;
  }

  public Boolean getRaOnly() {
    return raOnly;
  }

  public void setRaOnly(Boolean raOnly) {
    this.raOnly = raOnly;
  }

  public Integer getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(Integer maxSize) {
    this.maxSize = maxSize;
  }

  public Map<String, ExtensionType> buildExtensions() {
    Map<String, ExtensionType> ret = new HashMap<>();
    for (ExtensionType m : getExtensions()) {
      String type = m.getType().getOid();
      ret.put(type, m);
    }
    return ret;
  }

  public Map<ASN1ObjectIdentifier, KeyParametersOption> toXiKeyAlgorithms()
      throws CertprofileException {
    Map<ASN1ObjectIdentifier, KeyParametersOption> keyAlgorithms = new HashMap<>();
    for (AlgorithmType type : this.keyAlgorithms) {
      List<DescribableOid> algIds = type.getAlgorithms();
      List<ASN1ObjectIdentifier> oids = new ArrayList<>(algIds.size());
      for (DescribableOid algId : algIds) {
        ASN1ObjectIdentifier oid = algId.toXiOid();
        if (keyAlgorithms.containsKey(oid)) {
          throw new CertprofileException("duplicate definition of keyAlgorithm " + oid.getId());
        }
        oids.add(oid);
      }

      KeyParametersOption keyParamsOption = type.getParameters().toXiKeyParametersOption();
      for (ASN1ObjectIdentifier oid : oids) {
        keyAlgorithms.put(oid, keyParamsOption);
      }
    }
    return CollectionUtil.unmodifiableMap(keyAlgorithms);
  } // method buildKeyAlgorithms

  @Override
  public void validate() throws InvalidConfException {
    notNull(version, "version");
    notNull(certLevel, "certLevel");
    notEmpty(validity, "validity");
    notEmpty(notBeforeTime, "notBeforeTime");
    validate(keypairGeneration);
    validate(keyAlgorithms);
    notNull(subject, "subject");
    validate(subject);
    validate(subjectToSubjectAltNames);
    notNull(extensions, "extensions");
    validate(extensions);

    Set<String> extnTypes = new HashSet<>();
    for (ExtensionType m : extensions) {
      String type = m.getType().getOid();
      if (!extnTypes.add(type)) {
        throw new InvalidConfException("duplicated definition of extension "
            + ObjectIdentifiers.getName(m.getType().toXiOid()));
      }
    }

  }

  public Map<ASN1ObjectIdentifier, ExtensionValue> buildConstantExtesions()
      throws CertprofileException {
    Map<ASN1ObjectIdentifier, ExtensionValue> map = new HashMap<>();

    for (ExtensionType m : getExtensions()) {
      ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(m.getType().getOid());
      if (Extension.subjectAlternativeName.equals(oid)
          || Extension.subjectInfoAccess.equals(oid)
          || Extension.biometricInfo.equals(oid)) {
        continue;
      }

      if (m.getConstant() == null) {
        continue;
      }

      byte[] encodedValue = m.getConstant().getValue();
      ASN1StreamParser parser = new ASN1StreamParser(encodedValue);
      ASN1Encodable value;
      try {
        value = parser.readObject();
      } catch (IOException ex) {
        throw new CertprofileException("could not parse the constant extension value", ex);
      }
      ExtensionValue extension = new ExtensionValue(m.isCritical(), value);
      map.put(oid, extension);
    }

    if (CollectionUtil.isEmpty(map)) {
      return null;
    }

    return Collections.unmodifiableMap(map);
  } // buildConstantExtesions

  public Map<ASN1ObjectIdentifier, ExtensionControl> buildExtensionControls()
      throws CertprofileException {
    // Extension controls
    Map<ASN1ObjectIdentifier, ExtensionControl> controls = new HashMap<>();
    for (ExtensionType extn : getExtensions()) {
      ASN1ObjectIdentifier oid = extn.getType().toXiOid();
      if (controls.containsKey(oid)) {
        throw new CertprofileException("duplicated definition of extension " + oid.getId());
      }

      ExtensionControl ctrl = new ExtensionControl(extn.isCritical(), extn.isRequired(),
          extn.isPermittedInRequest());
      controls.put(oid, ctrl);
    }

    return Collections.unmodifiableMap(controls);
  } // method buildExtensionControls

  public static Set<ASN1ObjectIdentifier> toOidSet(List<DescribableOid> oidWithDescTypes) {
    if (CollectionUtil.isEmpty(oidWithDescTypes)) {
      return null;
    }

    Set<ASN1ObjectIdentifier> oids = new HashSet<>();
    for (DescribableOid type : oidWithDescTypes) {
      oids.add(type.toXiOid());
    }
    return Collections.unmodifiableSet(oids);
  }

}
