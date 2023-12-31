TEXTURES LIBRARY EDITOR v 2.0
-----------------------------

This archive contains source code of the Textures Library Editor for Sweet Home 3D.

You may also download this source code with the following SVN command:

svn checkout https://svn.code.sf.net/p/sweethome3d/code/tags/V_2_0/TexturesLibraryEditor TexturesLibraryEditor


As this project depends on Sweet Home 3D, download and install also Sweet Home 3D source code from 
http://prdownloads.sourceforge.net/sweethome3d/SweetHome3D-7.2-src.zip 
or with the following SVN command: 

svn checkout https://svn.code.sf.net/p/sweethome3d/code/tags/V_7_2/SweetHome3D SweetHome3D


HOW TO USE THIS SOURCE CODE
---------------------------

Source code classes are compatible with Java SE 5, and the directory where
you find this file may be compiled as an Eclipse project or with Ant.

If you want to compile and test the Java files of this release 
with Eclipse, please follow the next steps:
1. install JDK or OpenJDK >= 5 available at http://java.sun.com and elsewhere
2. install Eclipse available at http://www.eclipse.org
3. in Eclipse, select File > Import... menu
4. in "Select an import source" list of Import dialog box, 
   choose "Existing project into Workspace" option
5. click on Next button
6. in "Select root directory" text field, 
   type or select the path to the root of this archive
7. click on Finish 
8. repeat the steps 3 to 7 to import Sweet Home 3D project

You may execute com.eteks.textureslibraryeditor.TexturesLibraryEditor 
application included in this archive, with the Eclipse Run menu.
If you use Java 16 or above, you should add also the following VM arguments 
to run the program:
--add-opens=java.desktop/java.awt=ALL-UNNAMED
--add-opens=java.desktop/sun.awt=ALL-UNNAMED
--add-opens=java.desktop/com.apple.eio=ALL-UNNAMED
--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED

If you want to build Textures Library Editor with Ant, please install last JDK 5 or greater and 
Ant available at http://ant.apache.org/, and run ant command in the directory where
you uncompressed this archive.
The default target provided by build.xml Ant file builds a JAR executable version.
Read build.xml Ant file for more information about other available targets. 


Read LICENSE.TXT file for more details about licenses applicable to this software
and included materials developed by third parties.
    
    
Copyright (c) 2012-2023 Emmanuel Puybaret / eTeks. Distributed under GNU General Public License