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

package org.pentaho.platform.dataaccess.datasource.ui.admindialog;

import java.util.ArrayList;
import java.util.List;

import org.pentaho.agilebi.modeler.services.IModelerServiceAsync;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.filechooser.RepositoryFile;
import org.pentaho.gwt.widgets.client.tabs.PentahoTabPanel;
import org.pentaho.gwt.widgets.client.utils.NameUtils;
import org.pentaho.gwt.widgets.client.utils.i18n.IResourceBundleLoadCallback;
import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.metadata.model.Domain;
import org.pentaho.platform.dataaccess.datasource.DatasourceInfo;
import org.pentaho.platform.dataaccess.datasource.IDatasourceInfo;
import org.pentaho.platform.dataaccess.datasource.beans.LogicalModelSummary;
import org.pentaho.platform.dataaccess.datasource.permissions.FilePropertiesDialog;
import org.pentaho.platform.dataaccess.datasource.permissions.IRepositoryFileCallback;
import org.pentaho.platform.dataaccess.datasource.permissions.IRepositoryFileListCallback;
import org.pentaho.platform.dataaccess.datasource.permissions.messages.Messages;
import org.pentaho.platform.dataaccess.datasource.ui.service.DSWUIDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.IUIDatasourceAdminService;
import org.pentaho.platform.dataaccess.datasource.ui.service.JdbcDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.MetadataUIDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.MondrianUIDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.UIDatasourceServiceManager;
import org.pentaho.platform.dataaccess.datasource.wizard.GwtDatasourceEditorEntryPoint;
import org.pentaho.platform.dataaccess.datasource.wizard.GwtDatasourceMessages;
import org.pentaho.platform.dataaccess.datasource.wizard.service.IXulAsyncDSWDatasourceService;
import org.pentaho.platform.dataaccess.datasource.wizard.service.IXulAsyncDatasourceServiceManager;
import org.pentaho.ui.database.gwt.GwtDatabaseDialog;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.XulServiceCallback;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingConvertor;
import org.pentaho.ui.xul.binding.BindingException;
import org.pentaho.ui.xul.binding.BindingExceptionHandler;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.components.XulButton;
import org.pentaho.ui.xul.components.XulLabel;
import org.pentaho.ui.xul.components.XulMenuitem;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.containers.XulMenupopup;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.containers.XulTreeCols;
import org.pentaho.ui.xul.stereotype.Bindable;
import org.pentaho.ui.xul.util.AbstractXulDialogController;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;

public class DatasourceAdminDialogController extends AbstractXulDialogController<IDatasourceInfo> implements
    BindingExceptionHandler, IResourceBundleLoadCallback {

  // ~ Static fields/initializers ======================================================================================

  // ~ Instance fields =================================================================================================
  private BindingFactory bf;
  
  private IXulAsyncDatasourceServiceManager datasourceServiceManager;
  private IModelerServiceAsync modelerService;
  private IXulAsyncDSWDatasourceService dswService;
  private DatasourceAdminDialogModel datasourceAdminDialogModel = new DatasourceAdminDialogModel();

  private XulDialog datasourceAdminDialog;
  private XulDialog datasourceAdminErrorDialog;
  private XulDialog removeDatasourceConfirmationDialog;
  private XulLabel datasourceAdminErrorLabel = null;
  
  private XulButton datasourceAddButton;
  private XulMenupopup datasourceTypeMenuPopup;
  private XulButton exportDatasourceButton;
  private XulButton editDatasourceButton;
  private XulButton removeDatasourceButton;

  private Binding editDatasourceButtonBinding; 
  private Binding removeDatasourceButtonBinding;
  private Binding exportDatasourceButtonBinding;
  
  private GwtDatabaseDialog databaseDialog;
  private boolean administrator;
  XulTree datasourceTable = null;

  XulTreeCols datasourceTreeCols = null;
  UIDatasourceServiceManager manager;
  private GwtDatasourceEditorEntryPoint entryPoint;
  private DialogListener adminDatasourceListener;
  private GwtDatasourceMessages messageBundle;
  private boolean permissionMessageBundleLoaded;

  /**
   * Sets up bindings.
   */
  @Bindable
  public void init() {
    datasourceTable = (XulTree) document.getElementById("datasourcesListTable"); //$NON-NLS-1$
    datasourceTreeCols = (XulTreeCols) document.getElementById("datasourcesListCols"); //$NON-NLS-1$
    datasourceAdminDialog = (XulDialog) document.getElementById("datasourceAdminDialog"); //$NON-NLS-1$
    datasourceAdminErrorDialog = (XulDialog) document.getElementById("datasourceAdminErrorDialog"); //$NON-NLS-1$
    removeDatasourceConfirmationDialog = (XulDialog) document.getElementById("removeDatasourceConfirmationDialog"); //$NON-NLS-1$
    datasourceAdminErrorLabel = (XulLabel) document.getElementById("datasourceAdminErrorLabel");//$NON-NLS-1$

    datasourceAddButton = (XulButton) document.getElementById("datasourceAddButton"); //$NON-NLS-1$
    datasourceTypeMenuPopup = (XulMenupopup) document.getElementById("datasourceTypeMenuPopup"); //$NON-NLS-1$
    exportDatasourceButton = (XulButton) document.getElementById("exportDatasourceButton"); //$NON-NLS-1$
    editDatasourceButton = (XulButton) document.getElementById("editDatasourceButton"); //$NON-NLS-1$
    removeDatasourceButton = (XulButton) document.getElementById("removeDatasourceButton"); //$NON-NLS-1$
    bf.setBindingType(Binding.Type.ONE_WAY);
    try {
      
      BindingConvertor<IDatasourceInfo, Boolean> removeDatasourceButtonConvertor = new BindingConvertor<IDatasourceInfo, Boolean>() {
        @Override
        public Boolean sourceToTarget(final IDatasourceInfo datasourceInfo) {
          return datasourceInfo.isRemovable();
        }

        @Override
        public IDatasourceInfo targetToSource(final Boolean value) {
          throw new UnsupportedOperationException();
        }
      };
      
      //setup binding to disable edit datasource button until user selects a datasource
      BindingConvertor<IDatasourceInfo, Boolean> editDatasourceButtonConvertor = new BindingConvertor<IDatasourceInfo, Boolean>() {
        @Override
        public Boolean sourceToTarget(final IDatasourceInfo datasourceInfo) {
          return datasourceInfo.isEditable();
        }

        @Override
        public IDatasourceInfo targetToSource(final Boolean value) {
          throw new UnsupportedOperationException();
        }
      };
      
      BindingConvertor<IDatasourceInfo, Boolean> exportDatasourceButtonConvertor = new BindingConvertor<IDatasourceInfo, Boolean>() {
        @Override
        public Boolean sourceToTarget(final IDatasourceInfo datasourceInfo) {
          return datasourceInfo.isExportable();
        }

        @Override
        public IDatasourceInfo targetToSource(final Boolean value) {
          throw new UnsupportedOperationException();
        }
      };
      removeDatasourceButtonBinding = bf.createBinding(datasourceAdminDialogModel, "selectedDatasource", //$NON-NLS-1$
          removeDatasourceButton, "!disabled", removeDatasourceButtonConvertor); //$NON-NLS-1$
      editDatasourceButtonBinding = bf.createBinding(datasourceAdminDialogModel, "selectedDatasource", //$NON-NLS-1$
          editDatasourceButton, "!disabled", editDatasourceButtonConvertor); //$NON-NLS-1$
      exportDatasourceButtonBinding = bf.createBinding(datasourceAdminDialogModel, "selectedDatasource", //$NON-NLS-1$
          exportDatasourceButton, "!disabled", exportDatasourceButtonConvertor); //$NON-NLS-1$

      bf.createBinding(datasourceAdminDialogModel, "datasources", datasourceTable, "elements");
      bf.createBinding(datasourceTable, "selectedItems", datasourceAdminDialogModel, "selectedDatasource", //$NON-NLS-1$//$NON-NLS-2$
      new BindingConvertor<List, IDatasourceInfo>() { 

        @Override
        public IDatasourceInfo sourceToTarget( List datasources) {
          if(datasources != null && datasources.size() > 0) {
            return (IDatasourceInfo) datasources.get(0);
          }
          return null;
        }

        @Override
        public List targetToSource( IDatasourceInfo arg0 ) {
          throw new UnsupportedOperationException();
        }

      }).fireSourceChanged();

      bf.createBinding(datasourceTable, "selectedItem", datasourceAdminDialogModel, "selectedDatasource").fireSourceChanged();
      
      removeDatasourceButtonBinding.fireSourceChanged();
      editDatasourceButtonBinding.fireSourceChanged();
      exportDatasourceButtonBinding.fireSourceChanged();
      setupNativeHooks(this);
      // Initialize UI Datasource Service Manager
      adminDatasourceListener = new DialogListener() {

        @Override
        public void onDialogAccept(Object returnValue) {
          refreshDatasourceList();
        }

        @Override
        public void onDialogCancel() {
          // TODO Auto-generated method stub

        }

        @Override
        public void onDialogReady() {
          // TODO Auto-generated method stub
          
        }

        @Override
        public void onDialogError(String errorMessage) {
          openErrorDialog("Error", errorMessage);
        }
      };
      
      manager = UIDatasourceServiceManager.getInstance();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }
  private native void setupNativeHooks(DatasourceAdminDialogController controller)/*-{
    $wnd.pho.showDatasourceAdminErrorDialog = function(title, errorMessage) {
      controller.@org.pentaho.platform.dataaccess.datasource.ui.admindialog.DatasourceAdminDialogController::openErrorDialog(Ljava/lang/String;Ljava/lang/String;)(title, errorMessage);
    }
    $wnd.pho.refreshDatasourceList = function() {
      controller.@org.pentaho.platform.dataaccess.datasource.ui.admindialog.DatasourceAdminDialogController::refreshDatasourceList()();
    }
    
  }-*/;
  
  /**
   * ID of this controller. This is how event handlers are referenced in <code>.xul</code> files.
   */
  @Override
  public String getName() {
    return "datasourceAdminDialogController"; //$NON-NLS-1$
  }

  public void setBindingFactory(final BindingFactory bf) {
    this.bf = bf;
    this.bf.setExceptionHandler(this);
  }

  public void setDatasourceServiceManager(final IXulAsyncDatasourceServiceManager datasourceServiceManager) {
    this.datasourceServiceManager = datasourceServiceManager;
  }

  public void setModelerService(final IModelerServiceAsync modelerService) {
    this.modelerService = modelerService;
  }

  public void setDSWService(final IXulAsyncDSWDatasourceService dswService) {
    this.dswService = dswService;
  }

  private void refreshDatasourceList() {
    if(messageBundle != null) {
      DatasourceInfo.setMessageBundle(messageBundle);
    }
    datasourceAdminDialogModel.setDatasourcesList(null);
      manager.getIds(new XulServiceCallback<List<IDatasourceInfo>>() {
  
        @Override
        public void success(List<IDatasourceInfo> infoList) {
          DatasourceAdminDialogController.super.showDialog();
          datasourceAdminDialogModel.setDatasourcesList(infoList);
          getDatasourceTypes();
          exportDatasourceButton.setDisabled(true);
          editDatasourceButton.setDisabled(true);
          removeDatasourceButton.setDisabled(true);
        }
  
        @Override
        public void error(String message, Throwable error) {
          openErrorDialog("Error", message + error.getMessage());
        }
      });

      checkPermissionMessageBundle();
  }
  
  private void getDatasourceTypes() {
        List<String> datasourceTypes = manager.getTypes();
        // Clear out the current component list
        List<XulComponent> components = datasourceTypeMenuPopup.getChildNodes();
        for(XulComponent component:components) {
          datasourceTypeMenuPopup.removeComponent(component);
        }

        List<IDatasourceInfo> datasourceInfoList = datasourceAdminDialogModel.getDatasourcesList();

        for(String datasourceType:datasourceTypes) {

          boolean creatable = true;

          IUIDatasourceAdminService datasourceAdminService = manager.getService( datasourceType );

          if(!datasourceAdminService.isCreatable()){
            continue;
          }

          XulMenuitem menuItem;
          try {
            menuItem = (XulMenuitem) document.createElement("menuitem");
            menuItem.setLabel(DatasourceInfo.getDisplayType(datasourceType, messageBundle));
            menuItem.setCommand(getName() + ".launchNewUI(\""+ datasourceType + "\")");
            menuItem.setId(datasourceType);
            datasourceTypeMenuPopup.addChild(menuItem);
          } catch (XulException e) {
            throw new RuntimeException(e);
          }
        }
        datasourceAdminDialogModel.setDatasourceTypeList(datasourceTypes);
  }

  @Bindable
  public void launchNewUI(String datasourceType) {
      IUIDatasourceAdminService service = manager.getService(datasourceType);
      String newUI = service.getNewUI();
      if(newUI != null && newUI.length() > 0) {
        if(newUI.indexOf("builtin:") >= 0) {
          if(service.getType().equals(JdbcDatasourceService.TYPE)) {
            entryPoint.showDatabaseDialog(adminDatasourceListener);
          } else if (service.getType().equals(MondrianUIDatasourceService.TYPE)){
            entryPoint.showAnalysisImportDialog(adminDatasourceListener);
          } else if (service.getType().equals(MetadataUIDatasourceService.TYPE)){
            entryPoint.showMetadataImportDialog(adminDatasourceListener);
          } else if (service.getType().equals(DSWUIDatasourceService.TYPE)){
            entryPoint.showWizard(true, adminDatasourceListener);
          }
        } else if(newUI.indexOf("javascript:") >= 0) {
          String script = newUI.substring(newUI.indexOf(":") + 1);
          executeJavaScript(script);
        }
        		
      }
  }

  private native void executeJavaScript(String script) /*-{
    try{
      var callback = "{ onCancel: function(){} ,onOk: function(returnValue){$wnd.pho.refreshDatasourceList();},onError: function(errorMessage){$wnd.pho.showDatasourceAdminErrorDialog('Error', errorMessage);}}";
      var updatedScript = script.replace(/\{callback\}/g, callback);
      eval(updatedScript);
    } catch (e){
      $wnd.pho.showDatasourceAdminErrorDialog("Error", e.message);
    }
  }-*/;
  
  @Override
  public void showDialog() {
    refreshDatasourceList();
  }

  @Override
  protected XulDialog getDialog() {
    return datasourceAdminDialog;
  }

  @Override
  protected IDatasourceInfo getDialogResult() {
    return datasourceAdminDialogModel.getDatasourcesList().get(datasourceAdminDialogModel.getSelectedIndex());
  }

  @Bindable
  public void openErrorDialog(String title, String message) {
    datasourceAdminErrorDialog.setTitle(title);
    datasourceAdminErrorLabel.setValue(message);
    datasourceAdminErrorDialog.show();
  }

  @Bindable
  public void closeErrorDialog() {
    if (!datasourceAdminErrorDialog.isHidden()) {
      datasourceAdminErrorDialog.hide();
      refreshDatasourceList();
    }
  }
  
  public void setEntryPoint(GwtDatasourceEditorEntryPoint entryPoint) {
    this.entryPoint = entryPoint;
  }
  
  @Override
  public void handleException(BindingException t) {
    t.printStackTrace();
  }
  
  @Bindable
  public void export() {
    IDatasourceInfo dsInfo = datasourceAdminDialogModel.getSelectedDatasource();
    manager.exportDatasource(dsInfo);
  }
  
  @Bindable
  public void remove() {
    removeDatasourceConfirmationDialog.show();
  }
  
  @Bindable
  public void edit() {
    IDatasourceInfo dsInfo = datasourceAdminDialogModel.getSelectedDatasource();
    String type = dsInfo.getType();
    final String dsId = dsInfo.getId();
    if (DSWUIDatasourceService.TYPE.equals(type)) {
      dswService.getLogicalModels(dsId, new XulServiceCallback<List<LogicalModelSummary>>(){

        @Override
        public void success(List<LogicalModelSummary> retVal) {
          for (LogicalModelSummary logicalModelSummary : retVal){
            if (!dsId.equals(logicalModelSummary.getDomainId())) continue;
              entryPoint.showWizardEdit(
                logicalModelSummary.getDomainId(),
                logicalModelSummary.getModelId(),
                false,
                new DialogListener<Domain> (){

                  @Override
                  public void onDialogAccept(
                      Domain returnValue) {
                    // TODO Auto-generated method stub
                    
                  }

                  @Override
                  public void onDialogCancel() {
                    // TODO Auto-generated method stub
                    
                  }

                  @Override
                  public void onDialogReady() {
                    // TODO Auto-generated method stub
                    
                  }

                  @Override
                  public void onDialogError(String errorMessage) {
                    // TODO Auto-generated method stub
                    
                  }
                  
                }
              );          
          }
        }

        @Override
        public void error(String message, Throwable error) {
          // TODO Auto-generated method stub
          
        }
        
      });
    }
    else 
    if (JdbcDatasourceService.TYPE.equals(type)) {
      entryPoint.showEditDatabaseDialog(
        adminDatasourceListener, 
        dsId
      );
    }
    else
    if (MondrianUIDatasourceService.TYPE.equals(type)) {
      IDatasourceInfo datasourceInfo = datasourceAdminDialogModel.getSelectedDatasource();	
      entryPoint.showEditAnalysisDialog(adminDatasourceListener, datasourceInfo);
    }
  }
  
  @Bindable
  public void removeDatasourceAccept() {
    final IDatasourceInfo dsInfo = datasourceAdminDialogModel.getSelectedDatasource();
    manager.remove(dsInfo, new XulServiceCallback<Boolean>() {

       @Override
       public void success(Boolean isOk) {
    	   if (isOk) {
    		   refreshDatasourceList();
               editDatasourceButton.setDisabled(true);
           } else {
        	   Window.alert(
                 messageBundle.getString("datasourceAdminDialogController.COULD_NOT_REMOVE")
        			   + ": " + dsInfo.getId());
           }
        }

        @Override
        public void error(String message, Throwable error) {
        	Window.alert(
              messageBundle.getString("datasourceAdminDialogController.ERROR_REMOVING")
        			+  ": " + dsInfo.getId() + "." 
        					+ messageBundle.getString("ERROR") + "=" + error.getLocalizedMessage());
        }
    });
    removeDatasourceConfirmationDialog.hide();
  }
  
  @Bindable
  public void removeDatasourceCancel() {
    removeDatasourceConfirmationDialog.hide();
  }

  public void setMessageBundle(GwtDatasourceMessages messageBundle) {
    this.messageBundle = messageBundle;
  }
  
  @Bindable
  public void datasourceProperties() {
    IDatasourceInfo dsInfo = datasourceAdminDialogModel.getSelectedDatasource();
    String type = dsInfo.getType();
    
    
    //Get a RepositoryFile object for this datasource
    //RepositoryFile repositoryFile = null;

      getRepositoryFiles( dsInfo.getName(), dsInfo.getType(), new IRepositoryFileListCallback() {
        @Override
        public void repositoryFileListResponse( List<RepositoryFile> repositoryFileList ) {
          if ( repositoryFileList != null && repositoryFileList.size() > 0 ) {
            initiateFilePropertiesDialog( repositoryFileList.get( 0 ) );
          }
        }
      });

    //Get a RepositoryFile object for this datasource
/*    RepositoryFile repositoryFile = null;
    if ( type.equals( "JDBC" ) ) {
      getRepositoryFile( "/etc/pdi/databases/" + dsInfo.getName() + ".kdb", new IRepositoryFileCallback() {
        @Override
        public void repositoryFileReponse( RepositoryFile repositoryFile ) {
          initiateFilePropertiesDialog( repositoryFile );
        }
      });
      
    }*/
  }
  
  protected void initiateFilePropertiesDialog( RepositoryFile repositoryFile ) {
    if ( permissionMessageBundleLoaded ) {
      if ( repositoryFile != null ) {
        FilePropertiesDialog dialog = new FilePropertiesDialog( repositoryFile, new PentahoTabPanel(), null, FilePropertiesDialog.Tabs.GENERAL, true );
        dialog.showTab( FilePropertiesDialog.Tabs.GENERAL ); //PERMISSION
        dialog.center();
        dialog.show();
        dialog.getElement().getStyle().setProperty( "zIndex", "1300" );
    }   
//
//      event.setMessage( e.getMessage() );
//      EventBusUtil.EVENT_BUS.fireEvent( event );

    }
  }
  
  protected void getRepositoryFiles( final String datasourceName, final String datasourceType,
      final IRepositoryFileListCallback repositoryFileListCallback ) {
    String url = GWT.getModuleBaseURL();
    int indexOfContent = url.indexOf( "content" );
    if ( indexOfContent > -1 ) {
      url = url.substring( 0, indexOfContent );
    }
    url = url + "plugin/data-access/api/datasource/" + NameUtils.URLEncode( datasourceName ) + "/getRepositoryFileList?type=" + encodeUri( datasourceType ); //$NON-NLS-1$ //$NON-NLS-2$
    RequestBuilder builder = new RequestBuilder( RequestBuilder.GET, url );
    builder.setHeader( "Accept", "application/json" );
    // This header is required to force Internet Explorer to not cache values from the GET response.
    builder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    
    final StringBuilder outputJsonHolder = new StringBuilder();
    try {
      builder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
              new MessageDialogBox( Messages.getString( "error" ), exception.getLocalizedMessage(), false, false, true ); //$NON-NLS-1$
          dialogBox.center();
        }

        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ) {
            outputJsonHolder.append( response.getText() );
            List<RepositoryFile> fileList = new ArrayList<RepositoryFile>();

            JSONValue jsonValue =
                ( (JSONObject) JSONParser.parseLenient( outputJsonHolder.toString() ) ).get( "repositoryFileDto" );
            if ( jsonValue != null ) {
              if ( jsonValue.isArray() != null ) {
                JSONArray jsonArray = jsonValue.isArray();
                for ( int i = 0; i < jsonArray.size(); i++ ) {
                  JSONObject jso = new JSONObject();
                  jso.put( "repositoryFileDto", jsonArray.get( i ) );
                  fileList.add( new RepositoryFile( jso ) );
                }
              } else {
                JSONObject jso = new JSONObject();
                jso.put( "repositoryFileDto", jsonValue );
                fileList.add(  new RepositoryFile (jso) );
              }
            }
            
            repositoryFileListCallback.repositoryFileListResponse( fileList );
          } else {
            MessageDialogBox dialogBox =
                new MessageDialogBox(
                    Messages.getString( "error" ), Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode(), false, false, true ); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            dialogBox.center();
          }
        }
      } );
    } catch ( RequestException e ) {
      MessageDialogBox dialogBox =
          new MessageDialogBox( Messages.getString( "error" ), e.getLocalizedMessage(), false, false, true ); //$NON-NLS-1$
      dialogBox.center();
    }
  }
  
  //Delete this method.  It is what I used when I was returning a single repositoryFile
  protected void getRepositoryFile( final String path, final IRepositoryFileCallback repositoryFileCallback ) {
    String contextURL = getAjaxBaseUrl();
    String url = contextURL + "api/repo/files/" + pathToId( path ) + "/properties"; //$NON-NLS-1$ //$NON-NLS-2$
    RequestBuilder builder = new RequestBuilder( RequestBuilder.GET, url );
    builder.setHeader( "Accept", "application/json" );
    // This header is required to force Internet Explorer to not cache values from the GET response.
    builder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    
    final StringBuilder outputJsonHolder = new StringBuilder();
    try {
      builder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
              new MessageDialogBox( Messages.getString( "error" ), exception.getLocalizedMessage(), false, false, true ); //$NON-NLS-1$
          dialogBox.center();
        }

        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ) {
            outputJsonHolder.append( response.getText() );
            JSONObject jsonObject = new JSONObject();
            jsonObject.put( "repositoryFileDto", (JSONObject) JSONParser.parseLenient( outputJsonHolder.toString() ) );
            RepositoryFile repositoryFile = new RepositoryFile( jsonObject );
            repositoryFileCallback.repositoryFileReponse( repositoryFile );
          } else {
            MessageDialogBox dialogBox =
                new MessageDialogBox(
                    Messages.getString( "error" ), Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode(), false, false, true ); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            dialogBox.center();
          }
        }
      } );
    } catch ( RequestException e ) {
      MessageDialogBox dialogBox =
          new MessageDialogBox( Messages.getString( "error" ), e.getLocalizedMessage(), false, false, true ); //$NON-NLS-1$
      dialogBox.center();
    }
  }

  @SuppressWarnings( "nls" )
  public static String pathToId( String path ) {
    String id = NameUtils.encodeRepositoryPath( path );
    return NameUtils.URLEncode( id );
  }
  
  public static native String getAjaxBaseUrl()
  /*-{
     return $wnd.location.protocol + '//' + $wnd.location.host + $wnd.CONTEXT_PATH;
  }-*/;
  
  private void checkPermissionMessageBundle() {
    if ( !permissionMessageBundleLoaded ) {
      String moduleBaseURL = GWT.getModuleBaseURL();
      String moduleName = "data-access";
      String contextURL = moduleBaseURL.substring( 0, moduleBaseURL.lastIndexOf( moduleName ) + moduleName.length() ) + "/";
      ResourceBundle permissionsMessageBundle = new ResourceBundle();
      Messages.setResourceBundle( permissionsMessageBundle );
      permissionsMessageBundle.loadBundle( contextURL + "resources/messages/", "Messages", true, DatasourceAdminDialogController.this ); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }
  
  @Override
  public void bundleLoaded( String bundleName ) {
    permissionMessageBundleLoaded = true;
    
  }
  
  private static final native String encodeUri( String URI )
  /*-{
      return encodeURIComponent(URI);
  }-*/;
}
