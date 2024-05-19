/*******************************************************************************
 * Copyright (c) 2013 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import de.marw.cmake4eclipse.mbs.settings.ProjectPropsModifiedDateUtil;

/**
 * @author Martin Weber
 */
public class Activator extends Plugin {

  public static final String PLUGIN_ID = "de.marw.cmake4eclipse.mbs"; //$NON-NLS-1$
  /** the managed-build build system ID of cmake4eclipse */
  public static final String CMAKE4ECLIPSE_BUILD_SYSTEM_ID = PLUGIN_ID + ".cmake4eclipse";//$NON-NLS-1$
  /** the MBS project type as defined in plugin.xml */
  public static final String CMAKE4ECLIPSE_PROJECT_TYPE = "cmake4eclipse.mbs.projectType";//$NON-NLS-1$

  // The shared instance.
  private static Activator plugin;
  private TimestampFileTracker listener;

  /**
   * The constructor.
   */
  public Activator() {
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    if (!PLUGIN_ID.equals(this.getBundle().getSymbolicName()))
      throw new RuntimeException("BUG: PLUGIN_ID does not match Bundle-SymbolicName");
    plugin = this;
    listener= new TimestampFileTracker();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
    super.stop(context);
    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static Activator getDefault() {
    return plugin;
  }

  // ////////////////////////////////////////////////////////////////////////////////
  /**
   * Responsible for cleaning up stale files holding the time stamp of the most recent change to the project properties.
   *
   * @author Martin Weber
   */
  private static class TimestampFileTracker implements IResourceChangeListener {
    @Override
    public void resourceChanged(IResourceChangeEvent evt) {
      if (evt.getType() == IResourceChangeEvent.POST_CHANGE) {
//        System.out.println("Resources have changed.");
        try {
          final DeltaVisitor visitor = new DeltaVisitor();
          evt.getDelta().accept(visitor);
          processChanges(visitor.getChanges());
        } catch (CoreException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    private void processChanges(final Map<String, String> changes) {
      Thread thread = new Thread() {
        @Override
        public void run() {
          for (Entry<String, String> entry : changes.entrySet()) {
            String deletedProjectName = entry.getKey();
            String newProjectName = entry.getValue();
            Path oldTs = ProjectPropsModifiedDateUtil.getTimestampedFile(deletedProjectName);
            if (Files.exists(oldTs)) {
              try {
                if (newProjectName == null) {
                  Files.deleteIfExists(oldTs);
                } else {
                  Files.move(oldTs, ProjectPropsModifiedDateUtil.getTimestampedFile(newProjectName));
                }
              } catch (IOException e) {
                // ignore
              }
            }
          }
        }
      };
      thread.start();
    }

    private static class DeltaVisitor implements IResourceDeltaVisitor {
      @Override
      public boolean visit(IResourceDelta delta) {
        IResource res = delta.getResource();
        final int type = res.getType();
        switch (type) {
        case IResource.ROOT:
//        System.out.println("res: " + res);
          return true; // visit the children
        case IResource.PROJECT:
//        System.out.println("res: " + res);
//          printDelta(delta);
          processDelta(delta);
          break;
        }
        return false; // do not visit the children
      }

      private Map<String, String> changes = new HashMap<>();

      private void processDelta(IResourceDelta delta) {
        IResource res = delta.getResource();

        if (delta.getKind() == IResourceDelta.REMOVED) {
          if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
            // relevant for project rename
            // getMovedToPath -> name of new project
            // resource does no longer exist
            changes.put(res.getName(), delta.getMovedToPath().segment(0));
          } else {
            changes.put(res.getName(), null);
          }
        }
      }

      /**
       * @return the changes
       */
      public Map<String, String> getChanges() {
        return changes;
      }

      @SuppressWarnings("unused")
      private void printDelta(IResourceDelta delta) {
        IResource res = delta.getResource();
        int flags = delta.getFlags();

        switch (delta.getKind()) {
        case IResourceDelta.ADDED:
          System.out.print("Resource ");
          System.out.print(res.getFullPath());
          System.out.println(" was added.");
          if ((flags & IResourceDelta.MOVED_FROM) != 0) {
            System.out.print("--> moved from ");
            System.out.println(delta.getMovedFromPath());
            // relevant for project rename
//        getMovedFromPath -> name of old project
//        new project does not yet exist
          }
          break;
        case IResourceDelta.REMOVED:
          System.out.print("Resource ");
          System.out.print(res.getFullPath());
          System.out.println(" was removed.");
          if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
            System.out.print("--> moved to ");
            System.out.println(delta.getMovedToPath());
            // relevant for project rename
//        getMovedToPath -> name of new project
//        old project does no longer exist
          }
          break;
        case IResourceDelta.CHANGED:
          System.out.print("Resource ");
          System.out.print(res.getFullPath());
          System.out.println(" has changed.");
          if ((flags & IResourceDelta.CONTENT) != 0) {
            System.out.println("--> Content Change");
          }
          if ((flags & IResourceDelta.REPLACED) != 0) {
            System.out.println("--> Content Replaced");
          }
          if ((flags & IResourceDelta.MARKERS) != 0) {
            System.out.println("--> Marker Change");
          }
          break;
        }
      }
    }
  }
}
