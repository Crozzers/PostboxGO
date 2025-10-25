# Contributor Guidelines

Contributions, issues, feedback and ideas are welcome.

## Dev setup

You'll need these installed:
- Android Studio
- ADB
- OpenJDK / Some other Java dev kit

Once done you can clone the project and set up your secrets file:
```shell
git clone https://github.com/Crozzers/PostboxGO
cd PostboxGO
echo "MAPS_API_KEY=<your api key here> > secrets.properties"
```

Open the project in android studio and create a virtual device to test the app on. Once up and
running you can upload a sample savefile to it with
`adb push docs/sample_savefile.json /storage/emulated/0/Download/base.json`.

Then install the app to the device, open settings and import that savefile from the downloads folder.

## Commits and PRs

This repo uses the [angular style](https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-angular)
of conventional commits to enable automatic generation of the changelog and release notes.

Also see Angular's [commit message format guidelines](https://github.com/angular/angular/blob/main/contributing-docs/commit-message-guidelines.md).

## Updating app screenshots

Start up an android emulator where you don't mind if the save file is overwritten.
Run the following commands to remove the previous screenshots from the emulator:
```shell
# options for device are phone or tablet
device=phone &&
    adb push docs/sample_savefile.json /storage/emulated/0/Download/base.json &&
    adb shell "rm -rf /storage/emulated/0/Pictures/pbg" &&
    adb shell "mkdir /storage/emulated/0/Pictures/pbg" &&
    ./gradlew -Pandroid.testInstrumentationRunnerArguments.screenshots=1 -Pandroid.testInstrumentationRunnerArguments.device=$device connectedAndroidTest &&
    for file in $(adb shell "ls /storage/emulated/0/Pictures/pbg/*.png"); do adb pull $file docs/images/autocapture/; done
```

This will output the images to `docs/images`, where you can then update the README/playstore listing.