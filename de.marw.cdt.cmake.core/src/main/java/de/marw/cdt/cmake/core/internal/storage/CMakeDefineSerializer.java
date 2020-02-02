/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.internal.storage;

import org.eclipse.cdt.core.settings.model.ICStorageElement;

import de.marw.cdt.cmake.core.settings.CmakeDefine;
import de.marw.cdt.cmake.core.settings.CmakeVariableType;

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