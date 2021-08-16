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

import de.marw.cmake4eclipse.mbs.settings.CmakeUnDefine;

/**
 * Responsible for serialization/de-serialization of CmakeUndefine objects.
 *
 * @author Martin Weber
 */
public class CMakeUndefineSerializer implements
    StorageSerializer<CmakeUnDefine> {
  /**  */
  private static final String ELEM_UNDEFINE = "undef";
  /**  */
  private static final String ATTR_NAME = "name";

  public void toStorage(ICStorageElement parent, CmakeUnDefine item) {
    ICStorageElement elem = parent.createChild(ELEM_UNDEFINE);
    elem.setAttribute(ATTR_NAME, item.getName());
  }

  /*-
   * @see de.marw.cmake4eclipse.mbs.internal.storage.StorageSerializer#fromStorage(org.eclipse.cdt.core.settings.model.ICStorageElement)
   */
  @Override
  public CmakeUnDefine fromStorage(ICStorageElement item) {
    if (!ELEM_UNDEFINE.equals(item.getName()))
      return null; // item is not an element representing a cmake undefine
    String nameVal = item.getAttribute(ATTR_NAME);
    if (nameVal != null) {
      return new CmakeUnDefine(nameVal);
    }
    return null;
  }
}