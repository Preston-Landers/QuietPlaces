#!/usr/bin/env bash

# Extract all of the resources of the given name into the project folder.

SDKDIR=/cygdrive/d/AndroidSDK/android-sdk/platforms/android-19/data/res
DESTDIR=/cygdrive/d/IdeaProjects/QuietPlaces/QuietPlaces/src/main/res/

# RESOURCES="ic_menu_upload.png ic_menu_week.png"

RESOURCES="ic_menu_btn_add.png ic_menu_save.png ic_menu_forward.png ic_menu_revert.png ic_menu_rotate.png ic_menu_clear_playlist.png"


pushd $SDKDIR

for FILE in $RESOURCES ; do
    # echo $FILE
    for RES in `find . -name $FILE`; do
        DEST=$DESTDIR/$RES
        echo Copying $RES to $DEST
        cp $RES $DEST
    done
done

popd
