# Android Sprite Library

Tiny library that provides sprite support.

## Usage


``` java
    ...
	Sprites sprites = new Sprites(context, sheetInputStream, imageInputStream);
	....
	Bitmap icon = sprites.get("an icon");
```


## Including in your project

Add the following to your *build.gradle* file(s):

``` groovy
repositories {
    mavenCentral()
}
```

``` groovy
dependencies {
    implementation 'ch.poole.android:sprites:0.0.4'
}
```

## Legal

Sources used for testing: Maki icons are from https://labs.mapbox.com/maki-icons/, the sprite icon sheet and image are from https://github.com/maputnik/osm-liberty 