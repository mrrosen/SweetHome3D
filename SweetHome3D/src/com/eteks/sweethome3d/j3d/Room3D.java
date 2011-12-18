/*
 * Room3D.java 23 jan. 09
 *
 * Sweet Home 3D, Copyright (c) 2007-2009 Emmanuel PUYBARET / eTeks <info@eteks.com>
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
package com.eteks.sweethome3d.j3d;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.Node;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeFurnitureGroup;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.HomeTexture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

/**
 * Root of room branch.
 */
public class Room3D extends Object3DBranch {
  private static final TextureAttributes MODULATE_TEXTURE_ATTRIBUTES = new TextureAttributes();
  
  static {
    MODULATE_TEXTURE_ATTRIBUTES.setTextureMode(TextureAttributes.MODULATE);
  }
  
  private static final int FLOOR_PART  = 0;
  private static final int CEILING_PART = 1;
  
  private final Home home;

  /**
   * Creates the 3D room matching the given home <code>room</code>.
   */
  public Room3D(Room room, Home home) {
    this(room, home, false, false, false);
  }

  /**
   * Creates the 3D room matching the given home <code>room</code>.
   */
  public Room3D(Room room, Home home,
                boolean ignoreCeilingPart, 
                boolean ignoreInvisiblePart,
                boolean waitTextureLoadingEnd) {
    setUserData(room);
    this.home = home;

    // Allow room branch to be removed from its parent
    setCapability(BranchGroup.ALLOW_DETACH);
    // Allow to read branch shape children
    setCapability(BranchGroup.ALLOW_CHILDREN_READ);
    
    // Add room floor and cellar empty shapes to branch
    addChild(createRoomPartShape());
    addChild(createRoomPartShape());
    // Set room shape geometry and appearance
    updateRoomGeometry();
    updateRoomAppearance(waitTextureLoadingEnd);
    
    if (ignoreCeilingPart
        || (ignoreInvisiblePart
            && !room.isCeilingVisible())) {
      removeChild(CEILING_PART);
    }
    if (ignoreInvisiblePart
        && !room.isFloorVisible()) {
      removeChild(FLOOR_PART);
    }
  }

  /**
   * Returns a new room part shape with no geometry  
   * and a default appearance with a white material.
   */
  private Node createRoomPartShape() {
    Shape3D roomShape = new Shape3D();
    // Allow room shape to change its geometry
    roomShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
    roomShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    roomShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

    Appearance roomAppearance = new Appearance();
    roomShape.setAppearance(roomAppearance);
    roomAppearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
    TransparencyAttributes transparencyAttributes = new TransparencyAttributes();
    transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
    transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
    roomAppearance.setTransparencyAttributes(transparencyAttributes);
    roomAppearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
    RenderingAttributes renderingAttributes = new RenderingAttributes();
    renderingAttributes.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
    roomAppearance.setRenderingAttributes(renderingAttributes);
    roomAppearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
    roomAppearance.setMaterial(DEFAULT_MATERIAL);      
    roomAppearance.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
    roomAppearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
    // Mix texture and room color
    roomAppearance.setTextureAttributes(MODULATE_TEXTURE_ATTRIBUTES);
    
    return roomShape;
  }

  @Override
  public void update() {
    updateRoomGeometry();
    updateRoomAppearance(false);
  }
  
  /**
   * Sets the 3D geometry of this room shapes that matches its 2D geometry.  
   */
  private void updateRoomGeometry() {
    updateRoomPartGeometry(FLOOR_PART, ((Room)getUserData()).getFloorTexture());
    updateRoomPartGeometry(CEILING_PART, ((Room)getUserData()).getCeilingTexture());
  }
  
  private void updateRoomPartGeometry(int roomPart, HomeTexture texture) {
    Shape3D roomShape = (Shape3D)getChild(roomPart);
    int currentGeometriesCount = roomShape.numGeometries();
    Room room = (Room)getUserData();
    if (room.getLevel() == null || room.getLevel().isVisible()) {
      for (Geometry roomGeometry : createRoomGeometries(roomPart, texture)) {
        roomShape.addGeometry(roomGeometry);
      }
    }
    for (int i = currentGeometriesCount - 1; i >= 0; i--) {
      roomShape.removeGeometry(i);
    }
  }
  
  /**
   * Returns room geometry computed from its points.
   */
  private Geometry [] createRoomGeometries(int roomPart, HomeTexture texture) {
    Room room = (Room)getUserData();
    float [][] points = room.getPoints();
    if ((roomPart == FLOOR_PART && room.isFloorVisible()
         || roomPart == CEILING_PART && room.isCeilingVisible())
        && points.length > 2) {
      Level roomLevel = room.getLevel();
      List<Level> levels = this.home.getLevels();
      boolean lastLevel = isLastLevel(roomLevel, levels);
      float floorBottomElevation;
      float roomElevation;
      if (roomLevel != null) {
        roomElevation = roomLevel.getElevation();
        floorBottomElevation = roomElevation - roomLevel.getFloorThickness();
      } else {
        roomElevation = 0;
        floorBottomElevation = 0;
      }

      float firstLevelElevation;
      if (levels.size() == 0) {
        firstLevelElevation = 0;
      } else {
        firstLevelElevation = levels.get(0).getElevation();
      }
      boolean floorBottomVisible = roomPart == FLOOR_PART 
          && roomLevel != null 
          && roomElevation != firstLevelElevation;

      // Find rooms at the same elevation 
      /// and room ceilings at same elevation as the floor bottom  
      List<Room> roomsAtSameElevation = new ArrayList<Room>();
      List<Room> ceilingsAtSameFloorBottomElevation = new ArrayList<Room>();
      for (Room homeRoom : this.home.getRooms()) {
        Level homeRoomLevel = homeRoom.getLevel();
        if (room == homeRoom // Store also the room itself to know its order among rooms at same elevation
            || roomLevel == homeRoomLevel
                && (roomPart == FLOOR_PART && homeRoom.isFloorVisible()
                    || roomPart == CEILING_PART && homeRoom.isCeilingVisible()) 
            || roomLevel != null 
                && homeRoomLevel != null 
                && (roomPart == FLOOR_PART 
                        && homeRoom.isFloorVisible()
                        && Math.abs(roomElevation - homeRoomLevel.getElevation()) < 1E-5
                    || roomPart == CEILING_PART 
                        && homeRoom.isCeilingVisible()
                        && !lastLevel
                        && !isLastLevel(homeRoomLevel, levels)
                        && Math.abs(roomElevation + roomLevel.getHeight() - (homeRoomLevel.getElevation() + homeRoomLevel.getHeight())) < 1E-5)) {         
          roomsAtSameElevation.add(homeRoom);
        } else if (floorBottomVisible 
                    && homeRoomLevel != null 
                    && homeRoom.isCeilingVisible() 
                    && !isLastLevel(homeRoomLevel, levels) 
                    && Math.abs(floorBottomElevation - (homeRoomLevel.getElevation() + homeRoomLevel.getHeight())) < 1E-5) {
          ceilingsAtSameFloorBottomElevation.add(homeRoom);
        }
      }
      
      List<HomePieceOfFurniture> visibleStaircases;
      if (roomLevel == null
          || roomPart == CEILING_PART
              && lastLevel) {
        visibleStaircases = Collections.emptyList();
      } else {
        visibleStaircases = getVisibleStaircases(this.home.getFurniture(), roomPart, roomLevel, 
            roomLevel.getElevation() == firstLevelElevation);
      }

      // Retrieve room points
      List<float [][]> roomPoints;
      Map<Integer, List<float [][]>> roomHoles;
      Area roomVisibleArea;
      // If room isn't singular retrieve all the points of its different polygons 
      if (!room.isSingular() 
          || roomsAtSameElevation.get(roomsAtSameElevation.size() - 1) != room
          || visibleStaircases.size() > 0) {        
        roomVisibleArea = new Area(getShape(points));
        if (roomsAtSameElevation.contains(room)) {
          // Remove other rooms surface that may overlap the current room
          for (int i = roomsAtSameElevation.size() - 1; i > 0 && roomsAtSameElevation.get(i) != room; i--) {
            Room otherRoom = roomsAtSameElevation.get(i);
            roomVisibleArea.subtract(new Area(getShape(otherRoom.getPoints())));
          }
        }        
        removeStaircasesFromArea(visibleStaircases, roomVisibleArea);
        roomPoints = new ArrayList<float[][]>();
        roomHoles = new HashMap<Integer, List<float [][]>>();
        getAreaPoints(roomVisibleArea, roomPart == CEILING_PART, roomPoints, roomHoles);
      } else {
        boolean clockwise = room.isClockwise();
        if (clockwise && roomPart == FLOOR_PART
            || !clockwise && roomPart == CEILING_PART) {
          // Reverse points order if they are in the good order
          points = getReversedArray(points);
        }
        roomPoints = Arrays.asList(new float [][][] {points});
        roomHoles = Collections.emptyMap();
        roomVisibleArea = null;
      }
      
      // Retrieve points of the room floor bottom
      List<float [][]> floorBottomPoints;
      Map<Integer, List<float [][]>> floorBottomHoles;
      if (floorBottomVisible) {
        if (roomVisibleArea != null 
            || ceilingsAtSameFloorBottomElevation.size() > 0) {        
          Area floorBottomVisibleArea = roomVisibleArea != null ? roomVisibleArea : new Area(getShape(points));
          // Remove other rooms surface that may overlap the floor bottom
          for (Room otherRoom : ceilingsAtSameFloorBottomElevation) {
            floorBottomVisibleArea.subtract(new Area(getShape(otherRoom.getPoints())));
          }          
          floorBottomPoints = new ArrayList<float[][]>();
          floorBottomHoles = new HashMap<Integer, List<float [][]>>();
          getAreaPoints(floorBottomVisibleArea, true, floorBottomPoints, floorBottomHoles);
        } else {
          floorBottomPoints = Arrays.asList(new float [][][] {getReversedArray(points)});
          floorBottomHoles = Collections.emptyMap();
        }
      } else {
        floorBottomPoints = Collections.emptyList();
        floorBottomHoles = Collections.emptyMap();
      }      
      
      Geometry [] geometries = new Geometry [roomPoints.size() + floorBottomPoints.size()];
      boolean computeFloorBorder = roomLevel != null
          && roomPart == FLOOR_PART 
          && roomLevel.getElevation() != firstLevelElevation;
      // Compute room and border geometries
      int i = 0;      
      for ( ; i < roomPoints.size(); i++) {
        float [][] geometryPoints = roomPoints.get(i);
        List<float [][]> geometryHoles = roomHoles.get(i);
        if (geometryHoles == null) {
          geometryHoles = Collections.emptyList();
        }
        int geometryHolesPointCount = 0;
        for (float [][] geometryHole : geometryHoles) {
          geometryHolesPointCount += geometryHole.length;
        }
        
        int contourLength = 1;
        if (computeFloorBorder) {
          contourLength += geometryPoints.length + geometryHolesPointCount;
        }
        int [] contourCounts = new int [contourLength];
        int [] stripCounts = new int [contourCounts.length + geometryHoles.size()];
        int j = 0;
        int vertexCount = geometryPoints.length;
        if (computeFloorBorder) {
          vertexCount *= 5;
          vertexCount += geometryHolesPointCount * 4;
          Arrays.fill(contourCounts, 0, j = geometryPoints.length + geometryHolesPointCount, 1);
          Arrays.fill(stripCounts, 0, j, 4);
        }
        contourCounts [contourCounts.length - 1] = 1 + geometryHoles.size();
        stripCounts [j++] = geometryPoints.length;
        for (float [][] geometryHole : geometryHoles) {
          vertexCount += geometryHole.length;
          stripCounts [j++] = geometryHole.length;
        }
        
        j = 0;
        Point3f [] coords = new Point3f [vertexCount];
        if (computeFloorBorder) {
          // Compute room borders coordinates
          for (int k = 0; k < geometryPoints.length; k++) {
            coords [j++] = new Point3f(geometryPoints [k][0], roomElevation, geometryPoints [k][1]);
            coords [j++] = new Point3f(geometryPoints [k][0], floorBottomElevation, geometryPoints [k][1]);
            int nextPoint = k < geometryPoints.length - 1  
                ? k + 1
                : 0;
            coords [j++] = new Point3f(geometryPoints [nextPoint][0], floorBottomElevation, geometryPoints [nextPoint][1]);
            coords [j++] = new Point3f(geometryPoints [nextPoint][0], roomElevation, geometryPoints [nextPoint][1]);
          }
          // Compute holes borders coordinates
          for (float [][] geometryHole : geometryHoles) {
            for (int k = 0; k < geometryHole.length; k++) {
              coords [j++] = new Point3f(geometryHole [k][0], roomElevation, geometryHole [k][1]);
              int nextPoint = k < geometryHole.length - 1  
                  ? k + 1
                  : 0;
              coords [j++] = new Point3f(geometryHole [nextPoint][0], roomElevation, geometryHole [nextPoint][1]);
              coords [j++] = new Point3f(geometryHole [nextPoint][0], floorBottomElevation, geometryHole [nextPoint][1]);
              coords [j++] = new Point3f(geometryHole [k][0], floorBottomElevation, geometryHole [k][1]);
            }
          }
        }
        // Compute room coordinates
        for (int k = 0; k < geometryPoints.length; k++) {
          float y = roomPart == FLOOR_PART 
              ? roomElevation 
              : getRoomHeightAt(geometryPoints [k][0], geometryPoints [k][1]);
          coords [j++] = new Point3f(geometryPoints [k][0], y, geometryPoints [k][1]);
        }
        for (float [][] geometryHole : geometryHoles) {
          for (int k = 0; k < geometryHole.length; k++) {
            float y = roomPart == FLOOR_PART 
                ? roomElevation 
                : getRoomHeightAt(geometryHole [k][0], geometryHole [k][1]);
            coords [j++] = new Point3f(geometryHole [k][0], y, geometryHole [k][1]);
          }
        }
        GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        geometryInfo.setCoordinates(coords);
        geometryInfo.setStripCounts(stripCounts);
        geometryInfo.setContourCounts(contourCounts);
        
        if (texture != null) {
          TexCoord2f [] textureCoords = new TexCoord2f [vertexCount];
          j = 0;
          if (computeFloorBorder) {
            // Compute room border texture coordinates
            for (int k = 0; k < geometryPoints.length; k++) {
              textureCoords [j++] = new TexCoord2f(geometryPoints [k][0] / texture.getWidth(), -geometryPoints [k][1] / texture.getHeight());
              textureCoords [j++] = new TexCoord2f(geometryPoints [k][0] / texture.getWidth(), -(geometryPoints [k][1] - roomLevel.getFloorThickness()) / texture.getHeight());
              int nextPoint = k < geometryPoints.length - 1  
                  ? k + 1
                  : 0;
              textureCoords [j++] = new TexCoord2f(geometryPoints [nextPoint][0] / texture.getWidth(), -(geometryPoints [nextPoint][1] - roomLevel.getFloorThickness()) / texture.getHeight());
              textureCoords [j++] = new TexCoord2f(geometryPoints [nextPoint][0] / texture.getWidth(), -geometryPoints [nextPoint][1] / texture.getHeight());
            }
            // Compute holes borders texture coordinates
            for (float [][] geometryHole : geometryHoles) {
              for (int k = 0; k < geometryHole.length; k++) {
                textureCoords [j++] = new TexCoord2f(geometryHole [k][0] / texture.getWidth(), -geometryHole [k][1] / texture.getHeight());
                int nextPoint = k < geometryHole.length - 1  
                    ? k + 1
                    : 0;
                textureCoords [j++] = new TexCoord2f(geometryHole [nextPoint][0] / texture.getWidth(), -geometryHole [nextPoint][1] / texture.getHeight());
                textureCoords [j++] = new TexCoord2f(geometryHole [nextPoint][0] / texture.getWidth(), -(geometryHole [nextPoint][1] - roomLevel.getFloorThickness()) / texture.getHeight());
                textureCoords [j++] = new TexCoord2f(geometryHole [k][0] / texture.getWidth(), -(geometryHole [k][1] - roomLevel.getFloorThickness()) / texture.getHeight());
              }
            }
          }
          // Compute room texture coordinates
          for (int k = 0; k < geometryPoints.length; k++) {
            textureCoords [j++] = new TexCoord2f(geometryPoints [k][0] / texture.getWidth(), 
                roomPart == FLOOR_PART 
                    ? -geometryPoints [k][1] / texture.getHeight()
                    : geometryPoints [k][1] / texture.getHeight());
          }
          for (float [][] geometryHole : geometryHoles) {
            for (int k = 0; k < geometryHole.length; k++) {
              textureCoords [j++] = new TexCoord2f(geometryHole [k][0] / texture.getWidth(), 
                  roomPart == FLOOR_PART 
                      ? -geometryHole [k][1] / texture.getHeight()
                      : geometryHole [k][1] / texture.getHeight());
            }
          }
          geometryInfo.setTextureCoordinateParams(1, 2);
          geometryInfo.setTextureCoordinates(0, textureCoords);
        }
        
        // Generate normals
        new NormalGenerator(Math.PI / 8).generateNormals(geometryInfo);
        geometries [i] = geometryInfo.getIndexedGeometryArray();
      }

      // Compute floor bottom geometry
      for (int l = 0 ; l < floorBottomPoints.size(); l++, i++) {
        float [][] geometryPoints = floorBottomPoints.get(l);
        List<float [][]> geometryHoles = floorBottomHoles.get(l);
        if (geometryHoles == null) {
          geometryHoles = Collections.emptyList();
        }
        int [] contourCounts = new int [1];
        int [] stripCounts = new int [1 + geometryHoles.size()];
        int j = 0;
        int vertexCount = geometryPoints.length;
        contourCounts [contourCounts.length - 1] = 1 + geometryHoles.size();
        stripCounts [j++] = geometryPoints.length;
        for (float [][] geometryHole : geometryHoles) {
          vertexCount += geometryHole.length;
          stripCounts [j++] = geometryHole.length;
        }
        
        j = 0;
        Point3f [] coords = new Point3f [vertexCount];
        // Compute floor bottom coordinates
        for (int k = 0; k < geometryPoints.length; k++) {
          coords [j++] = new Point3f(geometryPoints [k][0], floorBottomElevation, geometryPoints [k][1]);
        }
        for (float [][] geometryHole : geometryHoles) {
          for (int k = 0; k < geometryHole.length; k++) {
            coords [j++] = new Point3f(geometryHole [k][0], floorBottomElevation, geometryHole [k][1]);
          }
        }
        GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        geometryInfo.setCoordinates(coords);
        geometryInfo.setStripCounts(stripCounts);
        geometryInfo.setContourCounts(contourCounts);
        
        if (texture != null) {
          TexCoord2f [] textureCoords = new TexCoord2f [vertexCount];
          j = 0;
          // Compute floor bottom texture coordinates
          for (int k = 0; k < geometryPoints.length; k++) {
            textureCoords [j++] = new TexCoord2f(geometryPoints [k][0] / texture.getWidth(), geometryPoints [k][1] / texture.getHeight());
          }
          for (float [][] geometryHole : geometryHoles) {
            for (int k = 0; k < geometryHole.length; k++) {
              textureCoords [j++] = new TexCoord2f(geometryHole [k][0] / texture.getWidth(), geometryHole [k][1] / texture.getHeight());
            }
          }
          geometryInfo.setTextureCoordinateParams(1, 2);
          geometryInfo.setTextureCoordinates(0, textureCoords);
        }
        
        // Generate normals
        new NormalGenerator().generateNormals(geometryInfo);
        geometries [i] = geometryInfo.getIndexedGeometryArray();
      }

      return geometries;
    } else {
      return new Geometry [0];
    }
  }

  private void removeStaircasesFromArea(List<HomePieceOfFurniture> visibleStaircases, Area area) {
    // Remove from room area all the staircases that intersect it
    for (HomePieceOfFurniture staircase : visibleStaircases) {
      Shape shape = parseShape(staircase.getStaircaseCutOutShape());
      Area staircaseArea = new Area(shape);
      if (staircase.isModelMirrored()) {
        staircaseArea = getMirroredArea(staircaseArea);
      }
      AffineTransform staircaseTransform = AffineTransform.getTranslateInstance(
          staircase.getX() - staircase.getWidth() / 2, 
          staircase.getY() - staircase.getDepth() / 2);
      staircaseTransform.concatenate(AffineTransform.getRotateInstance(staircase.getAngle(),
          staircase.getWidth() / 2, staircase.getDepth() / 2));
      staircaseTransform.concatenate(AffineTransform.getScaleInstance(staircase.getWidth(), staircase.getDepth()));
      staircaseArea.transform(staircaseTransform);
      area.subtract(staircaseArea);
    }
  }

  /**
   * Returns the mirror area of the given <<code>area</code>.
   */
  private Area getMirroredArea(Area area) {
    // As applying a -1 scale transform reverses the holes / non holes interpretation of the points, 
    // we have to create a mirrored shape by parsing points
    GeneralPath mirrorPath = new GeneralPath();
    float [] point = new float[6];
    for (PathIterator it = area.getPathIterator(null); !it.isDone(); it.next()) {
      switch (it.currentSegment(point)) {
        case PathIterator.SEG_MOVETO :
          mirrorPath.moveTo(1 - point[0], point[1]);
          break;
        case PathIterator.SEG_LINETO : 
          mirrorPath.lineTo(1 - point[0], point[1]);
          break;
        case PathIterator.SEG_QUADTO : 
          mirrorPath.quadTo(1 - point[0], point[1], 1 - point[2], point[3]);
          break;
        case PathIterator.SEG_CUBICTO : 
          mirrorPath.curveTo(1 - point[0], point[1], 1 - point[2], point[3], 1 - point[4], point[5]);
          break;
        case PathIterator.SEG_CLOSE :
          mirrorPath.closePath();
          break;
      }
    }
    return new Area(mirrorPath);
  }

  /**
   * Returns the visible staircases among the given <code>furniture</code>.  
   */
  private List<HomePieceOfFurniture> getVisibleStaircases(List<HomePieceOfFurniture> furniture, 
                                                          int roomPart, Level roomLevel,
                                                          boolean firstLevel) {
    List<HomePieceOfFurniture> visibleStaircases = new ArrayList<HomePieceOfFurniture>(furniture.size());
    for (HomePieceOfFurniture piece : furniture) {
      if (piece.isVisible()) {
        if (piece instanceof HomeFurnitureGroup) {
          visibleStaircases.addAll(getVisibleStaircases(((HomeFurnitureGroup)piece).getFurniture(), roomPart, roomLevel, firstLevel));
        } else if (piece.getStaircaseCutOutShape() != null
            && !"false".equalsIgnoreCase(piece.getStaircaseCutOutShape())
            && ((roomPart == FLOOR_PART 
                    && piece.getGroundElevation() < roomLevel.getElevation()
                    && piece.getGroundElevation() + piece.getHeight() >= roomLevel.getElevation() - (firstLevel ? 0 : roomLevel.getFloorThickness())
                || roomPart == CEILING_PART
                    && piece.getGroundElevation() < roomLevel.getElevation() + roomLevel.getHeight()
                    && piece.getGroundElevation() + piece.getHeight() >= roomLevel.getElevation() + roomLevel.getHeight()))) {
          visibleStaircases.add(piece);
        }
      }
    }
    return visibleStaircases;
  }

  /**
   * Fills <code>areaPoints</code> and <code>areaHoles</code> from the given area.
   */
  private void getAreaPoints(Area area, 
                             boolean reversed, 
                             List<float [][]> areaPoints,
                             Map<Integer, List<float [][]>> areaHoles) {
    // Retrieve the points of the different polygons 
    // and reverse their points order if necessary
    List<float []> currentPathPoints = new ArrayList<float[]>();
    float [] previousRoomPoint = null;
    int i = 0;
    for (PathIterator it = area.getPathIterator(null, 1); !it.isDone(); it.next()) {
      float [] roomPoint = new float[2];
      switch (it.currentSegment(roomPoint)) {
        case PathIterator.SEG_MOVETO :
        case PathIterator.SEG_LINETO : 
          if (previousRoomPoint == null
              || roomPoint [0] != previousRoomPoint [0] 
              || roomPoint [1] != previousRoomPoint [1]) {
            currentPathPoints.add(roomPoint);
          }
          previousRoomPoint = roomPoint;
          break;
        case PathIterator.SEG_CLOSE :
          if (currentPathPoints.get(0) [0] == previousRoomPoint [0] 
              && currentPathPoints.get(0) [1] == previousRoomPoint [1]) {
            currentPathPoints.remove(currentPathPoints.size() - 1);
          }
          if (currentPathPoints.size() > 2) {
            float [][] pathPoints = 
                currentPathPoints.toArray(new float [currentPathPoints.size()][]);
            Room subRoom = new Room(pathPoints);
            if (subRoom.getArea() > 0) {
              boolean pathPointsClockwise = subRoom.isClockwise();
              if (pathPointsClockwise) {
                // Store counter clockwise points as holes
                if (!reversed) {
                  pathPoints = getReversedArray(pathPoints);
                }
                List<float [][]> holes = areaHoles.get(i);
                if (holes == null) {
                  holes = new ArrayList<float [][]>(1);
                  areaHoles.put(i, holes);
                }
                holes.add(pathPoints);
              } else {
                if (reversed) {
                  pathPoints = getReversedArray(pathPoints);
                }
                areaPoints.add(pathPoints);
                i++;
              }
            }
          }
          currentPathPoints.clear();
          previousRoomPoint = null;
          break;
      }
    }
  }

  /**
   * Returns an array that cites <code>points</code> in reverse order.
   */
  private float [][] getReversedArray(float [][] points) {
    points = points.clone();
    List<float []> pointList = Arrays.asList(points);
    Collections.reverse(pointList);
    return pointList.toArray(points);
  }
  
  /**
   * Returns the room height at the given point. 
   */
  private float getRoomHeightAt(float x, float y) {
    double smallestDistance = Float.POSITIVE_INFINITY;
    Room room = (Room)getUserData();
    Level roomLevel = room.getLevel();
    float roomElevation = roomLevel != null
        ? roomLevel.getElevation()
        : 0;
    float roomHeight = roomElevation + 
        (roomLevel == null ? this.home.getWallHeight() : roomLevel.getHeight());
    List<Level> levels = this.home.getLevels();
    if (roomLevel == null || isLastLevel(roomLevel, levels)) {
      // Search the closest wall point to x, y at last level
      for (Wall wall : this.home.getWalls()) {
        if (wall.isAtLevel(roomLevel)) {
          float wallElevation = wall.getLevel() == null ? 0 : wall.getLevel().getElevation();
          Float wallHeightAtStart = wall.getHeight();
          float [][] points = wall.getPoints();
          for (int i = 0; i < points.length; i++) {
            double distanceToWallPoint = Point2D.distanceSq(points [i][0], points [i][1], x, y);
            if (distanceToWallPoint < smallestDistance) {
              smallestDistance = distanceToWallPoint; 
              if (i == 0 || i == points.length - 1) { // Wall start
                roomHeight = wallHeightAtStart != null 
                    ? wallHeightAtStart 
                    : this.home.getWallHeight();
              } else { // Wall end
                roomHeight = wall.isTrapezoidal() 
                    ? wall.getHeightAtEnd() 
                    : (wallHeightAtStart != null ? wallHeightAtStart : this.home.getWallHeight());
              }
              roomHeight += wallElevation;
            }
          }
        }
      }
    }
    return roomHeight;
  }
  
  /**
   * Returns <code>true</code> if the given level is the last level in home.
   */
  private boolean isLastLevel(Level level, List<Level> levels) {
    return levels.indexOf(level) == levels.size() - 1;
  }

  /**
   * Sets room appearance with its color, texture.
   */
  private void updateRoomAppearance(boolean waitTextureLoadingEnd) {
    Room room = (Room)getUserData();
    boolean ignoreTransparency = room.getLevel() == null || room.getLevel().getElevation() <= 0;
    updateRoomPartAppearance(((Shape3D)getChild(FLOOR_PART)).getAppearance(), 
        room.getFloorTexture(), waitTextureLoadingEnd, room.getFloorColor(), room.getFloorShininess(), room.isFloorVisible(), ignoreTransparency);
    updateRoomPartAppearance(((Shape3D)getChild(CEILING_PART)).getAppearance(), 
        room.getCeilingTexture(), waitTextureLoadingEnd, room.getCeilingColor(), room.getCeilingShininess(), room.isCeilingVisible(), false);
  }
  
  /**
   * Sets room part appearance with its color, texture and visibility.
   */
  private void updateRoomPartAppearance(final Appearance roomPartAppearance, 
                                        final HomeTexture roomPartTexture,
                                        boolean waitTextureLoadingEnd,
                                        Integer roomPartColor,
                                        float shininess,
                                        boolean visible,
                                        boolean ignoreTransparency) {
    if (roomPartTexture == null) {
      roomPartAppearance.setMaterial(getMaterial(roomPartColor, roomPartColor, shininess));
      roomPartAppearance.setTexture(null);
    } else {
      // Update material and texture of room part
      roomPartAppearance.setMaterial(getMaterial(DEFAULT_COLOR, DEFAULT_AMBIENT_COLOR, shininess));
      final TextureManager textureManager = TextureManager.getInstance();
      textureManager.loadTexture(roomPartTexture.getImage(), waitTextureLoadingEnd,
          new TextureManager.TextureObserver() {
              public void textureUpdated(Texture texture) {
                roomPartAppearance.setTexture(texture);
              }
            });
    }
    if (!ignoreTransparency) { 
      // Update room part transparency
      float upperRoomsAlpha = this.home.getEnvironment().getWallsAlpha();
      TransparencyAttributes transparencyAttributes = roomPartAppearance.getTransparencyAttributes();
      transparencyAttributes.setTransparency(upperRoomsAlpha);
      // If alpha is equal to zero, turn off transparency to get better results 
      transparencyAttributes.setTransparencyMode(upperRoomsAlpha == 0 
          ? TransparencyAttributes.NONE 
          : TransparencyAttributes.NICEST);
    }
    // Update room part visibility
    RenderingAttributes renderingAttributes = roomPartAppearance.getRenderingAttributes();
    renderingAttributes.setVisible(visible);
  }
}