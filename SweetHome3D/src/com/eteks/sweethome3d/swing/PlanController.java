/*
 * PlanController.java 2 juin 2006
 *
 * Copyright (c) 2006 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
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
package com.eteks.sweethome3d.swing;

import javax.swing.JComponent;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * A MVC controller for the plan view.
 * @author Emmanuel Puybaret
 */
public class PlanController implements Controller {
  public enum Mode {WALL_CREATION, SELECTION }
  
  private PlanComponent planComponent;
  private Home home;
  private UserPreferences preferences;

  public PlanController(Home home, UserPreferences preferences, UndoableEditSupport support) {
    this.home = home;
    this.preferences = preferences;
    this.planComponent = new PlanComponent(this, home, preferences);
  }

  /**
   * Returns the view associated with this controller.
   */
  public JComponent getView() {
    return this.planComponent;
  }

  /**
   * Sets the active mode of this controller. 
   */
  public void setMode(Mode mode) {
    // TODO Auto-generated method stub
    
  }

  /**
   * Returns the active mode of this controller.
   */
  public Object getMode() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Deletes the selection in home.
   */
  public void deleteSelection() {
    // TODO Auto-generated method stub
  }

  /**
   * Moves the selection of (<code>dx</code>,<code>dy</code>) pixels in home.
   */
  public void moveSelection(int dx, int dy) {
    // TODO Auto-generated method stub
  }
  
  /**
   * Disables temporary magnetism feature of user preferences. 
   * @param magnetismDisabled if <code>true</code> then magnetism feature isn't active.
   */
  public void setMagnetismDisabled(boolean magnetismDisabled) {
    // TODO Auto-generated method stub
   }

  /**
   * Processes a mouse click event. 
   */
  public void processMouseClicked(int x, int y, int clickCount) {
    // TODO Auto-generated method stub
  }

  /**
   * Processes a mouse button pressed event.
   */
  public void processMousePressed(int x, int y, boolean shiftDown) {
    // TODO Auto-generated method stub
  }

  /**
   * Processes a mouse button released event.
   */
  public void processMouseReleased(int x, int y) {
    // TODO Auto-generated method stub
  }

  /**
   * Processes a mouse button moved event.
   */
  public void processMouseMoved(int x, int y) {
    // TODO Auto-generated method stub
  }

  /**
   * Processes a mouse button dragged event.
   */
  public void processMouseDragged(int x, int y) {
    // TODO Auto-generated method stub
  }

  /**
   * Processes a focus lost event.
   */
  public void processFocusLost() {
    // TODO Auto-generated method stub
  }

  /**
   * Processes a component resized event.
   */
  public void processComponentResized() {
    // TODO Auto-generated method stub
  }
}
