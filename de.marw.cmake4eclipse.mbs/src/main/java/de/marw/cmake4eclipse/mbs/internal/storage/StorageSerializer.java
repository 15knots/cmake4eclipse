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