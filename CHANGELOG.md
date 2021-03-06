# Change Log

See also <https://github.com/xipki/xipki/releases>

## 5.3.8
  - Release date: -

## 5.3.8
  - Release date: Jul 09, 2020
  - CA
    - Fixed BUG: Set extension critical if contains key-purpose timeStamping
    - Fixed Bug: add extension deltaCRLIndicator to DeltaCRL
    - Verify SCT before adding it to the cert
    - Unify the use of X.509 certificate and CRL
    - Add validation of IPv6 address
    - Log software version
    - Remove the CA controls DUPLICATE_{KEY|SUBJECT}
    - For pre-defined DSA parameters, using Pi as seed
    - Check pathLenConstraint before issuing certificate
    - Use tagNo or tagName to identify a SAN tag in a certificat request
    - accept also PEM encoded CSR in rest servlet
  - OCSP
    - Unify the use of X.509 certificate and CRL
    - Log software version
    - Use generatedAt instead thisUpdate for OCSP cache
  - CLI
    - Unify the use of X.509 certificate and CRL
    - For pre-defined DSA parameters, using Pi as seed
    - Add default value to slot, better usage for the param id

## 5.3.7
  - Release date: Mar 15, 2020
  - CA
    - Make XIPKI_BASE configurable.
    - Do not set the highest bit, increase the dflt bit length from 127 to 159 of serial numbers
    - Use overlap.days instead overlap.minutes to control the overlap in CRL
    - Update hikaricp 3.4.1 to 3.4.2, fastjson 1.2.62 to 1.2.66.
  - OCSP
    - Make XIPKI_BASE configurable.
    - Fixed #447 OCSP-server cannot parse CRLs without revoked certificates.
    - Corrected type from 'ejbca' to 'ejbca-db' in the configuration file.
    - Fixed #148 Ocspd ignores the folder certs in case of CRL as source in ocspd.
    - Use bytes instead of bits to specify the length of serial number.
    - Change fullcrl.intervals from 1 to 7.
    - Fixed #154 OCSP server cannot answer request with unknown extension.
    - Update hikaricp 3.4.1 to 3.4.2, fastjson 1.2.62 to 1.2.66.
  - CLI
    - Better print of time in the benchmark test
    - Update karaf 4.2.7 to 4.2.8, hikaricp 3.4.1 to 3.4.2, fastjson 1.2.62 to 1.2.66.

## 5.3.6
  - Release date: Jan 7, 2020
  - CA
    - BUG: Fixed #134 The issuerCertIssuer in the extension AKI is not set correctly
    - Better handle of proxyed TLS connection
    - Removed the support of insecure JKS keystore
  - OCSP
    - BUG: Fixed NPE
    - BUG: Fixed #137: set OCSP extension extendedRevoke to not critical
    - BUG Fixed #140: OCSP response cacher saves time in (incorrect) milliseconds instead of (correct) seconds.
    - Better handle of proxyed TLS connection
    - Removed the support of insecure JKS keystore
    - Changed the mode in ocsp-responder.json from RFC6960 to RFC2560 (configurable)
    - #138 Set the extension nonce in OCSP response as NOT critical
    - Include extn extendedRevoke only if unknown marked as revoked
  - CLI
    - Removed the support of insecure JKS keystore

## 5.3.6
  - Release date: Jan 7, 2020
  - CA
    - BUG: Fixed #134 The issuerCertIssuer in the extension AKI is not set correctly
    - Better handle of proxyed TLS connection
    - Removed the support of insecure JKS keystore
  - OCSP
    - BUG: Fixed NPE
    - BUG: Fixed #137: set OCSP extension extendedRevoke to not critical
    - BUG Fixed #140: OCSP response cacher saves time in (incorrect) milliseconds instead of (correct) seconds.
    - Better handle of proxyed TLS connection
    - Removed the support of insecure JKS keystore
    - Changed the mode in ocsp-responder.json from RFC6960 to RFC2560 (configurable)
    - #138 Set the extension nonce in OCSP response as NOT critical
    - Include extn extendedRevoke only if unknown marked as revoked
  - CLI
    - Removed the support of insecure JKS keystore

## 5.3.5
  - Release date: Nov 5, 2019
  - CA
    - Upgrade bcprov-jdk15on and bcpkix-jdk15on to 1.64
    - Fixed bug #128 "CA cannot start with NULL CMP_CONTROL"
    - Downgrade liquibase from 3.8.0 to 3.6.3 to support MariaDB 10.3+ 
    - Securities accepts explicit P11ModuleFactories
  - OCSP
    - Upgrade bcprov-jdk15on and bcpkix-jdk15on to 1.64
    - Downgrade liquibase from 3.8.0 to 3.6.3 to support MariaDB 10.3+ 
    - Securities accepts explicit P11ModuleFactories
  - CLI 
    - Optimized the display of benchmark with number over 1,000,000,000
    - Securities accepts explicit P11ModuleFactories

## 5.3.4
  - Release date: Sep 6, 2019
  - OCSP
    - Fixed bug "OCSP server cannot answer anymore" (this bug is introduced in 5.3.3)

## 5.3.3
  - Release date: Aug 8, 2019
  - CA
    - Add feature to log the the HTTP requests and responses
    - Remove the extension authorityInfoAccess from the mandatory list
  - OCSP
    - Add feature to log the the HTTP requests and responses
    - Add option to configure how to handle expired CRLs:
       - Default or ignoreExpiredCrls=false: consider the last imported CRLs as valid.
       - ignoreExpiredCrls=true: return OCSP response tryLater.

## 5.3.2
  - Release date: Jul 4, 2019
  - CA
    - Use EXPLICIT tag for the GMT 0015 IdentityCode
    - Reduce the table size to make it loadable if the database has charset utf8mb4 in MySQL/MariaDB.
  - ALL
    - Remove the leading zero of DSA's P, Q and G in CK_DSA_PARAMS.
    - EC: use Object Identifer of a curve instead name if possible
    - EdDSA: use curveId instead of curveName in PKCS#11 keypair generation.
  - OCSP
    - Added configuration of OCSP responder for the store types xipki-db, ejbca and crl.

## 5.3.1
  - Release date: Jun 10, 2019
  - CA
    - Replace the logging backend logback by log4j2.
    - Reintroduced the support of databases H2 and HSQLDB.
  - OCSP
    - Use stream parser to parse the CRL to get small memory usage even for very large CRLs.
    - Extend the OCSP store type "crl" to support multiple CRLs, even for the same CA.
    - Replace the logging backend logback by log4j2.
    - Reintroduced the support of databases H2 and HSQLDB.

## 5.3.0
  - Release date: May 12, 2019
  - CA
    - Add support of RFC8410 (Edwards and Montgomery Curves).
    - Add REST API to enroll certificate whose keypair is generated by the CA
  - OCSP
    - Add support of Ed25519 and Ed448 as signature algorithm.
  - CLI
    - Add support to generate keypair, generate CSR, and enrol certificates of edwards and montgomery curves.

## 5.2.0
  - Release date: Apr 27, 2019
  - CA
    - New feature to configure fixed value of subject RDN in the certificate profile
    - Make sure that the certificate serial number is randomly generated with at least 70 bit entropy and not weak by checking the NAF weight.
    - In the extension CertificatePolicies, the OID for User Notice is not correct. This has been fixed.
    - Add the management of the certificate of parenet CAs for given CA
    - Extension AuthorityKeyIdentifier embeds both KeyIdentifier and (authorityCertIssuer, authorityCertSerialNumber) in case of incorrect configuration. However exactly one of them is allowed. This has been fixed.
    - Add the native support of jurisdictionOfIncorporationCountryName, jurisdictionOfIncorporationLocalityName and jurisdictionOfIncorporationStateOrProvinceName
    - Add the native support of extensions IdentityCode, InsuranceNumber, ICRegistrationNumber, OrganizationCode and TaxationNumber defined in the chinese standard GM/T 0015
    - Add support of specification of extension admission in subject
    - Add CA/Browser certificate profiles.
    - Add support of Certificate Transparency (RFC 6962)
    - Increase the max. size of a certificate from 3000 to 4500 bytes.
  - OCSP
    - Add the configuration of OCSP response behaviour for unknown certificate
    - The OCSP cacher exhausts the database connections. This has been fixed.
  - CLI
    - Extend the command csr-p11 and csr-p12 to generate CSR with complex subject and extensions
    - Simplify and extend the configuration of custom extension
 
## 5.1.0
  - Release date: Mar 17, 2019
  - Relax the limitation of OCSP response in HTTP GET
  - New feature to add NextUpdate to OCSP Response, even if no NextUpdate is available. This is configurable.
  - Optimize the mechanism to generate CRL
  - Add example modules to demonstrate how to extend XiPKI OCSP server to use customized certificate status source.
  - Better mechanism to handle emailAddress in Subject / SubjectAltName
  - Add support of OCSP certificate status source published by EJBCA
  - Simplify the specification of customized extension in certificate profile.
  - If ca.war and ocsp.war are both in one tomcat instance, and one war cannot be started, the other too. This has been fixed.
  - Add support of certificate status source based on the database of XiPKI CA.

## 5.0.1
 - Release date: Feb 17, 2019
 - Validity other than {num}'y' will not be handled correctly. This has been fixed.
 - Increase the iteration count of PBKDF2 from 1000 to 10,000.
 - The flag 'crlUpdateInProcess' is not set correctly. This has been fixed.
 - OCSP-server DbCertStatusStore logic to detect issuer changes is wrong. This has been fixed.  

## 5.0.0
 - Release date: Dec 28, 2018
 - Optimized the file operations
 - Merged modules
 - Change the distributions
    - CA: from stand-alone karaf based appication to WAR package.
    - OCSP: from stand-alone karaf based appication to WAR package.
    - SDK: replaced by xipki-cli
    - CLI: Command Line Interface. Introduced in version 5.0.0.
 - Merged classes
 - Change the specification format of certificate profile from XML to JSON
 - Change the configuration format of CA, OCSP, PKCS#11 module, CMP client from XML to JSON
 - Add the remote management of CA via REST API
 - Add the remote management of OCSP via REST API
