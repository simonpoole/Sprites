# Android Sprite Library

Tiny library that provides sprite support.

## Usage


``` java
    ...
	Sprite sprite = new Sprite(sheetInputStream, imageInputStream);
	....
	Bitmap icon = sprite("an icon");
```


## Including in your project

Add the following to your *build.gradle* file(s):

``` groovy
repositories {
    maven {
    ..... not published yet ......
    }
}
```

``` groovy
dependencies {
    compile "ch.poole.android:sprite:0.0.0"
}
```

## Legal

Maki icons are from https://labs.mapbox.com/maki-icons/, the sprite icon sheet and image are from https://github.com/maputnik/osm-liberty 