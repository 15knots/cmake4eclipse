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
class IntelCToolCommandlineParser extends DefaultToolCommandlineParser {

  static final IntelCToolCommandlineParser INSTANCE = new IntelCToolCommandlineParser();

  private IntelCToolCommandlineParser() {
    super("org.eclipse.cdt.core.gcc", new ResponseFileArglets.At(), null, new Arglets.IncludePath_C_POSIX(),
        new Arglets.MacroDefine_C_POSIX(), new Arglets.MacroUndefine_C_POSIX());
  }
}
