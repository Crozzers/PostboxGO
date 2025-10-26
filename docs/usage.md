# Usage

## Basics

When you open the app for the first time you will be greeted with an empty home page, and a message
telling you to register a postbox.

This app will only track postboxes that you have registered, so until you do so, nothing will show up.

There is a navigation bar at the bottom which will take you to the main screens in the app:

- [List view](#list-view) - the homepage and default view. Registered postboxes will be shown here
- [Map view](#map-view) - a map showing all of the postboxes you have registered
- [Statistics](#statistics) - shows stats about the postboxes you have registered
- [Settings](#settings) - app settings
- [Register](#registering-postboxes) - the screen to register a new postbox

## Registering postboxes

Registering postboxes is the main goal of the app, and there are 3 different types of registration
that can happen:

- [Nearby postboxes](#registering-a-nearby-postbox)
- [Postboxes in another location](#registering-a-postbox-in-another-location)
- [Inactive postboxes](#registering-an-inactive-postbox)


### Registering a nearby postbox

The main postbox registration method is registering a postbox that you are standing next to.

#### Step 1

Simply walk up to the postbox, open the app and click "Register" in the bottom right.

On this screen you have 2 options to fill in: the postbox and the monarch.

#### Step 2

Select the postbox dropdown and you'll be shown all the known postboxes within a few miles of
your location. Each postbox is shown in the following format:
```
Postbox Name (postbox number/ID) (X.X miles away)
# EG:
Grays Inn Road (ELM ST) (WC1X 36) (0.0 miles away)
# ^ Name               | ^ ID    |  ^ Distance
```
You can check the postbox label for the postbox number/ID, usually near the bottom, to confirm the
exact postbox you're looking at.

Once you've selected the postbox the app will also show it on a map view for confirmation.

#### Step 3

For the monarch, you'll be given a list of options. Look on the front of the postbox for a
royal cipher. It will usually spell out the monarch's initials (eg: VR for Queen Victoria)
and may include a number for clarification (eg: E II R for Queen Elizabeth 2nd).

The [letter box study group](https://lbsg.org/about-boxes/) has some really great resources
for recognising the different postbox types and ciphers.

Don't worry if you don't know exactly what the cipher is on the first go, you can always go
back and edit it later.

#### Step 4

Click save and congratulations! You've registered your first postbox

<div>
<img
    height="512" src="images/add_nearby_1.png"
    alt="A screenshot of the PostboxGO app postbox registration screen"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/add_nearby_2.png"
    alt="Screenshot of the PostboxGO app postbox registration screen, once the postbox dropdown has been selected"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/add_nearby_3.png"
    alt="Screenshot of the PostboxGO app postbox registration screen, once the monarch dropdown has been selected"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/autocapture/phone_add_postbox_view.png"
    alt="Screenshot of the PostboxGO app postbox registration screen with all options filled in"
    style="float: left; margin-left:5px"
/>
</div>

### Registering a postbox in another location

Sometimes you might want to register a postbox that you aren't standing nearby. Maybe you saw one
as you drove past, or maybe you know about a specific postbox but you aren't there now.

#### Step 1

Open the app and click "Register". Now click on "Select on map" in the top-bar.
This will show a screen with a map, a postbox dropdown and a monarch dropdown.

#### Step 2

The map has a pin on it. You can move the map around and drop the pin into the desired location.
Then, click the "Select Postbox" dropdown and the app will show you all the postboxes in that area.

#### Step 3

Select the desired postbox and monarch, just like you're [registering a nearby postbox](#registering-a-nearby-postbox).

Click save and the postbox has been registered.

<div>
<img
    height="512" src="images/add_unverified_1.png"
    alt="Screenshot of the PostboxGO app when adding an unverified postbox"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/add_unverified_2.png"
    alt="Screenshot of adding an unverified postbox once all options have been filled in"
    style="float: left; margin-left:5px"
/>
</div>

#### Verification

Whilst PostboxGO allows you to register postboxes even if you aren't near them, those postboxes will
be marked as "unverified", meaning we haven't been able to validate that you've actually been to this
postbox in-person.

In order to verify the postbox you must physically go to that postbox and verify it in the app.

##### Step 1

Open up the postbox details by selecting it from the list view. From here click the "edit button".

##### Step 2

There will be a button saying "Verify postbox". Click it. This will determine your current location
and verify that you are within a set distance of the postbox.

If you're close enough the postbox will be marked as verified.

Click save and go back to the home screen. The postbox will now show up as normal.

<div>
<img
    height="512" src="images/verify_postbox_1.png"
    alt="Screenshot of the PostboxGO app homepage with an unverified postbox"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/verify_postbox_2.png"
    alt="Screenshot of the details view for an unverified postbox"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/verify_postbox_3.png"
    alt="Screenshot of the verification process for a postbox"
    style="float: left; margin-left:5px"
/>
</div>

### Registering an inactive postbox

Every so often you'll run across a postbox that has been decommissioned and taken out of service, but
the postbox itself is still there.

Unfortunately these postboxes don't show up in Royal Mail APIs, but you can still register them in
the app.

#### Step 1

Open the app, click "Register" and click "Inactive Postbox" in the top-bar.

This will show you a map with a pin. Use the map to reposition the pin to the exact position of the
inactive postbox.

#### Step 2

Once you've positioned the map correctly, you can select the postbox type.
This dropdown will show you icons along with each postbox type to make it easier to identify.

After selecting the type, select the monarch in the usual way.

#### Step 3

Click save to register this postbox.

Inactive postboxes will show up in the list view and in maps with a faded-out icon to indicate that
they are inactive.

They are also subject to the same verification rules as any other postbox, so you may need
to verify the postbox after registration (see [postbox verification](#verification) for more details).

<div>
<img
    height="512" src="images/add_inactive_1.png"
    alt="Screenshot of the PostboxGO app registration page for inactive postboxes"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/add_inactive_2.png"
    alt="Screenshot of the registration page for inactive postboxes after repositioning the map"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/add_inactive_3.png"
    alt="Screenshot of the postbox type selection dropdown"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/add_inactive_4.png"
    alt="Screenshot of the registration page for inactive postboxes with all options filled in"
    style="float: left; margin-left:5px"
/>
<img
    height="512" src="images/add_inactive_5.png"
    alt="Screenshot of the details view for an inactive postbox"
    style="float: left; margin-left:5px"
/>
</div>

## Viewing registered postboxes

### List view

Once you've registered some postboxes, your homepage may looks something more like this:

<img
    height=512 src="../docs/images/autocapture/phone_homepage.png"
    alt="PostboxGO homepage"
/>

At the top you have a search bar. You can filter by postbox name, ID/postbox number, type,
registration date and monarch.

Under the search bar will be all of the registered postboxes listed. Each entry has an icon showing
the postbox type, the name and ID of the postbox, the monarch and the date it was registered.

By default the entries are sorted by registration date, but this can be changed in settings.

You can click on each of these entries to view [additional details about the postbox](#details-view).

### Details view

This view will show additional details about a postbox.

<img
    height=512 src="../docs/images/autocapture/phone_details_view.png"
    alt="PostboxGO details view"
/>

As shown in the above screenshot, the details page starts text information about the postbox, such
as the name and ID, and then shows a map of the postbox's location.

An age estimate is shown as well, although it must be stressed that this is a rough estimate based
on the postbox type and the reign of the monarch associated with it, with some deviations. For
example Charles 3rd took the throne in 2022 but the
[first postbox with his cypher](https://www.bbc.co.uk/news/articles/cjr4wxd277qo) only appeared in 2024.

Underneath this are the action buttons. You can either get directions to the postbox or delete this
entry from your save file. Note that once it is deleted there is no way to "undo", and you'll have to
register it again.

In the top-right corner there is also an edit button. Clicking this allows you to edit the monarch
associated with the postbox.

### Map view

The map view shows a map of all of the postboxes you have registered, letting you see just how far
and wide you've travelled, or how concentrated your postbox spotting has been. 

You can click on each postbox icon to see its name, and clicking on that label will take you to the
details view.

<img
height=512 src="../docs/images/autocapture/phone_map_view.png"
alt="PostboxGO map view"
/>

### Statistics

This page just shows some key statistics about the postboxes you have registered.

Here you can see the total count of postboxes, including inactive and unverified ones. It will show
a graph of registrations over time, the number of different monarchs you've seen, as well as the
distribution of postbox types that you've spotted.

If it's easier, you can put your phone in landscape to view these graphs, and you can also pan
and zoom on each graph.

## Settings

The settings page is fairly self explanatory. This is where you go to select the app theme and control
how the app behaves.

In the "Save file options" section there's the option to export your postboxes save file. This will
save it as a json file to your downloads folder. This can be useful if you're moving to a new device
and want to continue your postbox spotting there.

There's also the option to import a save file. This will overwrite your current save file, so use this
with caution.

Finally, at the bottom there's the standard buttons for reporting issues and privacy policy, as well
as the current app version.