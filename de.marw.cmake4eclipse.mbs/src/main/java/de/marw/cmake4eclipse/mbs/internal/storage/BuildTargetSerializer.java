/*******************************************************************************
 * Copyright (c) 2022 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.internal.storage;

import org.eclipse.cdt.core.settings.model.ICStorageElement;

/**
 * Responsible for serialization/de-serialization of build target objects. Each build target is represented as a String.
 *
 * @author Martin Weber
 */
public class BuildTargetSerializer implements StorageSerializer<String> {
  private static final String ELEM_TARGET = "target";
  private static final String ATTR_NAME = "name";

  @Override
  public void toStorage(ICStorageElement parent, String buildTarget) {
    ICStorageElement elem = parent.createChild(ELEM_TARGET);
    elem.setAttribute(ATTR_NAME, buildTarget);
  }

  @Override
  public String fromStorage(ICStorageElement item) {
    if (!ELEM_TARGET.equals(item.getName()))
      return null; // item is not an element representing a build target
    String nameVal = item.getAttribute(ATTR_NAME);
    return nameVal;
  }
}
