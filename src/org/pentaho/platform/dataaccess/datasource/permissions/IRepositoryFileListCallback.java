package org.pentaho.platform.dataaccess.datasource.permissions;
import java.util.List;

import org.pentaho.gwt.widgets.client.filechooser.RepositoryFile;

/**
 * @author tkafalas
 */
public interface IRepositoryFileListCallback {
  void repositoryFileListResponse( List<RepositoryFile> repositoryFile );
}
