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

package org.xipki.ca.server.mgmt.api;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class PermissionConstants {

  private static final Map<Integer, String> codeTextMap = new HashMap<>();
  private static final Map<String, Integer> textCodeMap = new HashMap<>();

  public static final int ENROLL_CERT = 1;
  public static final int REVOKE_CERT = 2;
  public static final int UNREVOKE_CERT = 4;
  public static final int REMOVE_CERT = 8;
  public static final int KEY_UPDATE = 16;
  public static final int GEN_CRL = 32;
  public static final int GET_CRL = 64;
  public static final int ENROLL_CROSS = 128;
  public static final int GEN_KEYPAIR = 256;

  public static final int ALL = GEN_KEYPAIR | ENROLL_CERT | REVOKE_CERT | UNREVOKE_CERT
      | REMOVE_CERT | KEY_UPDATE | GEN_CRL | GET_CRL | ENROLL_CROSS;

  static {
    codeTextMap.put(ENROLL_CERT, "enroll_cert");
    codeTextMap.put(REVOKE_CERT, "revoke_cert");
    codeTextMap.put(UNREVOKE_CERT, "unrevoke_cert");
    codeTextMap.put(REMOVE_CERT, "remove_cert");
    codeTextMap.put(KEY_UPDATE, "key_update");
    codeTextMap.put(GEN_CRL, "gen_crl");
    codeTextMap.put(GET_CRL, "get_crl");
    codeTextMap.put(ENROLL_CROSS, "enroll_cross");
    codeTextMap.put(GEN_KEYPAIR, "gen_keypair");
    codeTextMap.put(ALL, "all");

    for (Integer code : codeTextMap.keySet()) {
      textCodeMap.put(codeTextMap.get(code), code);
    }
  }

  private PermissionConstants() {
  }

  public static boolean contains(int permissionA, int permissionB) {
    return (permissionA & permissionB) == permissionB;
  }

  public static Integer getPermissionForText(String text) {
    return (text == null) ? null : textCodeMap.get(text.toLowerCase());
  }

  public static String getTextForCode(int code) {
    String text = codeTextMap.get(code);
    return (text == null) ? Integer.toString(code) : text;
  }

}
