This folder contains the build project to create custom builds of dojo specifically tailord to the application.

Directions for use:

1. Install CPM to help pull in the required libraries:  https://github.com/kriszyp/cpm

2. 
From the src directory execute:
cpm install dgrid - This will pull in the correct versions of dgrid,put-selector,xstyle into the src directory

download and install the Dojo build SDK http://dojotoolkit.org/download/#sdk into the src directory so the structure of the directory is:
src/dgrid
src/dijit
src/dojo
src/dojox
src/put-selector
src/xstyle
src/util


3. From the dojo-build directory run the build script build-dojo.js

Files In this folder:

build-dojo.sh - Generate the build into the dist folder using the mango.profile.js build profile
build-dojo-debug.sh - Generate the build into the debug folder using the mango-debug.profile.  This is used for testing and debugging mango.

src/ - where the source js files go

profiles/mango.profile.js - The dojo build profile 
profiles/mango.debug.profile.js - The debug dojo build profile