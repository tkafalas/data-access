/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.platform.dataaccess.datasource.permissions.utils;

import java.util.ArrayList;
import java.util.List;

import org.pentaho.platform.dataaccess.datasource.IDatasourceInfo;
import org.pentaho.platform.dataaccess.datasource.ui.service.DSWUIDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.MetadataUIDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.MondrianUIDatasourceService;

public class DatasourcePermissionUtil {
  public static List<DatasourceRepositoryPath> getRepositoryPath( IDatasourceInfo dsInfo ) {
    String type = dsInfo.getType();
    return getRepositoryPath( dsInfo.getName(), dsInfo.getType() );
  }
  
  /**
   * 
   * @param datasourceName
   *          The name of the datasource
   * @param type
   *          The type of the datasource
   * @return the path to the repository node holding the acls for the datasource
   */
  public static List<DatasourceRepositoryPath> getRepositoryPath( String datasourceName, String type ) {
    ArrayList<DatasourceRepositoryPath> repositoryPaths = new ArrayList<DatasourceRepositoryPath>();
    if ( type.equals( "JDBC" ) ) {
      repositoryPaths.add( new DatasourceRepositoryPath( "/etc/pdi/databases/" + datasourceName + ".kdb", type, true ) );
    } else if ( type.equals( MondrianUIDatasourceService.TYPE ) ) {
      repositoryPaths.add( new DatasourceRepositoryPath( "/etc/mondrian/" + datasourceName + "/schema.xml", type, true ) );
      repositoryPaths.add( new DatasourceRepositoryPath( "/etc/mondrian/" + datasourceName + "/metadata", type, false ) );
    } else if ( type.equals( MetadataUIDatasourceService.TYPE ) ) {
      
    } else if ( type.equals( DSWUIDatasourceService.TYPE ) ) {
      
    }
    return repositoryPaths;
  }
  
  static public class DatasourceRepositoryPath {
    String path;
    String type;
    // Is it required that this path exist? Determines whether to throw an error if the acl cannot be set.
    boolean required;

    DatasourceRepositoryPath( String path, String type, boolean required) {
      this.path = path;
      this.type = type;
      this.required = required;
    }

    public String getPath() {
      return path;
    }

    public String getType() {
      return type;
    }

    public boolean isRequired() {
      return required;
    }
    
  }
}
