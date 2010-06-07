/*
 * FurnitureController.java 
 *
 * Copyright (c) 2009 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.sweethome3d.model.CatalogDoorOrWindow;
import com.eteks.sweethome3d.model.CatalogLight;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.FurnitureCatalog;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Sash;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * A MVC controller for home furniture view.
 * @author Emmanuel Puybaret
 */
public class FurnitureController implements Controller {
  /**
   * The properties that may be edited by the view associated to this controller. 
   */
  public enum Property {ID, NAME, DESCRIPTION, CATEGORY, MODEL, ICON, 
      WIDTH, DEPTH,  HEIGHT, ELEVATION, MOVABLE, RESIZABLE, DOOR_OR_WINDOW, MODEL_ROTATION, CREATOR, 
      PROPORTIONAL, BACK_FACE_SHOWN, PRICE, VALUE_ADDED_TAX_PERCENTAGE}
  
  private static final Map<String, Property> PROPERTIES_MAP = new HashMap<String, Property>();
  
  static {
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_ID_PROPERTY, Property.ID);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_NAME_PROPERTY, Property.NAME);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY, Property.DESCRIPTION);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, Property.CATEGORY);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_CREATOR_PROPERTY, Property.CREATOR);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_ICON_PROPERTY, Property.ICON);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_WIDTH_PROPERTY, Property.WIDTH);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_DEPTH_PROPERTY, Property.DEPTH);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_HEIGHT_PROPERTY, Property.HEIGHT);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_ELEVATION_PROPERTY, Property.ELEVATION);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_MOVABLE_PROPERTY, Property.MOVABLE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_RESIZABLE_PROPERTY, Property.RESIZABLE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_PROPERTY, Property.DOOR_OR_WINDOW);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_MODEL_ROTATION_PROPERTY, Property.MODEL_ROTATION);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_PRICE_PROPERTY, Property.PRICE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_VALUE_ADDED_TAX_PERCENTAGE_PROPERTY, Property.VALUE_ADDED_TAX_PERCENTAGE);
  }
  
  private final FurnitureLibrary                furnitureLibrary;
  private final List<CatalogPieceOfFurniture>   modifiedFurniture;
  private final Set<Property>                   editableProperties;
  private final FurnitureLibraryUserPreferences preferences;
  private final FurnitureLanguageController     furnitureLanguageController;
  private final EditorViewFactory               viewFactory;
  private final PropertyChangeSupport           propertyChangeSupport;
  private DialogView                            homeFurnitureView;

  private String            id;
  private String            name;
  private String            description;
  private FurnitureCategory category;
  private Content           model;
  private Content           icon;
  private Float             width;
  private Float             depth;
  private Float             height;
  private Float             elevation;
  private Boolean           movable;
  private Boolean           doorOrWindow;
  private Boolean           backFaceShown;
  private Boolean           resizable;
  private float [][]        modelRotation;
  private String            creator;
  private BigDecimal        price;
  private BigDecimal        valueAddedTaxPercentage;
  
  private boolean           proportional;

  private PropertyChangeListener widthChangeListener;
  private PropertyChangeListener depthChangeListener;
  private PropertyChangeListener heightChangeListener;
  
  /**
   * Creates the controller of catalog furniture view.
   */
  public FurnitureController(FurnitureLibrary furnitureLibrary, 
                             List<CatalogPieceOfFurniture> modifiedFurniture,
                             FurnitureLibraryUserPreferences preferences, 
                             FurnitureLanguageController furnitureLanguageController,
                             EditorViewFactory viewFactory) {
    this.furnitureLibrary = furnitureLibrary;
    this.modifiedFurniture = modifiedFurniture;
    this.preferences = preferences;
    this.furnitureLanguageController = furnitureLanguageController;
    this.viewFactory = viewFactory;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    this.editableProperties = new HashSet<Property>();
    for (String editedProperty : preferences.getEditedProperties()) {
      this.editableProperties.add(PROPERTIES_MAP.get(editedProperty));
    }

    updateProperties();
    addListeners();
  }

  /**
   * Returns the view associated with this controller.
   */
  public DialogView getView() {
    // Create view lazily only once it's needed
    if (this.homeFurnitureView == null) {
      this.homeFurnitureView = this.viewFactory.createFurnitureView(this.preferences, this); 
    }
    return this.homeFurnitureView;
  }
  
  /**
   * Displays the view controlled by this controller.
   */
  public void displayView(View parentView) {
    getView().displayView(parentView);
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
   * Adds listeners to automatically update lengths when proportional check box is checked.
   */
  private void addListeners() {
    this.widthChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (isProportional()) {
            removePropertyChangeListener(Property.DEPTH, depthChangeListener);
            removePropertyChangeListener(Property.HEIGHT, heightChangeListener);
            
            // If proportions should be kept, update depth and height
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setDepth(getDepth() * ratio); 
            setHeight(getHeight() * ratio);
            
            addPropertyChangeListener(Property.DEPTH, depthChangeListener);
            addPropertyChangeListener(Property.HEIGHT, heightChangeListener);
          }
        }
      };
    this.depthChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (isProportional()) {
            removePropertyChangeListener(Property.WIDTH, widthChangeListener);
            removePropertyChangeListener(Property.HEIGHT, heightChangeListener);
            
            // If proportions should be kept, update width and height
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setWidth(getWidth() * ratio); 
            setHeight(getHeight() * ratio);
            
            addPropertyChangeListener(Property.WIDTH, widthChangeListener);
            addPropertyChangeListener(Property.HEIGHT, heightChangeListener);
          }
        }
      };
    this.heightChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (isProportional()) {
            removePropertyChangeListener(Property.WIDTH, widthChangeListener);
            removePropertyChangeListener(Property.DEPTH, depthChangeListener);
            
            // If proportions should be kept, update width and depth
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setWidth(getWidth() * ratio); 
            setDepth(getDepth() * ratio);
            
            addPropertyChangeListener(Property.WIDTH, widthChangeListener);
            addPropertyChangeListener(Property.DEPTH, depthChangeListener);
          }
        }
      };

    addPropertyChangeListener(Property.WIDTH, this.widthChangeListener);
    addPropertyChangeListener(Property.DEPTH, this.depthChangeListener);
    addPropertyChangeListener(Property.HEIGHT, this.heightChangeListener);
  }

  /**
   * Returns <code>true</code> if the given <code>property</code> is editable.
   * Depending on whether a property is editable or not, the view associated to this controller
   * may render it differently.
   */
  public boolean isPropertyEditable(Property property) {
    if (this.modifiedFurniture.size() == 1) {
      return this.editableProperties.contains(property);
    } else {
      return this.editableProperties.contains(property)
          && property != Property.ID
          && property != Property.ICON;
    }
  }
  
  /**
   * Updates edited properties from selected furniture in the home edited by this controller.
   */
  protected void updateProperties() {
    if (this.modifiedFurniture.isEmpty()) {
      setId(null); // Nothing to edit
      setName(null);
      setDescription(null);
      setCategory(null);
      setModel(null);
      setIcon(null);
      setWidth(null);
      setDepth(null);
      setHeight(null);
      setElevation(null);
      setMovable(null);
      setDoorOrWindow(null);
      setBackFaceShown(null);
      setResizable(null);
      setModelRotation(null);
      setCreator(null);
      setPrice(null);
      setValueAddedTaxPercentage(null);
      this.editableProperties.remove(Property.PROPORTIONAL);
    } else {
      CatalogPieceOfFurniture firstPiece = this.modifiedFurniture.get(0);
      
      setBackFaceShown(false);
      if (this.modifiedFurniture.size() == 1) {
        setIcon(firstPiece.getIcon());
        setModel(firstPiece.getModel());
        this.editableProperties.add(Property.BACK_FACE_SHOWN);
      } else {
        setIcon(null);
        setModel(null);
        this.editableProperties.remove(Property.BACK_FACE_SHOWN);
      }
      
      // Search the common properties among selected furniture
      String id = firstPiece.getId();
      if (id != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!id.equals(this.modifiedFurniture.get(i).getId())) {
            id = null;
            break;
          }
        }
      }
      setId(id);
      
      String furnitureLanguage = this.furnitureLanguageController.getFurnitureLangauge();
      String name = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          firstPiece, furnitureLanguage, FurnitureLibrary.FURNITURE_NAME_PROPERTY, firstPiece.getName());
      if (name != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (!name.equals(this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, furnitureLanguage, FurnitureLibrary.FURNITURE_NAME_PROPERTY, piece.getName()))) {
            name = null;
            break;
          }
        }
      }
      setName(name);
      
      String description = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          firstPiece, furnitureLanguage, FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY, firstPiece.getDescription());
      if (description != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (!description.equals(this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, furnitureLanguage, FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY, piece.getDescription()))) {
            description = null;
            break;
          }
        }
      }
      setDescription(description);
      
      FurnitureCategory category = firstPiece.getCategory();
      String categoryName = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          firstPiece, furnitureLanguage, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, category.getName());
      if (category != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (!categoryName.equals(this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, furnitureLanguage, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, piece.getCategory().getName()))) {
            category = null;
            break;
          }
        }
      }
      setCategory(new FurnitureCategory(categoryName));

      Float width = firstPiece.getWidth();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (width.floatValue() != this.modifiedFurniture.get(i).getWidth()) {
          width = null;
          break;
        }
      }
      setWidth(width);

      Float depth = firstPiece.getDepth();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (depth.floatValue() != this.modifiedFurniture.get(i).getDepth()) {
          depth = null;
          break;
        }
      }
      setDepth(depth);

      Float height = firstPiece.getHeight();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (height.floatValue() != this.modifiedFurniture.get(i).getHeight()) {
          height = null;
          break;
        }
      }
      setHeight(height);

      Float elevation = firstPiece.getElevation();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (elevation.floatValue() != this.modifiedFurniture.get(i).getElevation()) {
          elevation = null;
          break;
        }
      }
      setElevation(elevation);

      Boolean movable = firstPiece.isMovable();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (movable != this.modifiedFurniture.get(i).isMovable()) {
          movable = null;
          break;
        }
      }
      setMovable(movable);           

      Boolean resizable = firstPiece.isResizable();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (resizable.booleanValue() != this.modifiedFurniture.get(i).isResizable()) {
          resizable = null;
          break;
        }
      }
      setResizable(resizable);

      Boolean doorOrWindow = firstPiece.isDoorOrWindow();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (doorOrWindow != this.modifiedFurniture.get(i).isDoorOrWindow()) {
          doorOrWindow = null;
          break;
        }
      }
      setDoorOrWindow(doorOrWindow);           

      float [][] modelRotation = firstPiece.getModelRotation();
      if (modelRotation != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!Arrays.deepEquals(modelRotation, this.modifiedFurniture.get(i).getModelRotation())) {
            modelRotation = null;
            break;
          }
        }
      }
      setModelRotation(modelRotation);

      String creator = firstPiece.getCreator();
      if (creator != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!creator.equals(this.modifiedFurniture.get(i).getCreator())) {
            creator = null;
            break;
          }
        }
      }
      setCreator(creator);

      BigDecimal price = firstPiece.getPrice();
      if (price != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!price.equals(this.modifiedFurniture.get(i).getPrice())) {
            price = null;
            break;
          }
        }
      }
      setPrice(price);

      BigDecimal valueAddedTaxPercentage = firstPiece.getValueAddedTaxPercentage();
      if (valueAddedTaxPercentage != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!valueAddedTaxPercentage.equals(this.modifiedFurniture.get(i).getValueAddedTaxPercentage())) {
            valueAddedTaxPercentage = null;
            break;
          }
        }
      }
      setValueAddedTaxPercentage(valueAddedTaxPercentage);
      
      if (this.editableProperties.contains(Property.WIDTH)
          && this.editableProperties.contains(Property.DEPTH)
          && this.editableProperties.contains(Property.HEIGHT)
          && getWidth() != null
          && getDepth() != null
          && getHeight() != null) {
        this.editableProperties.add(Property.PROPORTIONAL);
        setProportional(false);
      }
    }
  }  
  
  /**
   * Sets the edited id.
   */
  public void setId(String id) {
    if (id != this.id) {
      String oldId = this.id;
      this.id = id;
      this.propertyChangeSupport.firePropertyChange(Property.ID.name(), oldId, id);
    }
  }

  /**
   * Returns the edited id.
   */
  public String getId() {
    return this.id;
  }
  
  /**
   * Sets the edited name.
   */
  public void setName(String name) {
    if (name != this.name) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
    }
  }

  /**
   * Returns the edited name.
   */
  public String getName() {
    return this.name;
  }
  
  /**
   * Sets the edited description.
   */
  public void setDescription(String description) {
    if (description != this.description) {
      String oldDescription = this.description;
      this.description = description;
      this.propertyChangeSupport.firePropertyChange(Property.DESCRIPTION.name(), oldDescription, description);
    }
  }

  /**
   * Returns the edited description.
   */
  public String getDescription() {
    return this.description;
  }
  
  /**
   * Sets the edited category.
   */
  public void setCategory(FurnitureCategory category) {
    if (category != this.category) {
      FurnitureCategory oldCategory = this.category;
      this.category = category;
      this.propertyChangeSupport.firePropertyChange(Property.CATEGORY.name(), oldCategory, category);
    }
  }

  /**
   * Returns the edited category.
   */
  public FurnitureCategory getCategory() {
    return this.category;
  }
  
  /**
   * Returns the list of available categories in furniture library sorted in alphabetical order.
   */
  public List<FurnitureCategory> getAvailableCategories() {
    String furnitureLanguage = this.furnitureLanguageController.getFurnitureLangauge();
    Set<FurnitureCategory> categories = new TreeSet<FurnitureCategory>(getDefaultCategories(furnitureLanguage));
    for (CatalogPieceOfFurniture piece : this.furnitureLibrary.getFurniture()) {
      String categoryName = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          piece, furnitureLanguage, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, piece.getCategory().getName());
      categories.add(new FurnitureCategory(categoryName));
    }
    return new ArrayList<FurnitureCategory>(categories);
  }

  /**
   * Returns the list of available categories in furniture library in the given language.
   */
  public List<FurnitureCategory> getDefaultCategories(String language) {
    Locale locale;
    int underscoreIndex = language.indexOf('_');
    if (underscoreIndex != -1) {
      locale = new Locale(language.substring(0, underscoreIndex), language.substring(underscoreIndex + 1));
    } else {
      locale = new Locale(language.length() == 0
          ? this.preferences.getFurnitureDefaultLanguage()
          : language);
    }
    ResourceBundle resource = ResourceBundle.getBundle(
        "com.eteks.furniturelibraryeditor.viewcontroller.DefaultCategories", locale);
    List<FurnitureCategory> categories = new ArrayList<FurnitureCategory>();
    int i = 1;
    try {
      while (true) {
        categories.add(new FurnitureCategory(resource.getString("defaultFurnitureCategory#" + i++)));
      }
    } catch (MissingResourceException ex) {
      // Stop searching for next category
    }
    return categories;
  }

  /**
   * Sets the edited icon.
   */
  public void setIcon(Content icon) {
    if (icon != this.icon) {
      Content oldIcon = this.icon;
      this.icon = icon;
      this.propertyChangeSupport.firePropertyChange(Property.ICON.name(), oldIcon, icon);
    }
  }

  /**
   * Returns the edited icon.
   */
  public Content getIcon() {
    return this.icon;
  }

  /**
   * Sets the edited model.
   */
  public void setModel(Content model) {
    if (model != this.model) {
      Content oldModel = this.model;
      this.model = model;
      this.propertyChangeSupport.firePropertyChange(Property.MODEL.name(), oldModel, model);
    }
  }
  
  /**
   * Returns the model of the edited piece of furniture.
   */
  public Content getModel() {
    return this.model;
  }

  /**
   * Sets the edited width.
   */
  public void setWidth(Float width) {
    if (width != this.width) {
      Float oldWidth = this.width;
      this.width = width;
      this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, width);
    }
  }

  /**
   * Returns the edited width.
   */
  public Float getWidth() {
    return this.width;
  }
  
  /**
   * Sets the edited depth.
   */
  public void setDepth(Float depth) {
    if (depth != this.depth) {
      Float oldDepth = this.depth;
      this.depth = depth;
      this.propertyChangeSupport.firePropertyChange(Property.DEPTH.name(), oldDepth, depth);
    }
  }

  /**
   * Returns the edited depth.
   */
  public Float getDepth() {
    return this.depth;
  }
  
  /**
   * Sets the edited height.
   */
  public void setHeight(Float height) {
    if (height != this.height) {
      Float oldHeight = this.height;
      this.height = height;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, height);
    }
  }

  /**
   * Returns the edited height.
   */
  public Float getHeight() {
    return this.height;
  }
  
  /**
   * Returns <code>true</code> if piece proportions should be kept.
   */
  public boolean isProportional() {
    return this.proportional;
  }
  
  /**
   * Sets whether piece proportions should be kept or not.
   */
  public void setProportional(boolean proportional) {
    if (proportional != this.proportional) {
      this.proportional = proportional;
      this.propertyChangeSupport.firePropertyChange(Property.PROPORTIONAL.name(), !proportional, proportional);
    }
  }

  /**
   * Multiplies width, depth and height by the given <code>factor</code>.
   */
  public void multiplySize(float factor) {
    if (isProportional()) {
      setWidth(getWidth() * factor);
    } else {
      setProportional(true);
      setWidth(getWidth() * factor);
      setProportional(false);
    }
  }
  
  /**
   * Sets the edited elevation.
   */
  public void setElevation(Float elevation) {
    if (elevation != this.elevation) {
      Float oldElevation = this.elevation;
      this.elevation = elevation;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION.name(), oldElevation, elevation);
    }
  }

  /**
   * Returns the edited elevation.
   */
  public Float getElevation() {
    return this.elevation;
  }
  
  /**
   * Sets whether furniture is movable or not.
   */
  public void setMovable(Boolean movable) {
    if (movable != this.movable) {
      Boolean oldVisible = this.movable;
      this.movable = movable;
      this.propertyChangeSupport.firePropertyChange(Property.MOVABLE.name(), oldVisible, movable);
    }
  }

  /**
   * Returns whether furniture is movable or not.
   */
  public Boolean getMovable() {
    return this.movable;
  }

  /**
   * Sets whether furniture model is a door or a window.
   */
  public void setDoorOrWindow(Boolean doorOrWindow) {
    if (doorOrWindow != this.doorOrWindow) {
      Boolean oldDoorOrWindow = this.doorOrWindow;
      this.doorOrWindow = doorOrWindow;
      this.propertyChangeSupport.firePropertyChange(Property.DOOR_OR_WINDOW.name(), oldDoorOrWindow, doorOrWindow);
    }
  }

  /**
   * Returns whether furniture model is a door or a window.
   */
  public Boolean getDoorOrWindow() {
    return this.doorOrWindow;
  }
   
  /**
   * Sets whether the back face of the furniture model should be shown or not.
   */
  public void setBackFaceShown(Boolean backFaceShown) {
    if (backFaceShown != this.backFaceShown) {
      Boolean oldBackFaceShown = this.backFaceShown;
      this.backFaceShown = backFaceShown;
      this.propertyChangeSupport.firePropertyChange(Property.BACK_FACE_SHOWN.name(), oldBackFaceShown, backFaceShown);
    }
  }
  
  /**
   * Returns whether the back face of the furniture model should be shown or not.
   */
  public Boolean getBackFaceShown() {
    return this.backFaceShown;
  }
  
  /**
   * Sets whether furniture model can be resized or not.
   */
  public void setResizable(Boolean resizable) {
    if (resizable != this.resizable) {
      Boolean oldResizable = this.resizable;
      this.resizable = resizable;
      this.propertyChangeSupport.firePropertyChange(Property.RESIZABLE.name(), oldResizable, resizable);
    }
  }
  
  /**
   * Returns whether furniture model can be resized or not.
   */
  public Boolean getResizable() {
    return this.resizable;
  }
  
  /**
   * Sets the edited model rotation.
   */
  public void setModelRotation(float [][] modelRotation) {
    if (modelRotation != this.modelRotation) {
      float [][] oldModelRotation = this.modelRotation;
      this.modelRotation = modelRotation;
      this.propertyChangeSupport.firePropertyChange(Property.MODEL_ROTATION.name(), oldModelRotation, modelRotation);
    }
  }

  /**
   * Returns the edited model rotation.
   */
  public float [][] getModelRotation() {
    return this.modelRotation;
  }
  
  /**
   * Sets the edited creator.
   */
  public void setCreator(String creator) {
    if (creator != this.creator) {
      String oldCreator = this.creator;
      this.creator = creator;
      this.propertyChangeSupport.firePropertyChange(Property.CREATOR.name(), oldCreator, creator);
    }
  }

  /**
   * Returns the edited creator.
   */
  public String getCreator() {
    return this.creator;
  }
  
  /**
   * Sets the edited price.
   */
  public void setPrice(BigDecimal price) {
    if (price != this.price) {
      BigDecimal oldPrice = this.price;
      this.price = price;
      this.propertyChangeSupport.firePropertyChange(Property.ICON.name(), oldPrice, price);
    }
  }

  /**
   * Returns the edited price.
   */
  public BigDecimal getPrice() {
    return this.price;
  }
  
  /**
   * Sets the edited value added tax percentage.
   */
  public void setValueAddedTaxPercentage(BigDecimal valueAddedTaxPercentage) {
    if (valueAddedTaxPercentage != this.valueAddedTaxPercentage) {
      BigDecimal oldValueAddedTaxPercentage = this.valueAddedTaxPercentage;
      this.valueAddedTaxPercentage = valueAddedTaxPercentage;
      this.propertyChangeSupport.firePropertyChange(Property.VALUE_ADDED_TAX_PERCENTAGE.name(), oldValueAddedTaxPercentage, valueAddedTaxPercentage);
    }
  }

  /**
   * Returns the edited value added tax percentage.
   */
  public BigDecimal getValueAddedTaxPercentage() {
    return this.valueAddedTaxPercentage;
  }

  /**
   * Controls the modification of selected furniture in the edited home.
   */
  public void modifyFurniture() {
    if (!this.modifiedFurniture.isEmpty()) {
      String id = getId(); 
      String name = getName();
      String description = getDescription();
      FurnitureCategory category = getCategory();
      Content model = getModel();
      Content icon = getIcon();
      Float width = getWidth();
      Float depth = getDepth();
      Float height = getHeight();
      Float elevation = getElevation();
      Boolean movable = getMovable();
      Boolean resizable = getResizable();
      Boolean doorOrWindow = getDoorOrWindow();
      float [][] modelRotation = getModelRotation();
      String creator = getCreator();
      BigDecimal price = getPrice();
      BigDecimal valueAddedTaxPercentage = getValueAddedTaxPercentage();
      boolean defaultFurnitureLanguage = FurnitureLibrary.DEFAULT_LANGUAGE.equals(this.furnitureLanguageController.getFurnitureLangauge());
      
      // Apply modification
      List<CatalogPieceOfFurniture> furniture = this.furnitureLibrary.getFurniture();
      for (CatalogPieceOfFurniture piece : this.modifiedFurniture) {
        int index = furniture.indexOf(piece);
        // Retrieve localized data
        Map<String, Object> localizedNames = new HashMap<String, Object>();
        Map<String, Object> localizedDescriptions = new HashMap<String, Object>();
        Map<String, Object> localizedCategories = new HashMap<String, Object>();
        for (String language : this.furnitureLibrary.getSupportedLanguages()) {
          localizedNames.put(language, this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, language, FurnitureLibrary.FURNITURE_NAME_PROPERTY));
          localizedDescriptions.put(language, this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, language, FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY));
          localizedCategories.put(language, this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, language, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY));
          
        }
        this.furnitureLibrary.deletePieceOfFurniture(piece);
        
        String pieceId = id != null ? id : piece.getId();
        String pieceName = name != null && defaultFurnitureLanguage ? name : piece.getName();
        String pieceDescription = description != null && defaultFurnitureLanguage ? description : piece.getDescription();
        FurnitureCategory pieceCategory = category != null && defaultFurnitureLanguage ? category : piece.getCategory();
        Content pieceModel = model != null ? model : piece.getModel();
        Content pieceIcon = icon != null ? icon : piece.getIcon();
        float pieceWidth = width != null ? width : piece.getWidth();
        float pieceDepth = depth != null ? depth : piece.getDepth();
        float pieceHeight = height != null ? height : piece.getHeight();
        float pieceElevation = elevation != null ? elevation : piece.getElevation();
        boolean pieceMovable = movable != null ? movable : piece.isMovable();
        float [][] pieceModelRotation = modelRotation != null ? modelRotation : piece.getModelRotation();
        String pieceCreator = creator != null ? creator : piece.getCreator();
        boolean pieceResizable = resizable != null ? resizable : piece.isResizable();
        BigDecimal piecePrice = price != null ? price : piece.getPrice();
        BigDecimal pieceValueAddedTaxPercentage = valueAddedTaxPercentage != null 
            ? valueAddedTaxPercentage : piece.getValueAddedTaxPercentage();
        
        if (piece instanceof CatalogDoorOrWindow) {
          CatalogDoorOrWindow opening = (CatalogDoorOrWindow)piece;
          piece = new CatalogDoorOrWindow(pieceId, pieceName, pieceDescription, 
              pieceIcon, opening.getPlanIcon(), pieceModel,
              pieceWidth, pieceDepth, pieceHeight, pieceElevation, pieceMovable, 
              opening.getWallThickness(), opening.getWallDistance(), opening.getSashes(), 
              pieceModelRotation, pieceCreator, pieceResizable, 
              piecePrice, pieceValueAddedTaxPercentage);
        } else if (piece instanceof CatalogLight) {
          CatalogLight light = (CatalogLight)piece;
          piece = new CatalogLight(pieceId, pieceName, pieceDescription, 
              pieceIcon, light.getPlanIcon(), pieceModel,
              pieceWidth, pieceDepth, pieceHeight, pieceElevation, pieceMovable, light.getLightSources(), 
              pieceModelRotation, pieceCreator, pieceResizable, 
              piecePrice, pieceValueAddedTaxPercentage);
        } else {
          if (doorOrWindow != null && doorOrWindow) {
            piece = new CatalogDoorOrWindow(pieceId, pieceName, pieceDescription, 
                pieceIcon, piece.getPlanIcon(), pieceModel,
                pieceWidth, pieceDepth, pieceHeight, pieceElevation, pieceMovable,
                1, 0, new Sash [0], 
                pieceModelRotation, pieceCreator, pieceResizable, 
                piecePrice, pieceValueAddedTaxPercentage);
          } else {
            piece = new CatalogPieceOfFurniture(pieceId, pieceName, pieceDescription, 
                pieceIcon, piece.getPlanIcon(), pieceModel,
                pieceWidth, pieceDepth, pieceHeight, pieceElevation, pieceMovable, 
                pieceModelRotation, pieceCreator, pieceResizable, 
                piecePrice, pieceValueAddedTaxPercentage);
          }
        }
        new FurnitureCatalog() { }.add(pieceCategory, piece);
        this.furnitureLibrary.addPieceOfFurniture(piece, index);
        Set<String> supportedLanguages = new HashSet<String>(this.furnitureLibrary.getSupportedLanguages());
        supportedLanguages.add(this.furnitureLanguageController.getFurnitureLangauge());
        for (String language : supportedLanguages) {
          if (!FurnitureLibrary.DEFAULT_LANGUAGE.equals(language)) {
            boolean editedFurnitureLanguage = this.furnitureLanguageController.getFurnitureLangauge().equals(language);
            Object localizedPieceName = name != null && editedFurnitureLanguage 
                ? name : localizedNames.get(language);
            this.furnitureLibrary.setPieceOfFurnitureLocalizedData(
                piece, language, FurnitureLibrary.FURNITURE_NAME_PROPERTY, localizedPieceName);
            
            Object localizedPieceDescription = description != null && editedFurnitureLanguage 
                ? description : localizedDescriptions.get(language);
            this.furnitureLibrary.setPieceOfFurnitureLocalizedData(
                piece, language, FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY, localizedPieceDescription);
            
            Object localizedPieceCategory = category != null && editedFurnitureLanguage 
                ? category.getName() : localizedCategories.get(language);
            this.furnitureLibrary.setPieceOfFurnitureLocalizedData(
                piece, language, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, localizedPieceCategory);
          }
        }
      }
    }
  }
}
