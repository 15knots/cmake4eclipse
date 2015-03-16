/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - initial implementation
 *******************************************************************************/
package de.marw.cmake;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Martin Weber
 */
public class CMakePlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "de.marw.cmake"; //$NON-NLS-1$
  /**
   * name of the session property attached to {@code CMakeCache.txt} file
   * resources. The property caches the parsed content of the CMake cache file (
   * {@code CMakeCache.txt})
   */
  public static QualifiedName CMAKECACHE_PARSED_PROP = new QualifiedName(
      CMakePlugin.PLUGIN_ID, "parsed-CMakeCache.txt");

  //The shared instance.
  private static CMakePlugin plugin;
  private IResourceChangeListener cacheCleaner;

  /**
   * The constructor.
   */
  public CMakePlugin() {
  }

  /**
   * This method is called upon plug-in activation
   */
  public void start(BundleContext context) throws Exception {
    super.start(context);
    if (!PLUGIN_ID.equals(this.getBundle().getSymbolicName()))
      throw new RuntimeException(
          "BUG: PLUGIN_ID does not match Bundle-SymbolicName");
    plugin = this;
    cacheCleaner = new CacheCleaner();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(cacheCleaner,
        IResourceChangeEvent.PRE_BUILD);
  }

  /**
   * This method is called when the plug-in is stopped
   */
  public void stop(BundleContext context) throws Exception {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(cacheCleaner);
    super.stop(context);
    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static CMakePlugin getDefault() {
    return plugin;
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not
   * found.
   *
   * @param key
   *        the message key
   * @return the resource bundle message
   */
  public static String getResourceString(String key) {
//		ResourceBundle bundle = CMakePlugin.getDefault().getResourceBundle();
//		try {
//			return bundle.getString(key);
//		} catch (MissingResourceException e) {
//			return key;
//		}
    return key;
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not
   * found.
   *
   * @param key
   *        the message key
   * @param args
   *        an array of substitution strings
   * @return the resource bundle message
   */
  public static String getFormattedString(String key, String[] args) {
    return MessageFormat.format(getResourceString(key), (Object[]) args);
  }

  /**
   * Invalidates the parsed content of a CMake cache file when the user edits
   * and saves the file.
   *
   * @author Martin Weber
   */
  private static class CacheCleaner implements IResourceChangeListener {
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      if (event == null || event.getDelta() == null) {
        return;
      }

      try {
        event.getDelta().accept(new IResourceDeltaVisitor() {
          public boolean visit(final IResourceDelta delta) throws CoreException {
            IResource resource = delta.getResource();
            if (((resource.getType() & IResource.FILE) != 0)
                && (delta.getKind() & (IResourceDelta.CHANGED )) != 0
                && "CMakeCache.txt".equals(resource.getName())
            ) {
              System.out.println("del parsed cache; " + delta.getKind()
                  + ", file " + resource.getFullPath());
              resource.setSessionProperty(CMAKECACHE_PARSED_PROP, null);
              return false;
            }
            return true;
          }
        });
      } catch (CoreException ex) {
        // TODO Auto-generated catch block
        ex.printStackTrace();
      }
    }
  }

}
