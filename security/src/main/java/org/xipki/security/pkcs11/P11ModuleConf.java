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

package org.xipki.security.pkcs11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.password.PasswordResolver;
import org.xipki.security.pkcs11.conf.MechanimFilterType;
import org.xipki.security.pkcs11.conf.MechanismSetType;
import org.xipki.security.pkcs11.conf.ModuleType;
import org.xipki.security.pkcs11.conf.NativeLibraryType;
import org.xipki.security.pkcs11.conf.PasswordSetType;
import org.xipki.security.pkcs11.conf.SlotType;
import org.xipki.util.Args;
import org.xipki.util.CollectionUtil;
import org.xipki.util.StringUtil;
import org.xipki.util.conf.InvalidConfException;

import iaik.pkcs.pkcs11.constants.Functions;
import iaik.pkcs.pkcs11.constants.PKCS11Constants;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class P11ModuleConf {

  private static final Logger LOG = LoggerFactory.getLogger(P11ModuleConf.class);

  private final String name;

  private final String type;

  private final String nativeLibrary;

  private final boolean readOnly;

  private final Set<P11SlotIdFilter> excludeSlots;

  private final Set<P11SlotIdFilter> includeSlots;

  private final P11PasswordsRetriever passwordRetriever;

  private final P11MechanismFilter mechanismFilter;

  private final int maxMessageSize;

  private final long userType;

  private final P11NewObjectConf newObjectConf;

  public P11ModuleConf(ModuleType moduleType, List<MechanismSetType> mechanismSets,
      PasswordResolver passwordResolver) throws InvalidConfException {
    Args.notNull(moduleType, "moduleType");
    Args.notEmpty(mechanismSets, "mechanismSets");
    this.name = moduleType.getName();
    this.readOnly = moduleType.isReadonly();

    String userTypeStr = moduleType.getUser().toUpperCase();
    if ("CKU_USER".equals(userTypeStr)) {
      this.userType = PKCS11Constants.CKU_USER;
    } else if ("CKU_SO".equals(userTypeStr)) {
      this.userType = PKCS11Constants.CKU_SO;
    } else if ("CKU_CONTEXT_SPECIFIC".equals(userTypeStr)) {
      this.userType = PKCS11Constants.CKU_CONTEXT_SPECIFIC;
    } else {
      try {
        if (userTypeStr.startsWith("0X")) {
          this.userType = Long.parseLong(userTypeStr.substring(2), 16);
        } else {
          this.userType = Long.parseLong(userTypeStr);
        }
      } catch (NumberFormatException ex) {
        throw new InvalidConfException("invalid user " + userTypeStr);
      }
    }

    this.maxMessageSize = moduleType.getMaxMessageSize();
    this.type = moduleType.getType();
    if (maxMessageSize < 128) {
      throw new InvalidConfException("invalid maxMessageSize (< 128): " + maxMessageSize);
    }

    // parse mechanismSets
    Map<String, Set<Long>> mechanismSetsMap = new HashMap<>(mechanismSets.size() * 3 / 2);
    for (MechanismSetType m : mechanismSets) {
      String name = m.getName();
      if (mechanismSetsMap.containsKey(name)) {
        throw new InvalidConfException("Duplication mechanismSets named " + name);
      }

      Set<Long> mechanisms = new HashSet<>();
      for (String mechStr : m.getMechanisms()) {
        mechStr = mechStr.trim().toUpperCase();
        if (mechStr.equals("ALL")) {
          mechanisms = null; // accept all mechanisms
          break;
        }

        Long mech = null;
        if (mechStr.startsWith("CKM_")) {
          mech = Functions.mechanismStringToCode(mechStr);
        } else {
          int radix = 10;
          if (mechStr.startsWith("0X")) {
            radix = 16;
            mechStr = mechStr.substring(2);
          }

          if (mechStr.endsWith("UL")) {
            mechStr = mechStr.substring(0, mechStr.length() - 2);
          } else if (mechStr.endsWith("L")) {
            mechStr = mechStr.substring(0, mechStr.length() - 1);
          }

          try {
            mech = Long.parseLong(mechStr, radix);
          } catch (NumberFormatException ex) {// CHECKSTYLE:SKIP
          }
        }

        if (mech == null) {
          LOG.warn("skipped unknown mechanism '" + mechStr + "'");
        } else {
          mechanisms.add(mech);
        }
      }

      mechanismSetsMap.put(name, mechanisms);
    }

    // Mechanism filter
    mechanismFilter = new P11MechanismFilter();

    List<MechanimFilterType> mechFilters = moduleType.getMechanismFilters();
    if (mechFilters != null && CollectionUtil.isNonEmpty(mechFilters)) {
      for (MechanimFilterType filterType : mechFilters) {
        Set<P11SlotIdFilter> slots = getSlotIdFilters(filterType.getSlots());
        String mechanismSetName = filterType.getMechanismSet();

        if (!mechanismSetsMap.containsKey(mechanismSetName)) {
          throw new InvalidConfException("MechanismSet '" +  mechanismSetName + "' is not defined");
        }

        Set<Long> mechanisms = mechanismSetsMap.get(mechanismSetName);
        if (mechanisms == null) {
          mechanismFilter.addAcceptAllEntry(slots);
        } else {
          mechanismFilter.addEntry(slots, mechanisms);
        }
      }
    }

    // Password retriever
    passwordRetriever = new P11PasswordsRetriever();
    List<PasswordSetType> passwordsList = moduleType.getPasswordSets();
    if (passwordsList != null && CollectionUtil.isNonEmpty(passwordsList)) {
      passwordRetriever.setPasswordResolver(passwordResolver);
      for (PasswordSetType passwordType : passwordsList) {
        Set<P11SlotIdFilter> slots = getSlotIdFilters(passwordType.getSlots());
        passwordRetriever.addPasswordEntry(slots, new ArrayList<>(passwordType.getPasswords()));
      }
    }

    includeSlots = getSlotIdFilters(moduleType.getIncludeSlots());
    excludeSlots = getSlotIdFilters(moduleType.getExcludeSlots());

    final String osName = System.getProperty("os.name").toLowerCase();
    String nativeLibraryPath = null;
    for (NativeLibraryType library : moduleType.getNativeLibraries()) {
      List<String> osNames = library.getOperationSystems();
      if (CollectionUtil.isEmpty(osNames)) {
        nativeLibraryPath = library.getPath();
      } else {
        for (String entry : osNames) {
          if (osName.contains(entry.toLowerCase())) {
            nativeLibraryPath = library.getPath();
            break;
          }
        }
      }

      if (nativeLibraryPath != null) {
        break;
      }
    } // end for (NativeLibraryType library)

    if (nativeLibraryPath == null) {
      throw new InvalidConfException("could not find PKCS#11 library for OS " + osName);
    }
    this.nativeLibrary = nativeLibraryPath;

    this.newObjectConf = (moduleType.getNewObjectConf() == null) ? new P11NewObjectConf()
        : new P11NewObjectConf(moduleType.getNewObjectConf());
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getNativeLibrary() {
    return nativeLibrary;
  }

  public int getMaxMessageSize() {
    return maxMessageSize;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public long getUserType() {
    return userType;
  }

  public P11PasswordsRetriever getPasswordRetriever() {
    return passwordRetriever;
  }

  public boolean isSlotIncluded(P11SlotIdentifier slotId) {
    Args.notNull(slotId, "slotId");
    boolean included;
    if (CollectionUtil.isEmpty(includeSlots)) {
      included = true;
    } else {
      included = false;
      for (P11SlotIdFilter entry : includeSlots) {
        if (entry.match(slotId)) {
          included = true;
          break;
        }
      }
    }

    if (!included) {
      return false;
    }

    if (CollectionUtil.isEmpty(excludeSlots)) {
      return included;
    }

    for (P11SlotIdFilter entry : excludeSlots) {
      if (entry.match(slotId)) {
        return false;
      }
    }

    return true;
  }

  public P11MechanismFilter getP11MechanismFilter() {
    return mechanismFilter;
  }

  public P11NewObjectConf getP11NewObjectConf() {
    return newObjectConf;
  }

  private static Set<P11SlotIdFilter> getSlotIdFilters(List<SlotType> slotTypes)
      throws InvalidConfException {
    if (CollectionUtil.isEmpty(slotTypes)) {
      return null;
    }

    Set<P11SlotIdFilter> filters = new HashSet<>();
    for (SlotType slotType : slotTypes) {
      Long slotId = null;
      if (slotType.getId() != null) {
        String str = slotType.getId().trim();
        try {
          slotId = StringUtil.startsWithIgnoreCase(str, "0X")
              ? Long.parseLong(str.substring(2), 16) : Long.parseLong(str);
        } catch (NumberFormatException ex) {
          String message = "invalid slotId '" + str + "'";
          LOG.error(message);
          throw new InvalidConfException(message);
        }
      }
      filters.add(new P11SlotIdFilter(slotType.getIndex(), slotId));
    }

    return filters;
  }

}
