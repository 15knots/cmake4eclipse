package de.marw.cmake.cdt.lsp.intel;

import de.marw.cmake.cdt.lsp.Arglets;
import de.marw.cmake.cdt.lsp.DefaultToolCommandlineParser;
import de.marw.cmake.cdt.lsp.IToolCommandlineParser;
import de.marw.cmake.cdt.lsp.ResponseFileArglets;

/**
 * An {@link IToolCommandlineParser} for the Intel C compilers.
 *
 * @author Martin Weber
 */
class IntelCppToolCommandlineParser extends DefaultToolCommandlineParser {

  static final IntelCppToolCommandlineParser INSTANCE= new IntelCppToolCommandlineParser();

  private IntelCppToolCommandlineParser() {
    super("org.eclipse.cdt.core.g++", new ResponseFileArglets.At(), null, new Arglets.IncludePath_C_POSIX(), new Arglets.MacroDefine_C_POSIX(),
        new Arglets.MacroUndefine_C_POSIX());
  }
}
