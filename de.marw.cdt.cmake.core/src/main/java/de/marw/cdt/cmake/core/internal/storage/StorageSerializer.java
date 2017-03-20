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

/**
 * Responsible for serialization/de-serialization of objects to/from
 * {@link ICStorageElement}s.
 *
 * @author Martin Weber
 * @param <T>
 *        the type of the object to serialize/de-serialize
 */
public interface StorageSerializer<T> {
  /**
   * Converts a {@code T} object to an {@link ICStorageElement}.
   *
   * @param parent
   *        the parent storage lement, must not be {@code null}.
   * @param item
   *        the object to convert, must not be {@code null}.
   */
  void toStorage(ICStorageElement parent, T item);

  /**
   * Converts an {@link ICStorageElement} to a {@code T} object.
   *
   * @param item
   *        the storage element for the object to read, must not be {@code null}
   *        .
   * @return the object, or {@code null} if the storage element could not be
   *         converted.
   */
  T fromStorage(ICStorageElement item);
}