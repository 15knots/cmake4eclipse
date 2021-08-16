/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal.storage;

import org.eclipse.cdt.core.settings.model.ICStorageElement;

import de.marw.cmake4eclipse.mbs.settings.CmakeDefine;
import de.marw.cmake4eclipse.mbs.settings.CmakeVariableType;

/**
 * Responsible for serialization/de-serialization of CmakeDefine objects.
 *
 * @author Martin Weber
 */
public class CMakeDefineSerializer implements StorageSerializer<CmakeDefine> {
  /**  */
  private static final String ELEM_DEFINE = "def";
  /**  */
  private static final String ATTR_CMAKEVAR_TYPE = "type";
  /**  */
  private static final String ATTR_CMAKEVAR_VALUE = "val";
  /**  */
  private static final String ATTR_NAME = "name";

  public void toStorage(ICStorageElement parent, CmakeDefine item) {
    ICStorageElement elem = parent.createChild(ELEM_DEFINE);
    elem.setAttribute(ATTR_NAME, item.getName());
    elem.setAttribute(ATTR_CMAKEVAR_TYPE, item.getType().name());
    elem.setAttribute(ATTR_CMAKEVAR_VALUE, item.getValue());
  }

  /*-
   * @see StorageSerializer#fromStorage(org.eclipse.cdt.core.settings.model.ICStorageElement)
   */
  @Override
  public CmakeDefine fromStorage(ICStorageElement item) {
    if (!ELEM_DEFINE.equals(item.getName()))
      return null; // item is not an element representing a cmake define
    String nameVal = item.getAttribute(ATTR_NAME);
    String typeVal = item.getAttribute(ATTR_CMAKEVAR_TYPE);
    String valueVal = item.getAttribute(ATTR_CMAKEVAR_VALUE);
    if (nameVal != null && typeVal != null && valueVal != null) {
      try {
        final CmakeVariableType type = CmakeVariableType.valueOf(typeVal);
        return new CmakeDefine(nameVal, type, valueVal);
      } catch (IllegalArgumentException ex) {
        // illegal cmake variable type, ignore
      }
    }
    return null;
  }
}