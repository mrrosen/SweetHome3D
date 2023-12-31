/*
 * FurnitureLibraryUserPreferencesController.java 7 juin 2010
 *
 * Furniture Library Editor, Copyright (c) 2010 Emmanuel PUYBARET / eTeks <info@eteks.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.furniturelibraryeditor.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.model.FurnitureProperty;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.UserPreferencesController;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;

/**
 * A MVC controller for user preferences view.
 * @author Emmanuel Puybaret
 */
public class FurnitureLibraryUserPreferencesController extends UserPreferencesController {
  /**
   * The properties that may be edited by the view associated to this controller.
   */
  public enum Property {DEFAULT_CREATOR, OFFLINE_FURNITURE_LIBRARY, FURNITURE_RESOURCES_LOCAL_DIRECTORY,
      FURNITURE_RESOURCES_REMOTE_URL_BASE, FURNITURE_ID_EDITABLE, CONTENT_MATCHING_FURNITURE_NAME, FURNITURE_NAME_EQUAL_TO_IMPORTED_MODEL_FILE_NAME, FURNITURE_PROPERTIES}

  private final FurnitureLibraryUserPreferences preferences;
  private final PropertyChangeSupport   propertyChangeSupport;

  private String               defaultCreator;
  private boolean              offlineFurnitureLibrary;
  private String               furnitureResourcesLocalDirectory;
  private String               furnitureResourcesRemoteUrlBase;
  private boolean              furnitureIdEditable;
  private boolean              contentMatchingFurnitureName;
  private boolean              furnitureNameEqualToImportedModelFileName;
  private FurnitureProperty [] furnitureProperties;

  public FurnitureLibraryUserPreferencesController(FurnitureLibraryUserPreferences preferences,
                                                   ViewFactory viewFactory,
                                                   ContentManager contentManager) {
    super(preferences, viewFactory, contentManager);
    this.preferences = preferences;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.furnitureProperties = new FurnitureProperty [0];
    updateFurnitureLibraryProperties();
  }

  /**
   * Adds the property change <code>listener</code> in parameter to this controller.
   */
  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  /**
   * Removes the property change <code>listener</code> in parameter from this controller.
   */
  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  /**
   * Updates furniture library preferences properties edited by this controller.
   */
  private void updateFurnitureLibraryProperties() {
    setDefaultCreator(this.preferences.getDefaultCreator());
    setFurnitureLibraryOffline(this.preferences.isFurnitureLibraryOffline());
    setFurnitureResourcesLocalDirectory(this.preferences.getFurnitureResourcesLocalDirectory());
    setFurnitureResourcesRemoteURLBase(this.preferences.getFurnitureResourcesRemoteURLBase());
    setFurnitureIdEditable(this.preferences.isFurnitureIdEditable());
    setContentMatchingFurnitureName(this.preferences.isContentMatchingFurnitureName());
    setFurnitureNameEqualToImportedModelFileName(this.preferences.isFurnitureNameEqualToImportedModelFileName());
    setFurnitureProperties(this.preferences.getFurnitureProperties());
  }

  @Override
  public boolean isPropertyEditable(com.eteks.sweethome3d.viewcontroller.UserPreferencesController.Property property) {
    switch (property) {
      case UNIT :
      case LANGUAGE :
        return true;
      default :
        return false;
    }
  }

  public boolean isPropertyEditable(Property property) {
    return this.preferences.isOnlineFurnitureLibrarySupported()
        || property == Property.DEFAULT_CREATOR
        || property == Property.FURNITURE_ID_EDITABLE
        || property == Property.CONTENT_MATCHING_FURNITURE_NAME
        || property == Property.FURNITURE_NAME_EQUAL_TO_IMPORTED_MODEL_FILE_NAME
        || property == Property.FURNITURE_PROPERTIES;
  }

  /**
   * Returns the creator used by default for imported furniture or <code>null</code>.
   */
  public String getDefaultCreator() {
    return this.defaultCreator;
  }

  /**
   * Sets the creator used by default for imported furniture.
   */
  public void setDefaultCreator(String defaultCreator) {
    if (defaultCreator != this.defaultCreator
        && (defaultCreator == null || !defaultCreator.equals(this.defaultCreator))) {
      String oldDefaultCreator = this.defaultCreator;
      this.defaultCreator = defaultCreator;
      this.propertyChangeSupport.firePropertyChange(Property.DEFAULT_CREATOR.toString(), oldDefaultCreator, defaultCreator);
    }
  }

  /**
   * Returns <code>true</code> if resources needed by the furniture of a library
   * must be included with the library to let it work without connection.
   */
  public boolean isFurnitureLibraryOffline() {
    return this.offlineFurnitureLibrary;
  }

  /**
   * Sets whether resources needed by the furniture of a library
   * must be included with the library to let it work without connection or not.
   */
  public void setFurnitureLibraryOffline(boolean offlineFurnitureLibrary) {
    if (offlineFurnitureLibrary != this.offlineFurnitureLibrary) {
      this.offlineFurnitureLibrary = offlineFurnitureLibrary;
      this.propertyChangeSupport.firePropertyChange(Property.OFFLINE_FURNITURE_LIBRARY.toString(),
          !offlineFurnitureLibrary, offlineFurnitureLibrary);
    }
  }

  /**
   * Returns the local directory where resources needed by the furniture of a library
   * will be saved before being deployed on server.
   */
  public String getFurnitureResourcesLocalDirectory() {
    return this.furnitureResourcesLocalDirectory;
  }

  /**
   * Sets the local directory where resources needed by the furniture of a library
   * will be saved before being deployed on server.
   */
  public void setFurnitureResourcesLocalDirectory(String furnitureResourcesLocalDirectory) {
    if (furnitureResourcesLocalDirectory != this.furnitureResourcesLocalDirectory
        && (furnitureResourcesLocalDirectory == null || !furnitureResourcesLocalDirectory.equals(this.furnitureResourcesLocalDirectory))) {
      String oldValue = this.furnitureResourcesLocalDirectory;
      this.furnitureResourcesLocalDirectory = furnitureResourcesLocalDirectory;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_RESOURCES_LOCAL_DIRECTORY.toString(),
          oldValue, furnitureResourcesLocalDirectory);
    }
  }

  /**
   * Returns the URL base (relative or absolute) used to build the path to resources
   * needed by the furniture of a library.
   */
  public String getFurnitureResourcesRemoteURLBase() {
    return this.furnitureResourcesRemoteUrlBase;
  }

  /**
   * Sets the URL base (relative or absolute) used to build the path to resources
   * needed by the furniture of a library.
   */
  public void setFurnitureResourcesRemoteURLBase(String furnitureResourcesRemoteUrlBase) {
    if (furnitureResourcesRemoteUrlBase != this.furnitureResourcesRemoteUrlBase
        && (furnitureResourcesRemoteUrlBase == null || !furnitureResourcesRemoteUrlBase.equals(this.furnitureResourcesRemoteUrlBase))) {
      Object oldValue = this.furnitureResourcesRemoteUrlBase;
      this.furnitureResourcesRemoteUrlBase = furnitureResourcesRemoteUrlBase;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_RESOURCES_REMOTE_URL_BASE.toString(),
          oldValue, furnitureResourcesRemoteUrlBase);
    }
  }

  /**
   * Returns <code>true</code> if furniture ids are editable.
   */
  public boolean isFurnitureIdEditable() {
    return this.furnitureIdEditable;
  }

  /**
   * Sets whether furniture ids are editable.
   */
  public void setFurnitureIdEditable(boolean furnitureIdEditable) {
    if (furnitureIdEditable != this.furnitureIdEditable) {
      this.furnitureIdEditable = furnitureIdEditable;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_ID_EDITABLE.toString(), !furnitureIdEditable, furnitureIdEditable);
    }
  }

  /**
   * Returns <code>true</code> if the furniture content saved with the library should be named
   * from the furniture name in the default language.
   */
  public boolean isContentMatchingFurnitureName() {
    return this.contentMatchingFurnitureName;
  }

  /**
   * Sets whether the furniture content saved with the library should be named
   * from the furniture name in the default language.
   */
  public void setContentMatchingFurnitureName(boolean contentMatchingFurnitureName) {
    if (contentMatchingFurnitureName != this.contentMatchingFurnitureName) {
      this.contentMatchingFurnitureName = contentMatchingFurnitureName;
      this.propertyChangeSupport.firePropertyChange(Property.CONTENT_MATCHING_FURNITURE_NAME.toString(),
          !contentMatchingFurnitureName, contentMatchingFurnitureName);
    }
  }

  /**
   * Returns <code>true</code> if the furniture name of an imported model be equal
   * to the imported file name.
   */
  public boolean isFurnitureNameEqualToImportedModelFileName() {
    return this.furnitureNameEqualToImportedModelFileName;
  }

  /**
   * Sets if the furniture name of an imported model be equal
   * to the imported file name.
   */
  public void setFurnitureNameEqualToImportedModelFileName(boolean furnitureNameEqualToImportedModelFileName) {
    if (furnitureNameEqualToImportedModelFileName != this.furnitureNameEqualToImportedModelFileName) {
      this.furnitureNameEqualToImportedModelFileName = furnitureNameEqualToImportedModelFileName;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_NAME_EQUAL_TO_IMPORTED_MODEL_FILE_NAME.toString(),
          !furnitureNameEqualToImportedModelFileName, furnitureNameEqualToImportedModelFileName);
    }
  }

  /**
   * Returns the furniture properties which can be edited and displayed.
   */
  public FurnitureProperty [] getFurnitureProperties() {
    return this.furnitureProperties.clone();
  }

  /**
   * Sets the furniture properties which can be edited and displayed.
   */
  public void setFurnitureProperties(FurnitureProperty [] furnitureProperties) {
    if (furnitureProperties != this.furnitureProperties) {
      Object oldFurnitureProperties = this.furnitureProperties.clone();
      this.furnitureProperties = furnitureProperties.clone();
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_PROPERTIES.toString(),
          oldFurnitureProperties, furnitureProperties);
    }
  }

  /**
   * Updates user preferences and saves them.
   */
  @Override
  public void modifyUserPreferences() {
    super.modifyUserPreferences();
    this.preferences.setDefaultCreator(getDefaultCreator());
    if (this.preferences.isOnlineFurnitureLibrarySupported()) {
      this.preferences.setFurnitureLibraryOffline(isFurnitureLibraryOffline());
      this.preferences.setFurnitureResourcesLocalDirectory(getFurnitureResourcesLocalDirectory());
      this.preferences.setFurnitureResourcesRemoteURLBase(getFurnitureResourcesRemoteURLBase());
    }
    this.preferences.setFurnitureIdEditable(isFurnitureIdEditable());
    this.preferences.setContentMatchingFurnitureName(isContentMatchingFurnitureName());
    this.preferences.setFurnitureNameEqualToImportedModelFileName(isFurnitureNameEqualToImportedModelFileName());
    this.preferences.setFurnitureProperties(getFurnitureProperties());
  }
}
