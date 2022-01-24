/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal.storage;

import java.util.Collection;

import org.eclipse.cdt.core.settings.model.ICStorageElement;

/**
 * @author Martin Weber
 */
public class Util {

  /**
   * Nothing to instantiate here, just static methods.
   */
  private Util() {
  }

  /**
   * Converts a collection of {@code T} objects to {@link ICStorageElement}s.
   *
   * @param targetCollectionStorageName
   *        the name for the ICStorageElement representing the collection. It
   *        will be created.
   * @param parent
   *        the parent element, must not be {@code null}.
   * @param itemSerializer
   *        the object that converts a collection element.
   * @param source
   *        the collection to convert, must not be {@code null}.
   */
  public static <E> void serializeCollection(
      String targetCollectionStorageName, ICStorageElement parent,
      StorageSerializer<E> itemSerializer, Collection<E> source) {
    ICStorageElement[] existingColls = parent
        .getChildrenByName(targetCollectionStorageName);

    ICStorageElement pColl;
    if (existingColls.length > 0) {
      pColl = existingColls[0];
      if (source.isEmpty()) {
        // remove element if collection is empty
        parent.removeChild(pColl);
        return;
      }
    } else {
      if (source.isEmpty()) {
        return;
      }
      pColl = parent.createChild(targetCollectionStorageName);
    }
    // to avoid duplicates, since we do not track additions/removals to lists..
    pColl.clear();

    // serialize collection elements
    for (E elem : source) {
      itemSerializer.toStorage(pColl, elem);
    }
  }

  /**
   * Converts an {@link ICStorageElement} to a collection of {@code T} objects.
   *
   * @param target
   *        the collection to store the converted objects in, must not be
   *        {@code null}.
   * @param itemSerializer
   *        the object that converts a collection element to an Object.
   * @param sourceParent
   *        the parent element of the collection to read, must not be
   *        {@code null}.
   */
  public static <E> void deserializeCollection(Collection<E> target,
      StorageSerializer<E> itemSerializer, ICStorageElement sourceParent) {
    for (ICStorageElement elem : sourceParent.getChildren()) {
      E item = itemSerializer.fromStorage(elem);
      if (item != null)
        target.add(item);
    }
  }

}
